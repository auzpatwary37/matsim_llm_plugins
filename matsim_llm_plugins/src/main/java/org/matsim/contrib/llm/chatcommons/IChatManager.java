package org.matsim.contrib.llm.chatcommons;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import org.matsim.contrib.llm.chatrequest.IRequestMessage;
import org.matsim.contrib.llm.chatresponse.ChatResult;
import org.matsim.contrib.llm.chatresponse.IChatCompletionResponse;
import org.matsim.contrib.llm.tools.ExternalValidator;

public interface IChatManager {
	
	
	/**
	 * Returns the unique identifier for this chat session.
	 * This ID is used to distinguish between different chat manager instances
	 * in the ChatManagerContainer.
	 *
	 * @return unique thread ID
	 */
	Id<IChatManager> getId();
	
	/**
	 * Returns the MATSim agent ID associated with this chat session.
	 * This links the chat history to a specific person in the population.
	 *
	 * @return person ID, or null if not associated
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
    ChatResult submit(IRequestMessage userMessage,Map<String,ExternalValidator<?>> externalToolResultValidator);

    /**
     * Internal step that sends a message to the LLM and returns its assistant reply.
     * Used during reasoning loop after injecting tool responses.
     *
     * @param message The request message (typically role=tool)
     * @return The assistant’s next message
     */
    IChatCompletionResponse submitInternal(IRequestMessage message);
    
    /**
     * Internal step that sends a message to the LLM and returns its assistant reply.
     * Used during reasoning loop after injecting tool responses.
     *
     * @param message The request message (typically role=tool)
     * @return The assistant’s next message
     */
    IChatCompletionResponse submitInternal(List<IRequestMessage> messages);

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
    
    /**
	 * Returns arbitrary context data stored with this chat session.
	 * Used to pass data between tool executions.
	 *
	 * @return context map
	 */
	Map<String, Object> getContextObject();

	/**
	 * Stores arbitrary context data with this chat session.
	 * The context is passed to tools during execution.
	 *
	 * @param context key-value pairs to store
	 */
	void setContextObject(Map<String,Object> context);

}
