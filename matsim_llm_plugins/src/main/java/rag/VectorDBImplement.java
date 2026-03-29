package rag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
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
        // Setup the Embedding Model (Connecting to your local/remote LLM)
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(config.getFullEmbeddingUrl())
                .apiKey(config.getAuthorization() != null ? config.getAuthorization() : "lm-studio")
                .modelName(config.getEmbeddingModelName())
                .build();

        // Setup the Store (gRPC connection to Qdrant)
        this.embeddingStore = QdrantEmbeddingStore.builder()
                .host(config.getVectorDbHost())
                .port(config.getVectorDbPort())
                .collectionName(config.getVectorDbCollectionName())
                .useTls(false) // Required for local Docker setups
                .build();

        // Initial Load
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

            // To use a custom ID, we must use the addAll signature
            embeddingStore.addAll(
                Collections.singletonList(id), 
                Collections.singletonList(embedding), 
                Collections.singletonList(segment)
            );

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
                    segment.metadata().toMap().forEach((k, v) -> 
                        stringMeta.put(k, String.valueOf(v)));
                }

                results.add(new RetrievedDocument(id, segment != null ? segment.text() : "", stringMeta));
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
        if (!staticIds.isEmpty()) {
            embeddingStore.removeAll(staticIds);
            staticIds.clear();
        }
    }

    public void loadStaticDocumentsFromFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> ids = new ArrayList<>();
            List<TextSegment> segments = new ArrayList<>();
            StringBuilder chunkBuilder = new StringBuilder();
            String line;
            int chunkIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                chunkBuilder.append(line).append(" ");

                if (chunkBuilder.length() >= 1000) {
                    addChunkToLists(chunkBuilder.toString().trim(), chunkIndex++, file.getName(), ids, segments);
                    chunkBuilder.setLength(0);
                }
            }

            if (chunkBuilder.length() > 0) {
                addChunkToLists(chunkBuilder.toString().trim(), chunkIndex, file.getName(), ids, segments);
            }

            if (!segments.isEmpty()) {
                // Batch embed for massive performance gain
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(ids, embeddings, segments);
                this.staticIds.addAll(ids);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load static context from: " + path, e);
        }
    }

    private void addChunkToLists(String text, int idx, String fileName, List<String> ids, List<TextSegment> segments) {
        String id = "static_" + idx;
        Metadata metadata = new Metadata(Map.of("source", "static", "file", fileName));
        ids.add(id);
        segments.add(TextSegment.from(text, metadata));
    }
}