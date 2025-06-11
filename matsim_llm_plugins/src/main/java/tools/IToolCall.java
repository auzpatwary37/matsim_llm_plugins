package tools;


/**
 * Represents a tool call from the LLM (e.g., OpenAI, LM Studio, Ollama).
 * Mirrors the structure of a function call as returned in the LLM response.
 */
public interface IToolCall {

    /**
     * The unique identifier of this tool call (e.g., "call_abc123").
     */
    String getId();

    /**
     * The name of the tool being called (e.g., "get_fastest_route").
     * This is used to dispatch the call to a registered ITool.
     */
    String getName();

    /**
     * The raw JSON argument string (i.e., the `arguments` field inside the `function` object).
     * This will be passed to the ITool's `call()` method.
     */
    String getArguments();
}

