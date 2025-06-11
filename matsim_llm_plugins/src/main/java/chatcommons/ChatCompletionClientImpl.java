package chatcommons;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

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
import matsimBinding.BackendType;
import matsimBinding.LLMConfigGroup;
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
    public IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage, List<JsonObject> tools) {
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
            history, tools, "auto", config.getTemperature(), config.getMaxTokens(), config.getModelName(), false
        );

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getAuthorization())
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP code: " + response);
            }

            String responseBody = Objects.requireNonNull(response.body()).string();

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

