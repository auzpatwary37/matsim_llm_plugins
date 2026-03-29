package rag;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import matsimBinding.LLMConfigGroup;

class RagRuntimeIT {

    private static final Duration INDEX_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DELETE_TIMEOUT = Duration.ofSeconds(10);
    private static final long POLL_INTERVAL_MS = 250L;

    private VectorDBImplement vectorDb;
    private Path tempStaticFile;

    @AfterEach
    void tearDown() throws IOException {
        if (tempStaticFile != null) {
            Files.deleteIfExists(tempStaticFile);
            tempStaticFile = null;
        }
    }

    @Test
    void qdrant_serverIsReachable_live() throws Exception {
        assumeLiveTestsEnabled();

        String host = firstNonBlank(System.getenv("QDRANT_HOST"), "localhost");
        int restPort = Integer.parseInt(firstNonBlank(System.getenv("QDRANT_REST_PORT"), "6333"));

        HttpURLConnection conn = (HttpURLConnection)
                new URL("http://" + host + ":" + restPort + "/healthz").openConnection();
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        assertEquals(200, status, "Qdrant REST health endpoint did not return 200");

        System.out.println("Confirmed: Qdrant is reachable on " + host + ":" + restPort);
    }

    @Test
    void embedding_endpointIsReachable_live() {
        assumeLiveTestsEnabled();

        LLMConfigGroup config = liveRagConfig(null);

        String baseUrl = normalizeEmbeddingBaseUrl(config.getFullEmbeddingUrl());

        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(firstNonBlank(System.getenv("EMBEDDING_API_KEY"), "lm-studio"))
                .modelName(config.getEmbeddingModelName())
                .build();

        String probeText = "embedding connectivity test";

        long start = System.currentTimeMillis();
        Embedding embedding;

        try {
            embedding = model.embed(probeText).content();
        } catch (Exception e) {
            fail("Embedding endpoint is not reachable or failed to respond. "
                    + "Check LM Studio / API config. Error: " + e.getMessage());
            return;
        }

        long durationMs = System.currentTimeMillis() - start;

        assertNotNull(embedding, "Embedding result is null");
        assertNotNull(embedding.vector(), "Embedding vector is null");
        assertTrue(embedding.vector().length > 0, "Embedding vector is empty");

        System.out.println("Embedding reachable. Dimension = "
                + embedding.vector().length + ", duration = " + durationMs + " ms");
    }

    @Test
    void vectorDb_canInsertRetrieveAndClear_dynamicDocuments_live() throws Exception {
        assumeLiveTestsEnabled();

        String staticToken = "STATIC-" + UUID.randomUUID();
        tempStaticFile = Files.createTempFile("rag-static-test-", ".txt");
        Files.writeString(tempStaticFile,
                "This is static context for test verification. Token=" + staticToken);

        LLMConfigGroup config = liveRagConfig(tempStaticFile.toString());
        String uniqueCollection = "rag_test_" + UUID.randomUUID().toString().replace("-", "");
        config.setVectorDbCollectionName(uniqueCollection);

        vectorDb = new VectorDBImplement(config);

        String dynamicToken = "DYNAMIC-" + UUID.randomUUID();
        String docId = "dynamic_" + dynamicToken;
        String content = "Agent 007 is stuck in traffic near Toronto. Token=" + dynamicToken;

        Map<String, String> metadata = Map.of(
                "source", "matsim-test",
                "token", dynamicToken,
                "kind", "dynamic"
        );

        vectorDb.insert(docId, content, metadata);

        List<IVectorDB.RetrievedDocument> indexedResults = waitForQueryMatch(
                dynamicToken,
                5,
                docs -> docs.stream().anyMatch(d ->
                        docId.equals(d.id())
                                && d.content().contains(dynamicToken)
                                && dynamicToken.equals(d.metadata().get("token"))
                ),
                INDEX_TIMEOUT,
                "Dynamic document was not retrievable after insert"
        );

        IVectorDB.RetrievedDocument matched = indexedResults.stream()
                .filter(d -> docId.equals(d.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Inserted document id not found in results"));

        assertTrue(matched.content().contains(dynamicToken), "Retrieved content missing unique token");
        assertEquals("matsim-test", matched.metadata().get("source"));
        assertEquals(dynamicToken, matched.metadata().get("token"));
        assertEquals("dynamic", matched.metadata().get("kind"));

        List<IVectorDB.RetrievedDocument> staticBeforeClear = waitForQueryMatch(
                staticToken,
                5,
                docs -> docs.stream().anyMatch(d -> d.content().contains(staticToken)),
                INDEX_TIMEOUT,
                "Static document was not retrievable after initialization"
        );
        assertFalse(staticBeforeClear.isEmpty(), "Expected static documents to be present");

        vectorDb.clearDynamicDocuments();

        waitForQueryMatch(
                dynamicToken,
                5,
                docs -> docs.stream().noneMatch(d -> d.content().contains(dynamicToken)),
                DELETE_TIMEOUT,
                "Dynamic document still retrievable after clearDynamicDocuments()"
        );

        List<IVectorDB.RetrievedDocument> staticAfterClear = waitForQueryMatch(
                staticToken,
                5,
                docs -> docs.stream().anyMatch(d -> d.content().contains(staticToken)),
                INDEX_TIMEOUT,
                "Static document disappeared after dynamic cleanup"
        );

        assertTrue(
                staticAfterClear.stream().anyMatch(d -> d.content().contains(staticToken)),
                "Static document should remain after clearing dynamic documents only"
        );
    }

    @Test
    void vectorDb_clearStaticDocuments_removesLoadedStaticContent_live() throws Exception {
        assumeLiveTestsEnabled();

        String staticToken = "STATIC-ONLY-" + UUID.randomUUID();
        tempStaticFile = Files.createTempFile("rag-static-clear-test-", ".txt");
        Files.writeString(tempStaticFile,
                "Static-only test document. Token=" + staticToken);

        LLMConfigGroup config = liveRagConfig(tempStaticFile.toString());
        String uniqueCollection = "rag_test_" + UUID.randomUUID().toString().replace("-", "");
        config.setVectorDbCollectionName(uniqueCollection);

        vectorDb = new VectorDBImplement(config);

        waitForQueryMatch(
                staticToken,
                5,
                docs -> docs.stream().anyMatch(d -> d.content().contains(staticToken)),
                INDEX_TIMEOUT,
                "Static document was not retrievable before clearStaticDocuments()"
        );

        vectorDb.clearStaticDocuments();

        waitForQueryMatch(
                staticToken,
                5,
                docs -> docs.stream().noneMatch(d -> d.content().contains(staticToken)),
                DELETE_TIMEOUT,
                "Static document still retrievable after clearStaticDocuments()"
        );
    }

    private List<IVectorDB.RetrievedDocument> waitForQueryMatch(
            String query,
            int topK,
            Predicate<List<IVectorDB.RetrievedDocument>> condition,
            Duration timeout,
            String failureMessage) throws InterruptedException {

        Instant deadline = Instant.now().plus(timeout);
        List<IVectorDB.RetrievedDocument> lastResults = List.of();

        while (Instant.now().isBefore(deadline)) {
            lastResults = vectorDb.query(query, topK);
            if (condition.test(lastResults)) {
                return lastResults;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        fail(failureMessage + ". Last results: " + lastResults);
        return lastResults;
    }

    private void assumeLiveTestsEnabled() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("RUN_LIVE_RAG_TESTS", "false")),
                "Skipping live RAG tests. Set RUN_LIVE_RAG_TESTS=true to enable."
        );
    }

    private static LLMConfigGroup liveRagConfig(String staticFilePath) {
        LLMConfigGroup config = new LLMConfigGroup();

        config.setBackend(LLMConfigGroup.BackendType.LM_STUDIO);
        config.setAuthorization(firstNonBlank(System.getenv("EMBEDDING_API_KEY"), "lm-studio"));
        config.setLlmHost(firstNonBlank(System.getenv("LLM_HOST"), "localhost"));
        config.setLlmPort(Integer.parseInt(firstNonBlank(System.getenv("LLM_PORT"), "1234")));
        config.setUseHttps(false);

        config.setEmbeddingModelName(
                firstNonBlank(System.getenv("EMBEDDING_MODEL"), "text-embedding-nomic-embed-text-v1.5@q8_0")
        );

        config.setVectorDbHost(firstNonBlank(System.getenv("QDRANT_HOST"), "localhost"));
        config.setVectorDbPort(Integer.parseInt(firstNonBlank(System.getenv("QDRANT_GRPC_PORT"), "6334")));
        config.setVectorDbCollectionName("rag_test_default");

        if (staticFilePath != null) {
            config.setVectorDBSourceFile(staticFilePath);
        }

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

    private static String normalizeEmbeddingBaseUrl(String url) {
        if (url == null) {
            return null;
        }
        String normalized = url.trim();
        if (normalized.endsWith("/embeddings")) {
            normalized = normalized.substring(0, normalized.length() - "/embeddings".length());
        }
        return normalized;
    }
}