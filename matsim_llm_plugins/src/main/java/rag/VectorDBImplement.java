package rag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matsimBinding.LLMConfigGroup;
import matsimBinding.LLMConfigGroup.VectorDbCleanupMode;
import tech.amikos.chromadb.ChromaException;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;

public class VectorDBImplement implements IVectorDB {

    private final LLMConfigGroup config;
    private Client client;
    private Collection collection;
    private EmbeddingFunction embeddingFunction;
    private final List<String> staticIds = new ArrayList<>();
    private final List<String> dynamicIds = new ArrayList<>();


    public VectorDBImplement(LLMConfigGroup config) {
        this.config = config;
        this.initialize();
    }

    @Override
    public void initialize() {
        // Initialize ChromaDB client
        this.client = new Client(config.getFullVectorDbBaseUrl());

        // Initialize embedding function based on embedding path + model
        try {
			this.embeddingFunction = new OpenAIEmbeddingFunction(
			    WithParam.baseAPI(config.getFullEmbeddingUrl()),
			    WithParam.model(config.getEmbeddingModelName()),
			    WithParam.apiKey(config.getAuthorization() != null ? config.getAuthorization() : "lm-studio")
			);
		} catch (EFException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Create or load collection
        try {
            this.collection = client.createCollection(
                config.getVectorDbCollectionName(),
                null, // metadata
                true, // get_or_create
                embeddingFunction
            );
        } catch (ApiException e) {
            throw new RuntimeException("Failed to initialize ChromaDB collection: " + e.getMessage(), e);
        }
        this.loadStaticDocumentsFromFile(this.config.getVectorDBSourceFile());
    }

    @Override
    public void insert(String id, String content, Map<String, String> metadata) {
        try {
        	
        	
            collection.add(null,
                           Collections.singletonList(metadata),
                           Collections.singletonList(content),
                           Collections.singletonList(id));
            if (!id.startsWith("static_")) {
                this.dynamicIds.add(id);
            }
            
        }catch (ChromaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public List<RetrievedDocument> query(String prompt, int topK) {
        try {
            Collection.QueryResponse response = collection.query(
                Collections.singletonList(prompt),
                topK,
                null,
                null,
                null
            );

            List<String> docs = response.getDocuments().isEmpty() ? List.of() : response.getDocuments().get(0);
            List<String> ids = response.getIds().isEmpty() ? List.of() : response.getIds().get(0);
            List<Map<String, Object>> metadatas = response.getMetadatas().isEmpty() ? List.of() : response.getMetadatas().get(0);

            List<RetrievedDocument> results = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) {
                String id = ids.size() > i ? ids.get(i) : "";
                String content = docs.get(i);
                Map<String, Object> rawMeta = metadatas.size() > i ? metadatas.get(i) : Map.of();

                // Convert metadata to <String, String>
                Map<String, String> stringMeta = new HashMap<>();
                for (Map.Entry<String, Object> entry : rawMeta.entrySet()) {
                    stringMeta.put(entry.getKey(), String.valueOf(entry.getValue()));
                }

                results.add(new RetrievedDocument(id, content, stringMeta));
            }

            return results;

        } catch (ChromaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

    @Override
    public String getEmbeddingModelName() {
        return this.config.getEmbeddingModelName();
    }



    @Override
    public void clearDynamicDocuments() {
        try {
            if (config.getCleanupModeEnum() == VectorDbCleanupMode.DYNAMIC_ONLY
                || config.getCleanupModeEnum() == VectorDbCleanupMode.ALL) {
                
                if (!dynamicIds.isEmpty()) {
                    collection.deleteWithIds(dynamicIds);
                    dynamicIds.clear();
                }
            }
        } catch (ApiException e) {
            throw new RuntimeException("Failed to clear dynamic documents: " + e.getMessage(), e);
        }
    }

    @Override
    public void clearStaticDocuments() {
        if (staticIds.isEmpty()) return;
        try {
            collection.deleteWithIds(staticIds);
        } catch (ApiException e) {
            throw new RuntimeException("Failed to clear static documents: " + e.getMessage(), e);
        }
    }
    
    public void loadStaticDocumentsFromFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid static context file path: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder chunkBuilder = new StringBuilder();
            int chunkIndex = 0;

            List<String> contents = new ArrayList<>();
            List<Map<String, String>> metadatas = new ArrayList<>();
            List<String> ids = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                chunkBuilder.append(line).append(" ");

                if (chunkBuilder.length() >= 1000) { // basic length-based chunking
                    String chunk = chunkBuilder.toString().trim();
                    String id = "static_" + chunkIndex++;
                    contents.add(chunk);
                    ids.add(id);
                    metadatas.add(Map.of("source", "static", "file", file.getName()));
                    staticIds.add(id);
                    chunkBuilder.setLength(0); // reset
                }
            }

            if (chunkBuilder.length() > 0) {
                String chunk = chunkBuilder.toString().trim();
                String id = "static_" + chunkIndex++;
                contents.add(chunk);
                ids.add(id);
                metadatas.add(Map.of("source", "static", "file", file.getName()));
                staticIds.add(id);
            }

            collection.add(null,metadatas, contents, ids);
        } catch (IOException | ChromaException e) {
            throw new RuntimeException("Failed to load static context from file: " + path, e);
        } 
    }

}
