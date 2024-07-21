package rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import apikeys.APIKeys;
import gsonprocessor.PlanSchema;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rest.ChatCompletionResponse.Message;
public class ChatCompletionClient {
    private String chatAPI_URL;
    private String embeddingAPI_URL;
    private String authorization;
    private String organization;
    private String project;
    private String modelName;//gpt-3.5-turbo
    private double temperature;
    private int maxToken;
    private boolean ifStream;
    private int seed;
    private String prompt;
    private List<Tool> tools = null;
    private String tool_choice = "auto";
    private static boolean error429 = false;
    

    // Private constructor to enforce the use of the builder
    private ChatCompletionClient(Builder builder) {
        this.chatAPI_URL = builder.chatAPI_URL;
        this.embeddingAPI_URL = builder.embeddingAPI_URL;
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.maxToken = builder.maxToken;
        this.ifStream = builder.ifStream;
        this.seed = builder.seed;
        this.authorization = builder.authorization;
        this.project = builder.project;
        this.organization = builder.organization;
        this.tools = builder.tools;
        this.tool_choice = builder.tool_choice;
        
    }
    
    
    public static void main(String[] args) {
    	ChatCompletionClient client = new ChatCompletionClient.Builder()
    			.setChatAPI_URL("http://localhost:11434/v1/chat/completions")//"https://api.openai.com/v1/chat/completions"http://localhost:1234/v1/chat/completions
    			.setEmbeddingAPI_URL("http://localhost:1234/v1/embeddings")
    			.setModelName("llama3")//gpt-3.5-turbo
    			//.setauthorization(APIKeys.GPT_KEY)
    			//.setOrganization(APIKeys.ORGANIZATION_ID)
    			//.setProject(APIKeys.PROJECT_ID)
    			.setIfStream(false)
    			.setMaxToken(4096)
    			.setTemperature(.7)
    			.setTools(List.of(PlanSchema.getPlanGsonSchemaAsFunctionTool()))
    			.build();
    	//client.getResponse("Answer in academic language.", "Introduce yourself.");
    	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    	String system = Prompt.SystemMessageFunctionCalling;
    	while (true) {
            System.out.print("\nUser: ");
            String input = null;
			try {
				input = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            String out = client.getResponse(system, input).getContent();
            System.out.print("AI: ");
            System.out.println(out);
        }
	}

    // Static Builder class
    public static class Builder {
        private String chatAPI_URL = "http://localhost:1234/v1/chat/completions";
        private String embeddingAPI_URL = "http://localhost:1234/v1/embeddings";
        private String modelName = "LM Studio Community/Meta-Llama-3-8B-Instruct-GGUF";
        private double temperature = 0.7;
        private int maxToken = -1;
        private boolean ifStream = false;
        private int seed = 1234;
        private String authorization;
        private String project;
        private String organization;
        private List<Tool> tools = null;
        private String tool_choice = "auto";

        public Builder() {
        	
        }

        public Builder setChatAPI_URL(String chatAPI_URL) {
            this.chatAPI_URL = chatAPI_URL;
            return this;
        }
        
        public Builder setToolChoice(String tool_choice) {
        	this.tool_choice = tool_choice;
        	return this;
        }

        public Builder setEmbeddingAPI_URL(String embeddingAPI_URL) {
            this.embeddingAPI_URL = embeddingAPI_URL;
            return this;
        }

        public Builder setModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder setTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder setMaxToken(int maxToken) {
            this.maxToken = maxToken;
            return this;
        }

        public Builder setauthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public Builder setProject(String project) {
            this.project = project;
            return this;
        }

        public ChatCompletionClient build() {
            return new ChatCompletionClient(this);
        }
        public Builder setIfStream(boolean ifStream) {
            this.ifStream = ifStream;
            return this;
        }

        public Builder setSeed(int seed) {
            this.seed = seed;
            return this;
        }
        
        public Builder setOrganization(String orgnaization) {
        	this.organization = orgnaization;
        	return this;
        }
        
        public Builder setTools(List<Tool> tools) {
        	this.tools = tools;
        	return this;
        }
        
    }

    // Getters for the fields (optional, but usually needed)
    public String getChatAPI_URL() {
        return chatAPI_URL;
    }

    public String getEmbeddingAPI_URL() {
        return embeddingAPI_URL;
    }

    public String getModelName() {
        return modelName;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxToken() {
        return maxToken;
    }

    public boolean isIfStream() {
        return ifStream;
    }

    public int getSeed() {
        return seed;
    }
    
    public String getToolChoice() {
    	return this.tool_choice;
    }

    public Message getResponse(String systemMessage,String userMessage) {
        try {
        	prompt+="\nUser:"+userMessage;
            // Create the request payload
            ChatCompletionRequest requestPayload = new ChatCompletionRequest();
            requestPayload.setModel(modelName);
            requestPayload.setMessages(Arrays.asList(
                new ChatCompletionRequest.Message("system", systemMessage),
                new ChatCompletionRequest.Message("user", prompt)
            ));
            requestPayload.setTemperature(temperature);
            requestPayload.setMaxTokens(maxToken);
            requestPayload.setStream(ifStream);
            requestPayload.SetTools(tools);
            requestPayload.setToolChoice(tool_choice);

            // Serialize the request payload to JSON
            Gson gson = new Gson();
            String requestBody = gson.toJson(requestPayload);
            //System.out.println(requestBody);
            // Create the HTTP request
            OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(1000, TimeUnit.SECONDS).build();
            
			MediaType mediaType = MediaType.parse("application/json");
			RequestBody body = RequestBody.create(mediaType, requestBody);
			
			Request request = null;
			
			okhttp3.Request.Builder requestBuilder = new Request.Builder()
			   .url(chatAPI_URL)
			   .method("POST", body)
			   .addHeader("Content-Type", "application/json");
			if(this.authorization!=null)requestBuilder.addHeader("Authorization", "Bearer "+this.authorization);
			if(this.project!=null)requestBuilder.addHeader("OpenAI-Project", this.project);
			if(this.organization!=null)requestBuilder.addHeader("OpenAI-Organization", this.organization);
			request = requestBuilder.build();
			String responseBody = null;
			Response responseok = null;
			try {
				while(true) {
					if(!error429) {
						responseok = client.newCall(request).execute();
						if(!responseok.isSuccessful() && responseok.code()==429 ||responseok.code()==500) {
							error429 = true;
						}else {
							error429 = false;
							break;
						}
					}else {
						Thread.sleep(10000);
						error429 = false;
					}
					
					
					
				}
				
				responseBody = responseok.body().string();
				client.dispatcher().executorService().shutdown();
				client.connectionPool().evictAll();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        

            // Parse the response
            ChatCompletionResponse chatCompletionResponse = gson.fromJson(responseBody, ChatCompletionResponse.class);

            // Print the response details
            //System.out.println("Response ID: " + chatCompletionResponse.getId());
           // for (ChatCompletionResponse.Choice choice : chatCompletionResponse.getChoices()) {
               // System.out.println("Message: " + choice.getMessage().getContent());
                //System.out.println("Finish Reason: " + choice.getFinishReason());
            //}
            if(chatCompletionResponse.getChoices()==null) {
            	System.out.println(responseBody);
            }
            Message response = chatCompletionResponse.getChoices().get(0).getMessage();
            //String functionCall = chatCompletionResponse.getChoices().get(0).getMessage().getToolCalls().get(0).getFunction().getArguments();
            
//            if(response.getToolCalls().isEmpty()) {
//            	System.out.println("Function was not called!!!");
//            }

            prompt+="\nModel:"+response;
            
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    @Override
    public String toString() {
        return "ChatCompletionClient{" +
                "chatAPI_URL='" + chatAPI_URL + '\'' +
                ", embeddingAPI_URL='" + embeddingAPI_URL + '\'' +
                ", modelName='" + modelName + '\'' +
                ", temperature=" + temperature +
                ", maxToken=" + maxToken +
                ", ifStream=" + ifStream +
                ", seed=" + seed +
                '}';
    }
    
}


