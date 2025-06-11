package matsimBinding;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ConfigGroup;

/**
 * Configuration for connecting MATSim to a Large Language Model (LLM) with optional retrieval-augmented generation (RAG).
 *
 * == LLM Setup ==
 * Supported providers:
 *   - Local models via LM Studio or Ollama
 *       > LM Studio: Load a model, enable "OpenAI-compatible API" (default port: 1234)
 *       > Ollama: Run `ollama run llama3` (default port: 11434)
 *
 *   - Remote via OpenAI
 *       > Use host = api.openai.com, protocol = HTTPS (default port 443, no need to set it)
 *       > Set `authorization` and optionally `organization`, `project`
 *
 * == Vector DB Setup ==
 * To run Chroma DB locally:
 *   docker run -p 8000:8000 ghcr.io/chroma-core/chroma:latest
 * This exposes:
 *   - http://localhost:8000/search
 *   - http://localhost:8000/insert
 */
public class LLMConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "llm";

    public LLMConfigGroup() {
        super(GROUP_NAME);
    }

    // === LLM Inference ===
    private String llmHost = "localhost";
    private int llmPort = 1234;
    private String llmPath = "/v1/chat/completions";
    private boolean useHttps = false;
    private String modelName = "gpt-3.5-turbo";
    
    private double temperature = 0.7;
    private int maxTokens = 2048;
    private int seed = 42;
    private String systemMessage = "You are an AI assistant.";

    // === Headers / Authentication (for OpenAI-compatible APIs) ===
    private String authorization;
    private String organization;
    private String project;

    // === Embeddings ===
    private String embeddingPath = "/v1/embeddings";

    // === Vector DB (RAG support) ===
    private String vectorDbHost = "localhost";
    private int vectorDbPort = 8000;
    private String vectorDbSearchPath = "/search";
    private String vectorDbInsertPath = "/insert";
    private String VectorDBSourceFile;

    // === Tool Calling ===
    private String toolSpecificationFile;
	

	
	// === Logging ===
	private boolean enableLogging = false;
	private String logFilePath = "llm_chat_log";
	
	@StringGetter("enableContextRetrieval")
	public boolean isEnableContextRetrieval() {
	    return enableContextRetrieval;
	}

	@StringSetter("enableContextRetrieval")
	public void setEnableContextRetrieval(boolean enableContextRetrieval) {
	    this.enableContextRetrieval = enableContextRetrieval;
	}
	
	// === Embedding backend selection for RAG ===
	private String embeddingFunction = "huggingface"; // default

	@StringGetter("embeddingFunction")
	public String getEmbeddingFunction() {
	    return embeddingFunction;
	}

	@StringSetter("embeddingFunction")
	public void setEmbeddingFunction(String embeddingFunction) {
	    this.embeddingFunction = embeddingFunction;
	}

	private boolean enableContextRetrieval = false;

    // === Getters / Setters ===

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

    @StringGetter("embeddingPath")
    public String getEmbeddingPath() { return embeddingPath; }
    @StringSetter("embeddingPath")
    public void setEmbeddingPath(String embeddingPath) { this.embeddingPath = embeddingPath; }

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
    public void setVectorDBSourceFile(String VectorDBSourceFile) {
        this.VectorDBSourceFile = VectorDBSourceFile;
    }
    
    
    @StringGetter("toolSpecificationFile")
    public String getToolSpecificationFile() { return toolSpecificationFile; }
    @StringSetter("toolSpecificationFile")
    public void setToolSpecificationFile(String toolSpecificationFile) {
        this.toolSpecificationFile = toolSpecificationFile;
    }
    
    @StringGetter("enableLogging")
    public boolean isEnableLogging() { return enableLogging; }

    @StringSetter("enableLogging")
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    @StringGetter("logFilePath")
    public String getLogFilePath() { return logFilePath; }

    @StringSetter("logFilePath")
    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }


    // === Computed Full URLs ===

    public String getFullLlmUrl() {
        return useHttps
            ? String.format("https://%s%s", llmHost, llmPath)
            : String.format("http://%s:%d%s", llmHost, llmPort, llmPath);
    }

    public String getFullEmbeddingUrl() {
        return useHttps
            ? String.format("https://%s%s", llmHost, embeddingPath)
            : String.format("http://%s:%d%s", llmHost, llmPort, embeddingPath);
    }

    public String getFullVectorDbBaseUrl() {
        return String.format("http://%s:%d", vectorDbHost, vectorDbPort);
    }


    
    public enum VectorDbCleanupMode {
        NONE,               // Leave everything intact
        DYNAMIC_ONLY,       // Clear just dynamic documents
        ALL                 // Clear the entire collection (static + dynamic)
    }
    
    private VectorDbCleanupMode cleanVectorDbUponCompletion = VectorDbCleanupMode.NONE;

    @StringGetter("cleanVectorDbUponCompletion")
    public String getCleanVectorDbUponCompletion() {
        return cleanVectorDbUponCompletion.name();
    }

    @StringSetter("cleanVectorDbUponCompletion")
    public void setCleanVectorDbUponCompletion(String value) {
        this.cleanVectorDbUponCompletion = VectorDbCleanupMode.valueOf(value.toUpperCase());
    }

    public VectorDbCleanupMode getCleanupModeEnum() {
        return cleanVectorDbUponCompletion;
    }

    private String vectorDbCollectionName = "dynamic_documents";

    @StringGetter("vectorDbCollectionName")
    public String getVectorDbCollectionName() {
        return vectorDbCollectionName;
    }

    @StringSetter("vectorDbCollectionName")
    public void setVectorDbCollectionName(String name) {
        this.vectorDbCollectionName = name;
    }


 // === Embedding model name ===
    private String embeddingModelName = "text-embedding-3-small"; // Default or any appropriate fallback

    @StringGetter("embeddingModelName")
    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    @StringSetter("embeddingModelName")
    public void setEmbeddingModelName(String embeddingModelName) {
        this.embeddingModelName = embeddingModelName;
    }

    
 // === Backend Selection ===

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

    /**
     * Returns the backend name as string (for MATSim XML config output).
     */
    @StringGetter("backend")
    public String getBackend() {
        return backend.name();
    }

    /**
     * Sets the backend type (case-insensitive). Accepted values: openai, lm_studio, ollama.
     */
    @StringSetter("backend")
    public void setBackend(String backend) {
        this.backend = BackendType.valueOf(backend.toUpperCase());
    }

    /**
     * Returns the enum value of the configured backend.
     */
    public BackendType getBackendEnum() {
        return backend;
    }

    
}
