package matsimBinding;

/**
 * Represents the backend provider used for LLM inference.
 * Determines how the request is formatted and how the response is parsed.
 */
public enum BackendType {
    /** OpenAI's official API (https://api.openai.com) */
    OPENAI,

    /** Locally hosted LM Studio using OpenAI-compatible API (http://localhost:1234) */
    LM_STUDIO,

    /** Placeholder for Ollama support (http://localhost:11434) */
    OLLAMA
}
