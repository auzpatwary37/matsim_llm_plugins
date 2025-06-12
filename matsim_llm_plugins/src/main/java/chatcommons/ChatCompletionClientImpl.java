package chatcommons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import apikeys.APIKeys;
import chatrequest.IChatCompletionRequest;
import chatrequest.IRequestMessage;
import chatrequest.LmStudioChatRequest;
import chatrequest.OpenAiChatRequest;
import chatrequest.SimpleRequestMessage;
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
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    @Inject
    public ChatCompletionClientImpl(LLMConfigGroup configGroup) {
        this.config = configGroup;
    }

    @Override
    public IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage, List<JsonObject> tools, Map<String,Boolean> ifToolDummy) {
        String endpoint = config.getFullLlmUrl();
        BackendType backend = config.getBackendEnum();
        history.add(userMessage);

        // Pick the correct payload builder based on backend
        IChatCompletionRequest builder = switch (backend) {
            case OPENAI -> new OpenAiChatRequest();
            case LM_STUDIO -> new LmStudioChatRequest();
            case OLLAMA -> throw new IllegalArgumentException("Ollama backend is not implemented yet!!! Use either openai or Lm");
        };

        String body = builder.serializeToHttpBody(
            history, tools,ifToolDummy, "auto", config.getTemperature(), config.getMaxTokens(), config.getModelName(), false
        );

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getAuthorization())
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();
        
        System.out.println(body);
        System.out.println(request.toString());

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "(no body)";

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " error from OpenAI: " + responseBody);
            }

            return switch (backend) {
                case OPENAI -> gson.fromJson(responseBody, OpenAiChatResponse.class);
                case LM_STUDIO -> gson.fromJson(responseBody, LmStudioChatResponse.class);
                case OLLAMA -> throw new IllegalArgumentException("Ollama backend is not implemented yet!!! Use either openai or Lm");
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to call LLM endpoint: " + e.getMessage(), e);
        }

    }

    @Override
    public LLMConfigGroup getLLMConfig() {
        return config;
    }
    
}

