package chatcommons;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.matsim.core.controler.MatsimServices;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import chatrequest.IChatCompletionRequest;
import chatrequest.IRequestMessage;
import chatrequest.LmStudioChatRequest;
import chatrequest.OpenAiChatRequest;
import chatresponse.IChatCompletionResponse;
import chatresponse.LmStudioChatResponse;
import chatresponse.OpenAiChatResponse;
import matsimBinding.LLMConfigGroup;
import matsimBinding.LLMConfigGroup.BackendType;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatCompletionClientImpl implements IChatCompletionClient {

	private final LLMConfigGroup config;
	  // Raw request/response JSONL log
    private final String rawLogFilePath;
    
    private MatsimServices services;


	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(10, TimeUnit.MINUTES)
			.writeTimeout(30, TimeUnit.SECONDS)
			.callTimeout(0, TimeUnit.SECONDS) // or omit this line
			.build();
	private final Gson gson = new Gson();

	@Inject
	public ChatCompletionClientImpl(LLMConfigGroup configGroup , MatsimServices services) {
		this.config = configGroup;
		this.rawLogFilePath = (config.getLogFilePath() != null && !config.getLogFilePath().isBlank())
                ? config.getLogFilePath()
                : "llm_raw.jsonl";
		
		this.services = services;

	}

	@Override
	public IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage, List<JsonObject> tools, Map<String,Boolean> ifToolDummy) {
		String endpoint = config.getFullLlmUrl();
		BackendType backend = config.getBackendEnum();
		history.add(userMessage);
		
		long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

		// Pick the correct payload builder based on backend
		IChatCompletionRequest builder = switch (backend) {
		case OPENAI -> new OpenAiChatRequest();
		case LM_STUDIO -> new LmStudioChatRequest();
		case OLLAMA -> new OpenAiChatRequest();
		};

		String body = builder.serializeToHttpBody(
				history, tools,ifToolDummy, "auto", config.getTemperature(), config.getMaxTokens(), config.getModelName(), false, userMessage.ifEnableThinking()
				);

		Request request = new Request.Builder()
				.url(endpoint)
				.addHeader("Content-Type", "application/json")
				.addHeader("Authorization", "Bearer " + config.getAuthorization())
				.post(RequestBody.create(body, MediaType.parse("application/json")))
				.build();

//		System.out.println(body);
//		System.out.println(request.toString());
		
		
		
		Integer httpStatus = null;
	    String responseBody = null;
	    String error = null;
	    
		try (Response response = httpClient.newCall(request).execute()) {
			responseBody  = response.body() != null ? response.body().string() : "(no body)";

			httpStatus = response.code();
            

			if (!response.isSuccessful()) {
				throw new IOException("HTTP " + response.code() + " error from OpenAI: " + responseBody);
			}

			IChatCompletionResponse parsed = switch (backend) {
			case OPENAI -> gson.fromJson(responseBody, OpenAiChatResponse.class);
			case LM_STUDIO -> gson.fromJson(responseBody, LmStudioChatResponse.class);
			case OLLAMA -> gson.fromJson(responseBody, OpenAiChatResponse.class);
			};

			if (parsed != null) {
				parsed.postBuildCleanup();
			}

			return parsed;

		} catch (IOException e) {
			error = e.toString();
			throw new RuntimeException("Failed to call LLM endpoint: " + e.getMessage(), e);
		}finally {
            long durationMs = System.currentTimeMillis() - startTime;

            RawLlmLogEntry logEntry = new RawLlmLogEntry();
            logEntry.traceId = traceId;
            logEntry.timestamp = LocalDateTime.now().toString();
            logEntry.backend = backend.name();
            logEntry.endpoint = endpoint;
            logEntry.modelName = config.getModelName();
            logEntry.temperature = config.getTemperature();
            logEntry.maxTokens = config.getMaxTokens();
            logEntry.thinkingEnabled = userMessage.ifEnableThinking();
            logEntry.httpStatus = httpStatus;
            logEntry.durationMs = durationMs;
            logEntry.requestBody = body;
            logEntry.responseBody = responseBody;
            logEntry.error = error;
            
            logEntry.iteration = (services != null) ? services.getIterationNumber() : null;
            
            String iterFilePath;
            String combinedFilePath;

            if (services != null) {
                iterFilePath = services.getControlerIO()
                        .getIterationFilename(services.getIterationNumber(), rawLogFilePath + "_ChatLog.jsonl");

                combinedFilePath = services.getControlerIO()
                        .getOutputFilename(rawLogFilePath + "_ChatLog_combined.jsonl");
            } else {
                iterFilePath = rawLogFilePath + "_ChatLog.json";
                combinedFilePath = rawLogFilePath + "_ChatLog_combined.jsonl";
            }

            appendJsonLine(iterFilePath, logEntry);
            appendJsonLine(combinedFilePath, logEntry);
            
            
//            String filePath = null;
//            if(services!=null) {
//            	filePath = services.getControlerIO().getIterationFilename(services.getIterationNumber(), rawLogFilePath+"_ChatLog.json");
//            	
//            }else {
//            	filePath = rawLogFilePath+"_ChatLog.json";
//            }
//            
//            appendJsonLine(filePath, logEntry);
        }

	}
	
	@Override
	public IChatCompletionResponse query(List<IChatMessage> history, List<IRequestMessage> userMessage, List<JsonObject> tools, Map<String,Boolean> ifToolDummy) {
		String endpoint = config.getFullLlmUrl();
		BackendType backend = config.getBackendEnum();
		history.addAll(userMessage);
		
		long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();

		// Pick the correct payload builder based on backend
		IChatCompletionRequest builder = switch (backend) {
		case OPENAI -> new OpenAiChatRequest();
		case LM_STUDIO -> new LmStudioChatRequest();
		case OLLAMA -> new OpenAiChatRequest();
		};

		String body = builder.serializeToHttpBody(
				history, tools,ifToolDummy, "auto", config.getTemperature(), config.getMaxTokens(), config.getModelName(), false, userMessage.get(userMessage.size()-1).ifEnableThinking()
				);

		Request request = new Request.Builder()
				.url(endpoint)
				.addHeader("Content-Type", "application/json")
				.addHeader("Authorization", "Bearer " + config.getAuthorization())
				.post(RequestBody.create(body, MediaType.parse("application/json")))
				.build();

//		System.out.println(body);
//		System.out.println(request.toString());
		
		
		
		Integer httpStatus = null;
	    String responseBody = null;
	    String error = null;
	    
		try (Response response = httpClient.newCall(request).execute()) {
			responseBody  = response.body() != null ? response.body().string() : "(no body)";

			httpStatus = response.code();
            

			if (!response.isSuccessful()) {
				throw new IOException("HTTP " + response.code() + " error from OpenAI: " + responseBody);
			}

			IChatCompletionResponse parsed = switch (backend) {
			case OPENAI -> gson.fromJson(responseBody, OpenAiChatResponse.class);
			case LM_STUDIO -> gson.fromJson(responseBody, LmStudioChatResponse.class);
			case OLLAMA -> gson.fromJson(responseBody, OpenAiChatResponse.class);
			};

			if (parsed != null) {
				parsed.postBuildCleanup();
			}

			return parsed;

		} catch (IOException e) {
			error = e.toString();
			throw new RuntimeException("Failed to call LLM endpoint: " + e.getMessage(), e);
		}finally {
            long durationMs = System.currentTimeMillis() - startTime;

            RawLlmLogEntry logEntry = new RawLlmLogEntry();
            logEntry.traceId = traceId;
            logEntry.timestamp = LocalDateTime.now().toString();
            logEntry.backend = backend.name();
            logEntry.endpoint = endpoint;
            logEntry.modelName = config.getModelName();
            logEntry.temperature = config.getTemperature();
            logEntry.maxTokens = config.getMaxTokens();
            logEntry.thinkingEnabled = userMessage.get(userMessage.size()-1).ifEnableThinking();
            logEntry.httpStatus = httpStatus;
            logEntry.durationMs = durationMs;
            logEntry.requestBody = body;
            logEntry.responseBody = responseBody;
            logEntry.error = error;
            
            logEntry.iteration = (services != null) ? services.getIterationNumber() : null;
            
            String iterFilePath;
            String combinedFilePath;

            if (services != null) {
                iterFilePath = services.getControlerIO()
                        .getIterationFilename(services.getIterationNumber(), rawLogFilePath + "_ChatLog.jsonl");

                combinedFilePath = services.getControlerIO()
                        .getOutputFilename(rawLogFilePath + "_ChatLog_combined.jsonl");
            } else {
                iterFilePath = rawLogFilePath + "_ChatLog.json";
                combinedFilePath = rawLogFilePath + "_ChatLog_combined.jsonl";
            }

            appendJsonLine(iterFilePath, logEntry);
            appendJsonLine(combinedFilePath, logEntry);
            
            
//            String filePath = null;
//            if(services!=null) {
//            	filePath = services.getControlerIO().getIterationFilename(services.getIterationNumber(), rawLogFilePath+"_ChatLog.json");
//            	
//            }else {
//            	filePath = rawLogFilePath+"_ChatLog.json";
//            }
//            
//            appendJsonLine(filePath, logEntry);
        }

	}

	

	@Override
	public LLMConfigGroup getLLMConfig() {
		return config;
	}
	
	private void appendJsonLine(String filePath, Object obj) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(gson.toJson(obj));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write LLM log to " + filePath + ": " + e.getMessage());
        }
    }

    private static class RawLlmLogEntry {
        String traceId;
        String timestamp;
        
        Integer iteration;

        String backend;
        String endpoint;
        String modelName;

        Double temperature;
        Integer maxTokens;
        Boolean thinkingEnabled;

        Integer httpStatus;
        Long durationMs;

        String requestBody;
        String responseBody;
        String error;
    }
}



