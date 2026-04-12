package chatcommons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;

import apikeys.APIKeys;
import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import chatresponse.IChatCompletionResponse;
import matsimBinding.LLMConfigGroup;
import tools.DefaultToolManager;
import tools.IToolResponse;

class LiveBackendConnectivityIT {

    @Test
    void lmStudio_clientAndChatThread_areAccessible() {
        LLMConfigGroup config = lmStudioConfig();
        Assumptions.assumeTrue(isLocalEndpointEnabled(config),
                "LM Studio endpoint is not reachable on " + config.getFullLlmUrl());

        IChatCompletionClient client = new ChatCompletionClientImpl(config);
        IChatCompletionResponse response = handshake(client,false);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertNotNull(response.getMessage().getRole());
        assertTrue(hasAnyAssistantPayload(response),
                "Expected either assistant content or tool calls from LM Studio.");

        DefaultChatManager manager = new DefaultChatManager(
                Id.create("lmstudio-live", IChatManager.class),
                client,
                new DefaultToolManager(),
                null);
        manager.setSystemMessage("You are paired with MATSim. Reply briefly.");

        Map<String, IToolResponse<?>> first = manager.submit(
                new SimpleRequestMessage(chatcommons.Role.USER, "Say hello to MATSim in one sentence.",false)).toolResponses;
        Map<String, IToolResponse<?>> second = manager.submit(
                new SimpleRequestMessage(chatcommons.Role.USER, "Now confirm the thread is still alive.", false)).toolResponses;

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertTrue(manager.getHistory().size() >= 3,
                "History should keep the system message and assistant replies across calls.");
        assertNotNull(manager.getLastMessage());
    }

    @Test
    void openAi_clientAndChatThread_areAccessible() {
        String apiKey = firstNonBlank(System.getenv("OPENAI_API_KEY"), APIKeys.GPT_KEY);
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "OpenAI API key is not configured.");

        LLMConfigGroup config = openAiConfig(apiKey);
        IChatCompletionClient client = new ChatCompletionClientImpl(config);
        IChatCompletionResponse response = handshake(client,true);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertNotNull(response.getMessage().getRole());
        assertTrue(hasAnyAssistantPayload(response),
                "Expected either assistant content or tool calls from OpenAI.");

        DefaultChatManager manager = new DefaultChatManager(
                Id.create("openai-live", IChatManager.class),
                client,
                new DefaultToolManager(),
                null);
        manager.setSystemMessage("You are paired with MATSim. Reply briefly.");

        manager.submit(new SimpleRequestMessage(chatcommons.Role.USER, "Say hello to MATSim."));
        manager.submit(new SimpleRequestMessage(chatcommons.Role.USER, "Confirm the chat thread still remembers this pairing."));

        assertTrue(manager.getHistory().size() >= 3);
        assertNotNull(manager.getLastMessage());
    }

    @Test
    void ollamaBackend_isCurrentlyNotImplemented_inChatCompletionClient() {
        LLMConfigGroup config = new LLMConfigGroup();
        config.setBackend(LLMConfigGroup.BackendType.OLLAMA);
        config.setAuthorization("ollama");
        config.setModelName("llama3");
        config.setLlmHost("localhost");
        config.setLlmPort(11434);
        config.setUseHttps(false);
        config.setLlmPath("/v1/chat/completions");

        IChatCompletionClient client = new ChatCompletionClientImpl(config);
        List<IChatMessage> history = new ArrayList<>();
        IRequestMessage user = new SimpleRequestMessage(chatcommons.Role.USER, "hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> client.query(history, user, List.of(), new HashMap<>()));
        assertTrue(ex.getMessage().contains("Ollama backend is not implemented yet"));
    }

    private static IChatCompletionResponse handshake(IChatCompletionClient client, boolean enableThinking) {
        List<IChatMessage> history = new ArrayList<>();
        history.add(new SimpleRequestMessage(chatcommons.Role.SYSTEM,
                "You are integrated into MATSim. Respond briefly so the integration test can verify connectivity.",enableThinking));
        IRequestMessage user = new SimpleRequestMessage(chatcommons.Role.USER,
                "Handshake test from MATSim. Acknowledge that you are live.",enableThinking);
        return client.query(history, user, List.of(), new HashMap<>());
    }

    private static boolean hasAnyAssistantPayload(IChatCompletionResponse response) {
        return (response.getMessage().getContent() != null && !response.getMessage().getContent().isBlank())
                || (response.getToolCalls() != null && !response.getToolCalls().isEmpty());
    }

    private static LLMConfigGroup lmStudioConfig() {
        LLMConfigGroup config = new LLMConfigGroup();
        config.setBackend(LLMConfigGroup.BackendType.LM_STUDIO);
        config.setAuthorization("lm-studio");
        config.setModelName(firstNonBlank(System.getenv("LM_STUDIO_MODEL"), "qwen/qwen3.5-9b"));
        config.setLlmHost(firstNonBlank(System.getenv("LM_STUDIO_HOST"), "localhost"));
        config.setLlmPort(Integer.parseInt(firstNonBlank(System.getenv("LM_STUDIO_PORT"), "1234")));
        config.setUseHttps(false);
        config.setLlmPath("/v1/chat/completions");
        config.setTemperature(0.0);
        config.setMaxTokens(2566);
        return config;
    }

    private static LLMConfigGroup openAiConfig(String apiKey) {
        LLMConfigGroup config = new LLMConfigGroup();
        config.setBackend(LLMConfigGroup.BackendType.OPENAI);
        config.setAuthorization(apiKey);
        if (APIKeys.PROJECT_ID != null && !APIKeys.PROJECT_ID.isBlank()) {
            config.setProject(APIKeys.PROJECT_ID);
        }
        if (APIKeys.ORGANIZATION_ID != null && !APIKeys.ORGANIZATION_ID.isBlank()) {
            config.setOrganization(APIKeys.ORGANIZATION_ID);
        }
        config.setModelName(firstNonBlank(System.getenv("OPENAI_MODEL"), "gpt-4o-mini"));
        config.setLlmHost("api.openai.com");
        config.setUseHttps(true);
        config.setLlmPath("/v1/chat/completions");
        config.setTemperature(0.0);
        config.setMaxTokens(2566);
        return config;
    }

    private static boolean isLocalEndpointEnabled(LLMConfigGroup config) {
        try {
            okhttp3.OkHttpClient http = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(config.getFullLlmUrl().replace("/chat/completions", "/models"))
                    .get()
                    .build();
            try (okhttp3.Response response = http.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
