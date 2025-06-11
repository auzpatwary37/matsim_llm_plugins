package rag;


import java.util.List;
import java.util.Map;

public interface IVectorDB {

    /**
     * Initializes the vector database system from LLMConfigGroup.
     * Loads static documents if a source file is specified.
     */
    void initialize();

    /**
     * Inserts a new text entry into the vector database.
     *
     * @param id       Unique identifier for the entry
     * @param content  Raw text to embed and store
     * @param metadata Optional metadata for filtering or retrieval context
     */
    void insert(String id, String content, Map<String, String> metadata);

    /**
     * Queries the vector store using semantic similarity.
     *
     * @param prompt The natural language query
     * @param topK   Maximum number of relevant results to return
     * @return List of retrieved documents
     */
    List<RetrievedDocument> query(String prompt, int topK);

    /**
     * Returns the name or ID of the embedding model used.
     */
    String getEmbeddingModelName();

    /**
     * Clears any dynamically added content (excluding static context if present).
     */
    void clearDynamicDocuments();

    /**
     * Represents one retrieved item from the vector store.
     */
    record RetrievedDocument(String id, String content, Map<String, String> metadata) {}

    /**
     * Clears the static document from the vector database. 
     */
	void clearStaticDocuments();
}

