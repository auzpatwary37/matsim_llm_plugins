package chatcommons;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import chatrequest.IRequestMessage;
import chatresponse.IResponseMessage;
import tools.IToolResponse;

public interface IChatManager {
	
	
	Id<IChatManager> getId();

    /**
     * External entry point from MATSim.
     * Submits a user message to the LLM, executes all tool calls,
     * and returns all tool responses (including dummy tools).
     *
     * @param userMessage The initial user message
     * @return A map of toolCallId to IToolResponse for all executed tools
     */
    Map<String, IToolResponse<?>> submit(IRequestMessage userMessage);

    /**
     * Internal step that sends a message to the LLM and returns its assistant reply.
     * Used during reasoning loop after injecting tool responses.
     *
     * @param message The request message (typically role=tool)
     * @return The assistant’s next message
     */
    IResponseMessage submitInternal(IRequestMessage message);

    /**
     * Appends a message to the chat history without invoking the LLM.
     * Used for injecting tool responses or error/debug messages.
     */
    void append(IChatMessage... message);

    /**
     * Returns the current system prompt, if set.
     */
    String getSystemMessage();

    /**
     * Sets the system prompt that will be prepended to the history.
     */
    void setSystemMessage(String systemMessage);

    /**
     * Returns all user/assistant/tool messages in order (excludes system).
     */
    List<IChatMessage> getHistory();

    /**
     * Returns the most recent message in the history, or null if empty.
     */
    IChatMessage getLastMessage();

    /**
     * Clears both the history and the system message.
     */
    void clear();
}
