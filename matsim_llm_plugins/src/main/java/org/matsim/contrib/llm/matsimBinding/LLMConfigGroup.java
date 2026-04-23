package org.matsim.contrib.llm.matsimBinding;

import org.matsim.core.config.ReflectiveConfigGroup;
/**
 * Configuration group for all LLM-related components used in the MATSim–LLM integration.
 *
 * This class centralizes configuration for:
 *
 * 1. Chat Completion (LLM)
 *    - Host, port, and API path for OpenAI-compatible endpoints (e.g., LM Studio, OpenAI, Ollama)
 *    - Model name and authorization settings
 *
 * 2. Embedding Service
 *    - Separate endpoint and model for generating vector embeddings
 *    - Used exclusively for retrieval (RAG), not for generation
 *
 * 3. Vector Database (Qdrant)
 *    - Connection settings (host, port, collection name)
 *    - Supports storage and retrieval of both static and dynamic documents
 *    - Used for similarity search during context injection
 *
 * 4. Retrieval Configuration (RAG)
 *    - Path to static source files for initial vector DB population
 *    - Controls behavior of dynamic memory insertion and cleanup
 *
 * 5. Lifecycle / Cleanup Behavior
 *    - Defines how the vector database is handled after simulation:
 *        - No cleanup
 *        - Clean only dynamic entries
 *        - Clean both static and dynamic entries
 *
 * Notes:
 * - Chat completion and embedding endpoints may point to the same backend or different services.
 * - All endpoints are expected to follow OpenAI-compatible API formats.
 * - This configuration is consumed by ChatCompletionClient, VectorDBImplement (Qdrant),
 *   and ChatManager during runtime.

 * Central configuration for the MATSim–LLM integration.
 *
 * This config connects MATSim to:
 * - a chat model (LLM)
 * - an embedding model (for retrieval)
 * - a Qdrant database (for memory / RAG)
 *
 * It also controls optional loading of static reference data and cleanup behavior.
 *
 * --------------------------------------------------
 * Setup (Step-by-Step, Non-Technical)
 * --------------------------------------------------
 *
 * Step 1 — Start a Chat Model (LLM)
 *
 * Pick ONE option below:
 *
 * --------------------------------------------------
 * Option A: LM Studio (Recommended)
 * --------------------------------------------------
 *
 * 1. Download and install LM Studio
 * 2. Open the application
 * 3. Go to the "Models" tab
 * 4. Download a model (e.g., Qwen, LLaMA)
 * 5. Go to the "Local Server" tab
 * 6. Select the downloaded model
 * 7. Click "Start Server"
 *
 * IMPORTANT:
 *   Copy the EXACT model name shown in LM Studio.
 *   You MUST set:
 *       llmModelName = "<that exact name>"
 *
 * You know it is working if:
 *   → You see: "Server running on http://localhost:1234"
 *
 * Use:
 *   llmHost = "http://localhost"
 *   llmPort = 1234
 *   llmPath = "/v1/chat/completions"
 *
 *
 * --------------------------------------------------
 * Option B: Ollama
 * --------------------------------------------------
 *
 * 1. Install Ollama
 * 2. Open terminal / command prompt
 * 3. Run:
 *        ollama run qwen3.5:9b
 *    (this downloads the model)
 *
 * 4. Then run:
 *        ollama serve
 *
 * IMPORTANT:
 *   Use the SAME model name:
 *       llmModelName = "qwen3.5:9b"
 *
 * You know it is working if:
 *   → Terminal shows no errors and stays running
 *
 * Use:
 *   llmHost = "http://localhost"
 *   llmPort = 11434
 *   llmPath = "/v1/chat/completions"
 *
 *
 * --------------------------------------------------
 * Option C: OpenAI
 * --------------------------------------------------
 *
 * 1. Get an API key from OpenAI
 * 2. Choose a model (e.g., gpt-4o)
 *
 * IMPORTANT:
 *   Set:
 *       llmModelName = "gpt-4o"
 *
 * Use:
 *   llmHost = "https://api.openai.com"
 *   llmPath = "/v1/chat/completions"
 *   authorization = "Bearer YOUR_API_KEY"
 *
 *
 * --------------------------------------------------
 * Step 2 — Set Up Embeddings
 * --------------------------------------------------
 *
 * This is required for retrieval (RAG).
 *
 * Most backends already support embeddings.
 *
 * Use:
 *   embeddingPath = "/v1/embeddings"
 *
 * IMPORTANT:
 *   You MUST also set:
 *       embeddingModelName
 *
 * Example:
 *   - OpenAI: "text-embedding-3-small"
 *   - LM Studio: embedding-capable model
 *   - Ollama: installed embedding model
 *
 *
 * --------------------------------------------------
 * Step 3 — Start Qdrant (Database)
 * --------------------------------------------------
 *
 * Qdrant stores and retrieves memory (RAG).
 *
 * Recommended method (NO terminal required):
 *
 * 1. Install Docker Desktop
 * 2. Open Docker Desktop
 * 3. Go to the "Images" tab
 * 4. Search for:
 *        qdrant/qdrant
 * 5. Click "Pull"
 * 6. After download, click "Run"
 *
 * 7. In container settings:
 *      Add port mapping:
 *          6334 → 6334   (IMPORTANT: gRPC port)
 *
 *      (Optional, for browser check):
 *          6333 → 6333
 *
 * 8. Start the container
 *
 *
 * You know it is working if:
 *
 *   Option A:
 *     Docker shows container status = "Running"
 *
 *   Option B (optional check):
 *     Open browser → http://localhost:6333
 *     → You see JSON response
 *
 *
 * Use:
 *   vectorDbHost = "localhost"
 *   vectorDbPort = 6334   // IMPORTANT: gRPC port
 *   vectorDbCollectionName = "<any name>"
 *
 * Notes:
 *   - Collection will be created automatically if missing
 *   - Port 6334 is REQUIRED for Java Qdrant client
 *
 *
 * --------------------------------------------------
 * Step 4 — Optional: Load a File
 * --------------------------------------------------
 *
 * If you want background knowledge:
 *   vectorDbSourceFile = "path/to/file"
 *
 * This file will be:
 *   read → split → embedded → stored in Qdrant
 *
 *
 * --------------------------------------------------
 * Quick Check (Very Important)
 * --------------------------------------------------
 *
 * Before running MATSim:
 *
 * 1. Open browser:
 *     http://localhost:1234   (LM Studio)
 *     http://localhost:6333   (Qdrant)
 *
 * 2. If page does NOT open → service is NOT running
 *
 *
 * --------------------------------------------------
 * Notes
 * --------------------------------------------------
 *
 * - Model name MUST exactly match backend model
 * - If model name is wrong → requests will fail
 * - All services must be running BEFORE starting MATSim
 */
public class LLMConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "llm";

    public LLMConfigGroup() {
        super(GROUP_NAME);
    }

    // ========================================================================
    // BACKEND CONFIGURATION
    // ========================================================================

    /**
     * The backend provider for LLM communication.
     * Determines request format, headers, and response parsing.
     *
     * Supported values:
     * - OPENAI:     Connects to https://api.openai.com using official OpenAI API.
     * - LM_STUDIO:  Connects to local LM Studio using OpenAI-compatible API.
     * - OLLAMA:     Future support for Ollama (http://localhost:11434).
     *
     * Default: LM_STUDIO
     */
    private BackendType backend = BackendType.LM_STUDIO;

    // ========================================================================
    // LLM CONNECTION SETTINGS
    // ========================================================================

    /** Hostname or IP address of the LLM server (default: localhost for local models) */
    private String llmHost = "localhost";
    
    /** Port number for the LLM server (default: 1234 for LM Studio, 11434 for Ollama) */
    private int llmPort = 1234;
    
    /** API endpoint path for chat completions (OpenAI-compatible: /v1/chat/completions) */
    private String llmPath = "/v1/chat/completions";
    
    /** Whether to use HTTPS protocol (true for remote APIs like OpenAI, false for local) */
    private boolean useHttps = false;

    // ========================================================================
    // MODEL CONFIGURATION
    // ========================================================================

    /** Name of the model to use (e.g., "gpt-3.5-turbo", "llama3", or local model name) */
    private String modelName = "gpt-3.5-turbo";
    
    /** Temperature for response generation (0.0 = deterministic, 1.0 = creative, range: 0.0-2.0) */
    private double temperature = 0.7;
    
    /** Maximum number of tokens in the model's response */
    private int maxTokens = 2048;
    
    /** Random seed for reproducible outputs (use same seed for consistent results) */
    private int seed = 42;
    
    /** System message that sets the AI assistant's behavior and context */
    private String systemMessage = "You are an AI assistant.";

    // ========================================================================
    // AUTHENTICATION & HEADERS
    // ========================================================================

    /** Authorization header value (e.g., "Bearer sk-..." for OpenAI API key) */
    private String authorization;
    
    /** OpenAI organization ID (optional, for organization-scoped requests) */
    private String organization;
    
    /** OpenAI project ID (optional, for project-scoped requests) */
    private String project;

    // ========================================================================
    // EMBEDDING CONFIGURATION
    // ========================================================================

    /** API endpoint path for generating embeddings (OpenAI-compatible: /v1/embeddings) */
    private String embeddingPath = "/v1/embeddings";
    
    /** Name of the embedding model (e.g., "text-embedding-3-small" for OpenAI) */
    private String embeddingModelName = "text-embedding-3-small";
    
    /** 
     * Embedding backend selection for RAG functionality.
     * Supported values: "huggingface", "openai"
     * Default: "huggingface"
     */
    private String embeddingFunction = "huggingface";

    // ========================================================================
    // VECTOR DATABASE (RAG) CONFIGURATION
    // ========================================================================

    /** Hostname or IP address of the vector database server (default: localhost) */
    private String vectorDbHost = "localhost";
    
    /** Port number for the vector database server (default: 6334 for Qdrant) */
    private int vectorDbPort = 6334;
    
    /** API endpoint path for searching the vector database */
    private String vectorDbSearchPath = "/search";
    
    /** API endpoint path for inserting documents into the vector database */
    private String vectorDbInsertPath = "/insert";
    
    /** 
     * File path to documents that should be loaded into the vector database as static content.
     * These documents form the knowledge base for RAG and remain persistent.
     */
    private String VectorDBSourceFile;
    
    /** 
     * Name of the vector database collection to use.
     * Default: "dynamic_documents" 
     */
    private String vectorDbCollectionName = "dynamic_documents";
    
    /** Whether to enable context retrieval from vector database for RAG functionality */
    private boolean enableContextRetrieval = false;

    /**
     * Cleanup strategy for vector database upon completion.
     * - NONE: Leave everything intact (static + dynamic documents)
     * - DYNAMIC_ONLY: Clear only runtime-added documents, keep static documents
     * - ALL: Clear the entire collection (static + dynamic documents)
     */
    private VectorDbCleanupMode cleanVectorDbUponCompletion = VectorDbCleanupMode.NONE;

    // ========================================================================
    // TOOL CALLING CONFIGURATION
    // ========================================================================

    /** 
     * File path to JSON/YAML specification defining available tools/functions that the LLM can call.
     * This enables the model to use external tools and APIs during conversations.
     */
    private String toolSpecificationFile;
    
    private int maxToolIterations = 10;

    // ========================================================================
    // LOGGING CONFIGURATION
    // ========================================================================

    /** Whether to enable comprehensive logging of LLM interactions */
    private boolean enableLogging = false;
    
    /** File path prefix for LLM chat logs (timestamp will be appended) */
    private String logFilePath = "llm_chat_log";
    

    // ========================================================================
    // AI CONTROL CONFIGURATION
    // ========================================================================

    
    /**
     * Number of agents that will use AI-based decision making.
     * If <= 0, all agents are eligible.
     * 
     * 
     */
    
    private int numberOfAIAgents = 100;

    /**
     * MATSim iteration from which AI behavior should start.
     * Before this iteration, default behavior is used.
     */
    private int iterationToStartAIActivity = 50;
    
    

    // ========================================================================
    // BACKEND ENUM DEFINITION
    // ========================================================================

    /**
     * Supported LLM backend types for different deployment scenarios.
     */
    public enum BackendType {
        /** OpenAI's official API (https://api.openai.com) */
        OPENAI,
        
        /** Locally hosted LM Studio using OpenAI-compatible API (http://localhost:1234) */
        LM_STUDIO,
        
        /** Placeholder for Ollama support (http://localhost:11434) */
        OLLAMA
    }

    /**
     * Vector database cleanup modes for different use cases.
     */
    public enum VectorDbCleanupMode {
        /** Leave everything intact (static + dynamic documents) */
        NONE,
        
        /** Clear just runtime-added documents, preserve static knowledge base */
        DYNAMIC_ONLY,
        
        /** Clear the entire collection (static + dynamic documents) */
        ALL
    }

    // ========================================================================
    // BACKEND CONFIGURATION - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("backend")
    public String getBackend() {
        return backend.name();
    }

    @StringSetter("backend")
    public void setBackend(BackendType backend) {
        this.backend = backend;
    }

    public BackendType getBackendEnum() {
        return backend;
    }

    // ========================================================================
    // LLM CONNECTION - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("llmHost")
    public String getLlmHost() { return llmHost; }
    
    @StringSetter("llmHost")
    public void setLlmHost(String llmHost) { this.llmHost = llmHost; }

    @StringGetter("llmPort")
    public int getLlmPort() { return llmPort; }
    
    @StringSetter("llmPort")
    public void setLlmPort(int llmPort) { this.llmPort = llmPort; }

    @StringGetter("llmPath")
    public String getLlmPath() { return llmPath; }
    
    @StringSetter("llmPath")
    public void setLlmPath(String llmPath) { this.llmPath = llmPath; }

    @StringGetter("useHttps")
    public boolean isUseHttps() { return useHttps; }
    
    @StringSetter("useHttps")
    public void setUseHttps(boolean useHttps) { this.useHttps = useHttps; }

    // ========================================================================
    // MODEL CONFIGURATION - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("modelName")
    public String getModelName() { return modelName; }
    
    @StringSetter("modelName")
    public void setModelName(String modelName) { this.modelName = modelName; }

    @StringGetter("temperature")
    public double getTemperature() { return temperature; }
    
    @StringSetter("temperature")
    public void setTemperature(double temperature) { this.temperature = temperature; }

    @StringGetter("maxTokens")
    public int getMaxTokens() { return maxTokens; }
    
    @StringSetter("maxTokens")
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    @StringGetter("seed")
    public int getSeed() { return seed; }
    
    @StringSetter("seed")
    public void setSeed(int seed) { this.seed = seed; }

    @StringGetter("systemMessage")
    public String getSystemMessage() { return systemMessage; }
    
    @StringSetter("systemMessage")
    public void setSystemMessage(String systemMessage) { this.systemMessage = systemMessage; }

    // ========================================================================
    // AUTHENTICATION - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("authorization")
    public String getAuthorization() { return authorization; }
    
    @StringSetter("authorization")
    public void setAuthorization(String authorization) { this.authorization = authorization; }

    @StringGetter("organization")
    public String getOrganization() { return organization; }
    
    @StringSetter("organization")
    public void setOrganization(String organization) { this.organization = organization; }

    @StringGetter("project")
    public String getProject() { return project; }
    
    @StringSetter("project")
    public void setProject(String project) { this.project = project; }

    // ========================================================================
    // EMBEDDING CONFIGURATION - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("embeddingPath")
    public String getEmbeddingPath() { return embeddingPath; }
    
    @StringSetter("embeddingPath")
    public void setEmbeddingPath(String embeddingPath) { this.embeddingPath = embeddingPath; }

    @StringGetter("embeddingModelName")
    public String getEmbeddingModelName() { return embeddingModelName; }
    
    @StringSetter("embeddingModelName")
    public void setEmbeddingModelName(String embeddingModelName) { this.embeddingModelName = embeddingModelName; }

    @StringGetter("embeddingFunction")
    public String getEmbeddingFunction() { return embeddingFunction; }
    
    @StringSetter("embeddingFunction")
    public void setEmbeddingFunction(String embeddingFunction) { this.embeddingFunction = embeddingFunction; }

    // ========================================================================
    // VECTOR DATABASE - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("vectorDbHost")
    public String getVectorDbHost() { return vectorDbHost; }
    
    @StringSetter("vectorDbHost")
    public void setVectorDbHost(String vectorDbHost) { this.vectorDbHost = vectorDbHost; }

    @StringGetter("vectorDbPort")
    public int getVectorDbPort() { return vectorDbPort; }
    
    @StringSetter("vectorDbPort")
    public void setVectorDbPort(int vectorDbPort) { this.vectorDbPort = vectorDbPort; }

    @StringGetter("vectorDbSearchPath")
    public String getVectorDbSearchPath() { return vectorDbSearchPath; }
    
    @StringSetter("vectorDbSearchPath")
    public void setVectorDbSearchPath(String vectorDbSearchPath) { this.vectorDbSearchPath = vectorDbSearchPath; }

    @StringGetter("vectorDbInsertPath")
    public String getVectorDbInsertPath() { return vectorDbInsertPath; }
    
    @StringSetter("vectorDbInsertPath")
    public void setVectorDbInsertPath(String vectorDbInsertPath) { this.vectorDbInsertPath = vectorDbInsertPath; }

    @StringGetter("VectorDBSourceFile")
    public String getVectorDBSourceFile() { return VectorDBSourceFile; }
    
    @StringSetter("VectorDBSourceFile")
    public void setVectorDBSourceFile(String VectorDBSourceFile) { this.VectorDBSourceFile = VectorDBSourceFile; }

    @StringGetter("vectorDbCollectionName")
    public String getVectorDbCollectionName() { return vectorDbCollectionName; }
    
    @StringSetter("vectorDbCollectionName")
    public void setVectorDbCollectionName(String name) { this.vectorDbCollectionName = name; }

    @StringGetter("enableContextRetrieval")
    public boolean isEnableContextRetrieval() { return enableContextRetrieval; }
    
    @StringSetter("enableContextRetrieval")
    public void setEnableContextRetrieval(boolean enableContextRetrieval) { this.enableContextRetrieval = enableContextRetrieval; }

    @StringGetter("cleanVectorDbUponCompletion")
    public String getCleanVectorDbUponCompletion() { return cleanVectorDbUponCompletion.name(); }
    
    @StringSetter("cleanVectorDbUponCompletion")
    public void setCleanVectorDbUponCompletion(String value) { 
        this.cleanVectorDbUponCompletion = VectorDbCleanupMode.valueOf(value.toUpperCase()); 
    }

    public VectorDbCleanupMode getCleanupModeEnum() { return cleanVectorDbUponCompletion; }

    // ========================================================================
    // TOOL CALLING - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("toolSpecificationFile")
    public String getToolSpecificationFile() { return toolSpecificationFile; }
    
    @StringSetter("toolSpecificationFile")
    public void setToolSpecificationFile(String toolSpecificationFile) { this.toolSpecificationFile = toolSpecificationFile; }

    // ========================================================================
    // LOGGING - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("enableLogging")
    public boolean isEnableLogging() { return enableLogging; }

    @StringSetter("enableLogging")
    public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }

    @StringGetter("logFilePath")
    public String getLogFilePath() { return logFilePath; }

    @StringSetter("logFilePath")
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
    
    // ========================================================================
    // AI CONTROL - GETTERS & SETTERS
    // ========================================================================

    @StringGetter("numberOfAIAgents")
    public int getNumberOfAIAgents() {
    	return numberOfAIAgents;
    }

    @StringSetter("numberOfAIAgents")
    public void setNumberOfAIAgents(int numberOfAIAgents) {
    	this.numberOfAIAgents = numberOfAIAgents;
    }

    @StringGetter("iterationToStartAIActivity")
    public int getIterationToStartAIActivity() {
    	return iterationToStartAIActivity;
    }

    @StringSetter("iterationToStartAIActivity")
    public void setIterationToStartAIActivity(int iterationToStartAIActivity) {
    	this.iterationToStartAIActivity = iterationToStartAIActivity;
    }

    @StringGetter("maxToolIterations")
    public int getMaxToolIterations() {
        return maxToolIterations;
    }

    @StringSetter("maxToolIterations")
    public void setMaxToolIterations(int maxToolIterations) {
        this.maxToolIterations = maxToolIterations;
    }
    // ========================================================================
    // UTILITY METHODS - URL CONSTRUCTION
    // ========================================================================

    /**
     * Constructs the full URL for LLM chat completions endpoint.
     * Handles both HTTP (local) and HTTPS (remote) protocols.
     * 
     * @return Complete URL for LLM inference requests
     */
    public String getFullLlmUrl() {
        return useHttps
            ? String.format("https://%s%s", llmHost, llmPath)
            : String.format("http://%s:%d%s", llmHost, llmPort, llmPath);
    }

    /**
     * Constructs the full URL for embedding generation endpoint.
     * Handles both HTTP (local) and HTTPS (remote) protocols.
     * 
     * @return Complete URL for embedding requests
     */
    public String getFullEmbeddingUrl() {
        return useHttps
            ? String.format("https://%s%s", llmHost, embeddingPath)
            : String.format("http://%s:%d%s", llmHost, llmPort, embeddingPath);
    }

    /**
     * Constructs the base URL for vector database operations.
     * Always uses HTTP protocol for local vector database.
     * 
     * @return Base URL for vector database requests
     */
    public String getFullVectorDbBaseUrl() {
    	return String.format("http://%s:%d", vectorDbHost, vectorDbPort);
    }

    

}