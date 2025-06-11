package tools;

/**
 * Default implementation of IToolResponse.
 * Wraps the output of a single tool call, including:
 * - The ID of the original tool_call (from LLM)
 * - The tool's name (for serialization)
 * - The JSON string representing the tool's output
 */
public class DefaultToolResponse<T> implements IToolResponse<T> {

    private final String toolCallId;
    private final String name;
    private final String responseJson;
    private final T toolOutput;
    private boolean isForLLM;

    public DefaultToolResponse(String toolCallId, String name, String responseJson, T toolOutput, boolean isDummyResponse) {
        this.toolCallId = toolCallId;
        this.name = name;
        this.responseJson = responseJson;
        this.toolOutput = toolOutput;
        this.isForLLM = !isDummyResponse;
    }

    @Override
    public String getToolCallId() {
        return toolCallId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getResponseJson() {
        return responseJson;
    }

    @Override
    public String toString() {
        return "ToolResponse[" +
                "toolCallId='" + toolCallId + '\'' +
                ", name='" + name + '\'' +
                ", responseJson=" + responseJson +
                ']';
    }

	@Override
	public T getToolCallOutputContainer() {
		
		return this.toolOutput;
	}

	@Override
	public boolean isForLLM() {
		
		return this.isForLLM;
	}
}

