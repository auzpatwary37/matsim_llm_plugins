package rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;

import apikeys.APIKeys;
import chatcommons.DefaultChatManager;
import chatcommons.IChatCompletionClient;
import chatcommons.IChatManager;
import chatcommons.IChatMessage;
import chatcommons.Role;
import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import chatresponse.IChatCompletionResponse;
import chatresponse.IResponseMessage;
import matsimBinding.LLMConfigGroup;
import tools.DefaultToolManager;
import tools.IToolCall;

class RagRuntimeIT {

    @Test
    void vectorDb_canInsertRetrieveAndClear_runtimeDocuments_live() throws IOException {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("RUN_LIVE_RAG_TESTS", "true")),
                "Skipping live RAG test. Set RUN_LIVE_RAG_TESTS=true to enable."
        );

        Path emptyStaticFile = Files.createTempFile("rag-empty-static-", ".txt");
        try {
            LLMConfigGroup config = liveRagConfig(emptyStaticFile.toString());
            config.setVectorDbCollectionName("rag_runtime_test_" + UUID.randomUUID());

            VectorDBImplement vectorDb = new VectorDBImplement(config);
            vectorDb.clearDynamicDocuments();

            String uniqueToken = "ZXQF-" + UUID.randomUUID();
            String docId = "dynamic_" + uniqueToken;
            String content = "The MATSim runtime inserted document contains unique token " + uniqueToken
                    + " and describes live commit and retrieval verification.";

            vectorDb.insert(docId, content, Map.of("source", "test", "token", uniqueToken));

            List<IVectorDB.RetrievedDocument> results = vectorDb.query(uniqueToken, 5);

            assertNotNull(results, "Expected non-null retrieval result list.");
            assertFalse(results.isEmpty(), "Expected at least one retrieved document.");
            assertTrue(
                    results.stream().anyMatch(d -> d.content() != null && d.content().contains(uniqueToken)),
                    "Inserted runtime document was not retrieved by search."
            );

            vectorDb.clearDynamicDocuments();

            List<IVectorDB.RetrievedDocument> afterClear = vectorDb.query(uniqueToken, 5);

            assertNotNull(afterClear, "Expected non-null retrieval result list after cleanup.");
            assertTrue(
                    afterClear.stream().noneMatch(d -> d.content() != null && d.content().contains(uniqueToken)),
                    "Dynamic document still appears retrievable after cleanup."
            );

        } finally {
            Files.deleteIfExists(emptyStaticFile);
        }
    }

    @Test
    void chatManager_injectsRetrievedContext_intoUserMessageBeforeCallingClient() {
        InMemoryVectorDb vectorDb = new InMemoryVectorDb();
        vectorDb.insert(
                "doc-1",
                "Toronto runtime context for MATSim charging plan retrieval.",
                Map.of("source", "test")
        );

        RecordingClient client = new RecordingClient();
        DefaultChatManager manager = new DefaultChatManager(
                Id.create("rag-injection-test", IChatManager.class),
                client,
                new DefaultToolManager(),
                vectorDb
        );
        manager.setSystemMessage("You are a test assistant.");

        manager.submitInternal(new SimpleRequestMessage(Role.USER, "Tell me about the Toronto charging plan."));

        assertNotNull(client.lastUserMessage);
        assertTrue(client.lastUserMessage.getContent().contains("[Retrieved Context]"));
        assertTrue(client.lastUserMessage.getContent()
                .contains("Toronto runtime context for MATSim charging plan retrieval."));
        assertTrue(client.lastUserMessage.getContent().contains("[User Message]"));
        assertTrue(client.lastUserMessage.getContent().contains("Tell me about the Toronto charging plan."));
    }
    
    @Test
    void qdrant_serverIsReachable_live() {
        String host = firstNonBlank(System.getenv("QDRANT_HOST"), "localhost");
        int port = Integer.parseInt(firstNonBlank(System.getenv("QDRANT_PORT"), "6333")); // REST port for health check

        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL("http://" + host + ":" + port + "/healthz").openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            assertTrue(status == 200, "Qdrant health check returned HTTP " + status + " — expected 200.");

        } catch (IOException e) {
            fail("Could not reach Qdrant at " + host + ":" + port + " — is the container running? " + e.getMessage());
        }
    }

    private static LLMConfigGroup liveRagConfig(String staticFilePath) {
        String auth = firstNonBlank(System.getenv("OPENAI_API_KEY"), APIKeys.GPT_KEY, "lm-studio");

        LLMConfigGroup config = new LLMConfigGroup();

        // LLM / embedding endpoint
        config.setBackend(LLMConfigGroup.BackendType.LM_STUDIO);
        config.setAuthorization(auth);
        config.setModelName(firstNonBlank(System.getenv("LM_STUDIO_MODEL"), "qwen/qwen3.5-9b"));
        config.setLlmHost(firstNonBlank(System.getenv("LM_STUDIO_HOST"), "localhost"));
        config.setLlmPort(1234);
        config.setUseHttps(false);
        config.setLlmPath("/v1/chat/completions");

        config.setEmbeddingPath("/v1/embeddings");
        config.setEmbeddingModelName(
                "text-embedding-nomic-embed-text-v1.5@q8_0"
        );

        // Vector DB
        config.setVectorDbHost("localhost");
        config.setVectorDbPort(6334);
        config.setVectorDbCollectionName("rag_runtime_test_collection");

        // Keep runtime test isolated from static preload contamination
        config.setVectorDBSourceFile(staticFilePath);

        config.setEnableContextRetrieval(true);
        config.setCleanVectorDbUponCompletion("DYNAMIC_ONLY");

        return config;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static class InMemoryVectorDb implements IVectorDB {
        private final Map<String, RetrievedDocument> docs = new HashMap<>();

        @Override
        public void initialize() {
        }

        @Override
        public void insert(String id, String content, Map<String, String> metadata) {
            docs.put(id, new RetrievedDocument(id, content, metadata));
        }

        @Override
        public List<RetrievedDocument> query(String prompt, int topK) {
            List<RetrievedDocument> results = new ArrayList<>();
            for (RetrievedDocument doc : docs.values()) {
                if (doc.content().toLowerCase().contains("toronto")
                        && prompt.toLowerCase().contains("toronto")) {
                    results.add(doc);
                }
            }
            return results;
        }

        @Override
        public String getEmbeddingModelName() {
            return "in-memory";
        }

        @Override
        public void clearDynamicDocuments() {
            docs.clear();
        }

        @Override
        public void clearStaticDocuments() {
            docs.clear();
        }
    }

    private static class RecordingClient implements IChatCompletionClient {
        private IRequestMessage lastUserMessage;

        @Override
        public IChatCompletionResponse query(
                List<IChatMessage> history,
                IRequestMessage userMessage,
                List<com.google.gson.JsonObject> tools,
                Map<String, Boolean> ifToolDummy
        ) {
            this.lastUserMessage = userMessage;

            return new IChatCompletionResponse() {
                @Override
                public IResponseMessage getMessage() {
                    return new IResponseMessage() {
                        @Override
                        public Role getRole() {
                            return Role.ASSISTANT;
                        }

                        @Override
                        public String getContent() {
                            return "ok";
                        }

                        @Override
                        public List<IToolCall> getToolCalls() {
                            return List.of();
                        }

                        @Override
                        public boolean ifEnableThinking() {
                            return false;
                        }
                    };
                }

                @Override
                public List<IToolCall> getToolCalls() {
                    return List.of();
                }

                @Override
                public chatresponse.IUsage getUsage() {
                    return null;
                }

                @Override
                public String getModel() {
                    return "fake";
                }

                @Override
                public Map<String, Object> getMetadata() {
                    return Map.of();
                }

                @Override
                public void postBuildCleanup() {
                }

                @Override
                public String getReasoning() {
                    return null;
                }
            };
        }

        @Override
        public LLMConfigGroup getLLMConfig() {
            return null;
        }
    }
}