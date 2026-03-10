package rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void vectorDb_canInsertAndRetrieve_runtimeDocuments_live() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("RUN_LIVE_RAG_TESTS", "false")),
                "Skipping live RAG test. Set RUN_LIVE_RAG_TESTS=true to enable.");

        LLMConfigGroup config = liveRagConfig();
        VectorDBImplement vectorDb = new VectorDBImplement(config);
        vectorDb.clearDynamicDocuments();

        String uniqueToken = "rag-token-" + UUID.randomUUID();
        String docId = "dynamic_" + uniqueToken;
        String content = "The MATSim runtime inserted document contains unique token " + uniqueToken
                + " and describes live commit and retrieval verification.";

        vectorDb.insert(docId, content, Map.of("source", "test", "token", uniqueToken));
        List<IVectorDB.RetrievedDocument> results = vectorDb.query(uniqueToken, 3);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Expected at least one retrieved document.");
        assertTrue(results.stream().anyMatch(d -> d.content().contains(uniqueToken)),
                "Inserted runtime document was not retrieved by semantic search.");

        vectorDb.clearDynamicDocuments();
    }

    @Test
    void chatManager_injectsRetrievedContext_intoUserMessageBeforeCallingClient() {
        InMemoryVectorDb vectorDb = new InMemoryVectorDb();
        vectorDb.insert("doc-1", "Toronto runtime context for MATSim charging plan retrieval.", Map.of("source", "test"));

        RecordingClient client = new RecordingClient();
        DefaultChatManager manager = new DefaultChatManager(
                Id.create("rag-injection-test", IChatManager.class),
                client,
                new DefaultToolManager(),
                vectorDb);
        manager.setSystemMessage("You are a test assistant.");

        manager.submitInternal(new SimpleRequestMessage(Role.USER, "Tell me about the Toronto charging plan."));

        assertNotNull(client.lastUserMessage);
        assertTrue(client.lastUserMessage.getContent().contains("[Retrieved Context]"));
        assertTrue(client.lastUserMessage.getContent().contains("Toronto runtime context for MATSim charging plan retrieval."));
        assertTrue(client.lastUserMessage.getContent().contains("[User Message]"));
        assertTrue(client.lastUserMessage.getContent().contains("Tell me about the Toronto charging plan."));
    }

    private static LLMConfigGroup liveRagConfig() {
        String auth = firstNonBlank(System.getenv("OPENAI_API_KEY"), APIKeys.GPT_KEY, "lm-studio");

        LLMConfigGroup config = new LLMConfigGroup();
        config.setBackend(LLMConfigGroup.BackendType.LM_STUDIO);
        config.setAuthorization(auth);
        config.setModelName(firstNonBlank(System.getenv("LM_STUDIO_MODEL"), "qwen/qwen3-14b"));
        config.setLlmHost(firstNonBlank(System.getenv("LM_STUDIO_HOST"), "localhost"));
        config.setLlmPort(Integer.parseInt(firstNonBlank(System.getenv("LM_STUDIO_PORT"), "1234")));
        config.setUseHttps(false);
        config.setLlmPath("/v1/chat/completions");

        config.setEmbeddingPath(firstNonBlank(System.getenv("EMBEDDING_PATH"), "/v1/embeddings"));
        config.setEmbeddingModelName(firstNonBlank(System.getenv("EMBEDDING_MODEL"), "text-embedding-nomic-embed-text-v1.5@q4_k_m:2"));
        config.setVectorDbHost(firstNonBlank(System.getenv("CHROMA_HOST"), "localhost"));
        config.setVectorDbPort(Integer.parseInt(firstNonBlank(System.getenv("CHROMA_PORT"), "8000")));
        config.setVectorDbCollectionName("rag_runtime_test_collection");
        config.setVectorDBSourceFile("src/test/resources/Chromadb/chromaBase.txt");
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
                if (doc.content().toLowerCase().contains("toronto") && prompt.toLowerCase().contains("toronto")) {
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
        public IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage,
                List<com.google.gson.JsonObject> tools, Map<String, Boolean> ifToolDummy) {
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
							// TODO Auto-generated method stub
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
