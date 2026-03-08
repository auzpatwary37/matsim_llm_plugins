package tools;

/**
 * Represents the result of executing a tool call.
 *
 * IMPORTANT:
 * - responseJson is what gets sent back to the LLM (as role="tool" message content).
 * - T (toolCallOutputContainer) is the actual Java object for MATSim / internal use.
 *
 * The model NEVER sees the Java object T.
 * MATSim NEVER reads responseJson (except maybe for logging).
 */
public interface IToolResponse<T> {

    /**
     * The unique tool_call_id from the LLM request.
     * Required so the framework can match this result
     * to the correct tool call when sending the response
     * back to the model.
     */
    String getToolCallId();

    /**
     * The tool name (must match the registered tool name).
     * Used in serialization when building the tool message.
     */
    String getName();

    /**
     * JSON string to be sent back to the LLM.
     *
     * This becomes:
     * {
     *   "role": "tool",
     *   "tool_call_id": "...",
     *   "name": "...",
     *   "content": responseJson
     * }
     *
     * The LLM only ever sees this string.
     * If null or if isForLLM() == false, nothing is sent.
     */
    String getResponseJson();

    /**
     * The actual Java object produced by the tool.
     *
     * This is consumed by MATSim / your application logic.
     * It is NOT serialized and NOT sent to the LLM.
     *
     * Example:
     * - NetworkRoute
     * - TransitRoute
     * - Plan
     * - Any domain object
     */
    T getToolCallOutputContainer();

    /**
     * Determines whether this tool result should be sent
     * back to the LLM as a tool message.
     *
     * true  -> responseJson will be appended to the conversation
     * false -> result is internal only (e.g., dummy tool)
     *
     * Typical usage:
     * - Non-dummy tools (routing, facility search) -> true
     * - Dummy final tools (plan extraction)        -> false
     */
    boolean isForLLM();
}
