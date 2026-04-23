package org.matsim.contrib.llm.chatrequest;

import java.util.List;

import org.matsim.contrib.llm.chatcommons.IChatMessage;
import org.matsim.contrib.llm.tools.IToolResponse;

/**
 * A message being sent to the model.
 */
public interface IRequestMessage extends IChatMessage {
    // This will contain any tool response 
	public List<IToolResponse<?>> getToolResponses();

}

