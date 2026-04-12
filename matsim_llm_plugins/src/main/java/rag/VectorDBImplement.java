package rag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import matsimBinding.LLMConfigGroup;
import matsimBinding.LLMConfigGroup.VectorDbCleanupMode;

public class VectorDBImplement implements IVectorDB {

    private static final int DEFAULT_STATIC_CHUNK_SIZE = 1000;
    private static final int DEFAULT_STATIC_CHUNK_OVERLAP = 150;

    private final LLMConfigGroup config;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;

    private final Set<String> staticIds = new LinkedHashSet<>();
    private final Set<String> dynamicIds = new LinkedHashSet<>();

    private volatile boolean initialized = false;
    

    public VectorDBImplement(LLMConfigGroup config) {
        if (config == null) {
            throw new IllegalArgumentException("LLMConfigGroup cannot be null");
        }
        this.config = config;
        this.initialize();
    }
    
    private long lastExecutionTime = 0;
    
    private List<String> pendingIds = new ArrayList<>();
    private List<TextSegment> pendingTextSegment = new ArrayList<>();
    private int batchSize = 8;

    @Override
    public synchronized void initialize() {
        if (initialized) {
            System.out.println("VectorDBImplement already initialized. Skipping re-initialization.");
            return;
        }

        validateConfig();

        try {
            String embeddingBaseUrl = normalizeEmbeddingBaseUrl(config.getFullEmbeddingUrl());
            
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1);

            JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                    .httpClientBuilder(httpClientBuilder);

            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl(embeddingBaseUrl)
                    .apiKey(config.getAuthorization() != null ? config.getAuthorization() : "lm-studio")
                    .modelName(config.getEmbeddingModelName())
                    .httpClientBuilder(jdkHttpClientBuilder)
                    .build();
            
            embeddingModel.embed("asdfa");

            ensureCollectionExists();

            this.embeddingStore = QdrantEmbeddingStore.builder()
                    .host(config.getVectorDbHost())
                    .port(config.getVectorDbPort())
                    .collectionName(config.getVectorDbCollectionName())
                    .useTls(false)
                    .build();

            System.out.println("Qdrant embedding store initialized for collection: "
                    + config.getVectorDbCollectionName());

            String staticSourcePath = config.getVectorDBSourceFile();
            if (staticSourcePath != null && !staticSourcePath.isBlank()) {
                this.loadStaticDocumentsFromFile(staticSourcePath);
            } else {
                System.out.println("No static source file configured. Skipping static document load.");
            }

            initialized = true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize VectorDBImplement. Are your servers running? Check both the LLM and RAG servers.", e);
        }
    }
    public static String createUUID() { return java.util.UUID.randomUUID().toString(); }

    @Override
    public synchronized String insert(String content, Map<String, String> metadata) {
        ensureInitialized();
        
        String id = createUUID();

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Document id cannot be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Document content cannot be null or blank");
        }

        try {
            Metadata lcMetadata = metadata == null
                    ? new Metadata()
                    : new Metadata(new HashMap<>(metadata));

            TextSegment segment = TextSegment.from(content, lcMetadata);
            
//            Embedding embedding = embeddingModel.embed(content).content();
            
            this.pendingIds.add(id);
            this.pendingTextSegment.add(segment);
            
            if(this.pendingIds.size()>=this.batchSize) {	
            	this.flush();
            }

            if (!id.startsWith("static_")) {
                dynamicIds.add(id);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert document into Qdrant: " + id, e);
        }
        return id;
    }
    
    public synchronized void flush() {
    	
    	if(this.pendingIds.isEmpty())return;
    	
    	 List<String> idsToFlush = new ArrayList<>(this.pendingIds);
    	 List<TextSegment> segmentsToFlush = new ArrayList<>(this.pendingTextSegment);
    	
		Response<List<Embedding>> embeddingResponse = this.embeddingModel.embedAll(segmentsToFlush);
		
		this.embeddingStore.addAll(
				idsToFlush,
                embeddingResponse.content(),
                segmentsToFlush
        );
        this.lastExecutionTime =  System.currentTimeMillis();
        this.pendingIds.clear();
        this.pendingTextSegment.clear();
        
    }

    @Override
    public List<RetrievedDocument> query(String prompt, int topK) {
        ensureInitialized();
        this.flush();

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Query prompt cannot be null or blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }

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
                    segment.metadata().toMap().forEach((k, v) -> stringMeta.put(k, String.valueOf(v)));
                }

                RetrievedDocument doc = new RetrievedDocument(
                        id,
                        segment != null ? segment.text() : "",
                        stringMeta
                );

                // If your RetrievedDocument already supports score, replace this with constructor/setter usage.
                // Example:
                // doc.setScore(match.score());

                results.add(doc);
            }

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Failed to query Qdrant", e);
        }
    }

    
    private Filter buildFilter(Map<String, String> metaDataFilter) {
        if (metaDataFilter == null || metaDataFilter.isEmpty()) {
            return null;
        }

        Filter combined = null;

        for (Map.Entry<String, String> entry : metaDataFilter.entrySet()) {
            Filter condition = new IsEqualTo(entry.getKey(), entry.getValue());

            if (combined == null) {
                combined = condition; // first condition
            } else {
                combined = new And(combined, condition); // chain
            }
        }

        return combined;
    }
    
    @Override
    public List<RetrievedDocument> query(String prompt, int topK, Map<String,String> metaDataFilter) {
        ensureInitialized();
        this.flush();

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Query prompt cannot be null or blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(prompt).content();
            List<Filter> conditions = new ArrayList<>();
            
            Filter filter = this.buildFilter(metaDataFilter);
            
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .filter(filter)
                    .maxResults(topK)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
            List<RetrievedDocument> results = new ArrayList<>();

            for (EmbeddingMatch<TextSegment> match : matches) {
                String id = match.embeddingId();
                TextSegment segment = match.embedded();

                Map<String, String> stringMeta = new HashMap<>();
                if (segment != null && segment.metadata() != null) {
                    segment.metadata().toMap().forEach((k, v) -> stringMeta.put(k, String.valueOf(v)));
                }

                RetrievedDocument doc = new RetrievedDocument(
                        id,
                        segment != null ? segment.text() : "",
                        stringMeta
                );

                // If your RetrievedDocument already supports score, replace this with constructor/setter usage.
                // Example:
                // doc.setScore(match.score());

                results.add(doc);
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
        ensureInitialized();

        VectorDbCleanupMode mode = config.getCleanupModeEnum();
        if (mode != VectorDbCleanupMode.DYNAMIC_ONLY && mode != VectorDbCleanupMode.ALL) {
            System.out.println("Dynamic cleanup skipped because cleanup mode is: " + mode);
            return;
        }

        if (dynamicIds.isEmpty()) {
            System.out.println("No dynamic documents to remove.");
            return;
        }

        try {
            embeddingStore.removeAll(new ArrayList<>(dynamicIds));
            System.out.println("Removed dynamic documents: " + dynamicIds.size());
            dynamicIds.clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear dynamic documents", e);
        }
    }

    @Override
    public void clearStaticDocuments() {
        ensureInitialized();

        if (staticIds.isEmpty()) {
            System.out.println("No static documents to remove.");
            return;
        }

        try {
            embeddingStore.removeAll(new ArrayList<>(staticIds));
            System.out.println("Removed static documents: " + staticIds.size());
            staticIds.clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear static documents", e);
        }
    }

    public synchronized void loadStaticDocumentsFromFile(String path) {
        ensureInitializedForStaticLoad();

        if (path == null || path.isBlank()) {
            System.out.println("Static document load skipped: path is null or blank.");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Static document load skipped: file does not exist: " + path);
            return;
        }
        if (!file.isFile()) {
            System.out.println("Static document load skipped: not a regular file: " + path);
            return;
        }

        try {
            List<String> ids = new ArrayList<>();
            List<TextSegment> segments = new ArrayList<>();

            String fullText = readWholeFile(file);
            List<String> chunks = chunkText(fullText, DEFAULT_STATIC_CHUNK_SIZE, DEFAULT_STATIC_CHUNK_OVERLAP);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                if (chunk == null || chunk.isBlank()) {
                    continue;
                }

                String id = createUUID();
                Metadata metadata = new Metadata(Map.of(
                        "source", "static",
                        "file", file.getName(),
                        "chunk_index", String.valueOf(i)
                ));

                ids.add(id);
                segments.add(TextSegment.from(chunk, metadata));
            }

            if (segments.isEmpty()) {
                System.out.println("No static chunks produced from file: " + file.getAbsolutePath());
                return;
            }

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(ids, embeddings, segments);
            staticIds.addAll(ids);

            System.out.println("Loaded static document chunks: " + ids.size()
                    + " from file: " + file.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load static context from: " + path, e);
        }
    }

    private void validateConfig() {
        requireNonBlank(config.getVectorDbHost(), "Vector DB host");
        requireNonBlank(config.getVectorDbCollectionName(), "Vector DB collection name");
        requireNonBlank(config.getFullEmbeddingUrl(), "Embedding base URL");
        requireNonBlank(config.getEmbeddingModelName(), "Embedding model name");

        if (config.getVectorDbPort() <= 0) {
            throw new IllegalArgumentException("Vector DB port must be > 0");
        }
    }

    private void ensureCollectionExists() {
        String host = config.getVectorDbHost();
        int port = config.getVectorDbPort();
        String collectionName = config.getVectorDbCollectionName();

        try (QdrantClient client =
                     new QdrantClient(QdrantGrpcClient.newBuilder(host, port, false).build())) {

            boolean exists = client.listCollectionsAsync().get().contains(collectionName);
            if (exists) {
                System.out.println("Qdrant collection already exists: " + collectionName);
                return;
            }

            int inferredDimension = inferEmbeddingDimension();

            client.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setDistance(Distance.Cosine)
                            .setSize(inferredDimension)
                            .build()
            );

            System.out.println("Created Qdrant collection: " + collectionName
                    + " with dimension: " + inferredDimension);

        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure Qdrant collection exists: " + collectionName, e);
        }
    }

    private int inferEmbeddingDimension() {
        try {
            Embedding probe = embeddingModel.embed("dimension_probe").content();
            if (probe == null || probe.vector() == null || probe.vector().length == 0) {
                throw new IllegalStateException("Embedding model returned empty vector during dimension probe");
            }
            return probe.vector().length;
        } catch (Exception e) {
            throw new RuntimeException("Failed to infer embedding dimension from model", e);
        }
    }

    private String normalizeEmbeddingBaseUrl(String url) {
        String normalized = url.trim();
        if (normalized.endsWith("/embeddings")) {
            normalized = normalized.substring(0, normalized.length() - "/embeddings".length());
        }
        return normalized;
    }

    private String buildStaticId(String fileName, int chunkIndex) {
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "static_" + safeFileName + "_" + chunkIndex;
    }

    private String readWholeFile(File file) {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean first = true;

            while ((line = reader.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + file.getAbsolutePath(), e);
        }

        return sb.toString();
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap must be >= 0 and < chunkSize");
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));

            if (end == normalized.length()) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }

    private void ensureInitialized() {
        if (!initialized || embeddingModel == null || embeddingStore == null) {
            throw new IllegalStateException("VectorDBImplement is not initialized");
        }
    }

    private void ensureInitializedForStaticLoad() {
        if (embeddingModel == null || embeddingStore == null) {
            throw new IllegalStateException("Embedding model/store must be initialized before loading static documents");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}