package chatresponse;


import java.util.List;
import java.util.Map;

import tools.IToolCall;

public interface IChatCompletionResponse {

    /**
     * Extracts the assistant's message (content + tool calls).
     */
    IResponseMessage getMessage();

    /**
     * Returns any tool calls the assistant made.
     */
    List<IToolCall> getToolCalls();

    /**
     * Returns token usage information, if available.
     */
    IUsage getUsage();

    /**
     * Returns the model that produced the response.
     */
    String getModel();

    /**
     * Additional metadata (e.g., ID, timestamp).
     */
    Map<String, Object> getMetadata();
}


