package rag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import matsimBinding.LLMConfigGroup;
import matsimBinding.LLMConfigGroup.VectorDbCleanupMode;

public class VectorDBImplement implements IVectorDB {

    private final LLMConfigGroup config;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;

    private final List<String> staticIds = new ArrayList<>();
    private final List<String> dynamicIds = new ArrayList<>();

    public VectorDBImplement(LLMConfigGroup config) {
        this.config = config;
        this.initialize();
    }

    @Override
    public void initialize() {

        /*
         * Assumptions:
         * - Qdrant is already running
         * - The collection already exists
         * - The collection dimension matches config.getEmbeddingDimension()
         *
         * Qdrant typically uses gRPC on 6334 for LangChain4j integration.
         */

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(config.getFullEmbeddingUrl())
                .apiKey(config.getAuthorization() != null ? config.getAuthorization() : "lm-studio")
                .modelName(config.getEmbeddingModelName())
                .build();

        this.embeddingStore = QdrantEmbeddingStore.builder()
                .host(config.getVectorDbHost())              // e.g. "localhost"
                .port(config.getVectorDbPort())              // usually 6334 for gRPC
                .collectionName(config.getVectorDbCollectionName())
                // .apiKey(config.getVectorDbApiKey())       // uncomment if needed
                .build();

        this.loadStaticDocumentsFromFile(this.config.getVectorDBSourceFile());
    }

    @Override
    public void insert(String id, String content, Map<String, String> metadata) {
        try {
            Metadata lcMetadata = metadata == null
                    ? new Metadata()
                    : new Metadata(new HashMap<>(metadata));

            TextSegment segment = TextSegment.from(content, lcMetadata);
            Embedding embedding = embeddingModel.embed(content).content();

            embeddingStore.add(embedding, segment);

            if (!id.startsWith("static_")) {
                this.dynamicIds.add(id);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert document into Qdrant: " + id, e);
        }
    }

    @Override
    public List<RetrievedDocument> query(String prompt, int topK) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(prompt).content();

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

            List<RetrievedDocument> results = new ArrayList<>();

            for (EmbeddingMatch<TextSegment> match : matches) {
                String id = match.embeddingId();
                TextSegment segment = match.embedded();

                Map<String, String> stringMeta = new HashMap<>();
                if (segment != null && segment.metadata() != null) {
                    Map<String, Object> raw = segment.metadata().toMap();
                    for (Map.Entry<String, Object> entry : raw.entrySet()) {
                        stringMeta.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }

                String content = segment != null ? segment.text() : "";

                results.add(new RetrievedDocument(id, content, stringMeta));
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to query Qdrant", e);
        }
    }

    @Override
    public String getEmbeddingModelName() {
        return this.config.getEmbeddingModelName();
    }

    @Override
    public void clearDynamicDocuments() {
        if (config.getCleanupModeEnum() == VectorDbCleanupMode.DYNAMIC_ONLY
                || config.getCleanupModeEnum() == VectorDbCleanupMode.ALL) {

            if (!dynamicIds.isEmpty()) {
                embeddingStore.removeAll(dynamicIds);
                dynamicIds.clear();
            }
        }
    }

    @Override
    public void clearStaticDocuments() {
        if (staticIds.isEmpty()) {
            return;
        }

        embeddingStore.removeAll(staticIds);
        staticIds.clear();
    }

    public void loadStaticDocumentsFromFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid static context file path: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            StringBuilder chunkBuilder = new StringBuilder();
            int chunkIndex = 0;

            List<String> ids = new ArrayList<>();
            List<TextSegment> segments = new ArrayList<>();
            List<Embedding> embeddings = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                chunkBuilder.append(line).append(" ");

                if (chunkBuilder.length() >= 1000) {
                    String chunk = chunkBuilder.toString().trim();
                    String id = "static_" + chunkIndex++;

                    Metadata metadata = new Metadata(Map.of(
                            "source", "static",
                            "file", file.getName()
                    ));

                    TextSegment segment = TextSegment.from(chunk, metadata);
                    Embedding embedding = embeddingModel.embed(chunk).content();

                    ids.add(id);
                    segments.add(segment);
                    embeddings.add(embedding);
                    staticIds.add(id);

                    chunkBuilder.setLength(0);
                }
            }

            if (chunkBuilder.length() > 0) {
                String chunk = chunkBuilder.toString().trim();
                String id = "static_" + chunkIndex++;

                Metadata metadata = new Metadata(Map.of(
                        "source", "static",
                        "file", file.getName()
                ));

                TextSegment segment = TextSegment.from(chunk, metadata);
                Embedding embedding = embeddingModel.embed(chunk).content();

                ids.add(id);
                segments.add(segment);
                embeddings.add(embedding);
                staticIds.add(id);
            }

            if (!ids.isEmpty()) {
                embeddingStore.addAll(ids, embeddings, segments);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read static context file: " + path, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load static context into Qdrant: " + path, e);
        }
    }
}