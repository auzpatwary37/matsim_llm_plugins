package chatcommons;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import chatrequest.IRequestMessage;
import chatresponse.IChatCompletionResponse;

public interface IChatManager {
	
	
	/**
	 * 
	 * @return the id of the chatmanager thread.
	 */
	Id<IChatManager> getId();
	
	/**
	 * 
	 * @return the personId associated to this manager if there is anyS
	 */
	Id<Person> getPersonId();
	
	/**
	 * 
	 * @param person associate the thread with a person. 
	 */
	void setPersonId(Id<Person> person);

    /**
     * External entry point from MATSim.
     * Submits a user message to the LLM, executes all tool calls,
     * and returns all tool responses (including dummy tools).
     *
     * @param userMessage The initial user message
     * @return A map of toolCallId to IToolResponse for all executed tools
     */
    ChatResult submit(IRequestMessage userMessage);

    /**
     * Internal step that sends a message to the LLM and returns its assistant reply.
     * Used during reasoning loop after injecting tool responses.
     *
     * @param message The request message (typically role=tool)
     * @return The assistant’s next message
     */
    IChatCompletionResponse submitInternal(IRequestMessage message);

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
     * Clears both the history .
     */
    void clear();
    
    Map<String, Object> getContextObject();
    void setContextObject(Map<String,Object> context);

}
