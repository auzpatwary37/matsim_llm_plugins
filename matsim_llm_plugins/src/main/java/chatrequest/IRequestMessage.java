package chatrequest;

import java.util.List;

import chatcommons.IChatMessage;
import tools.IToolResponse;

/**
 * A message being sent to the model.
 */
public interface IRequestMessage extends IChatMessage {
    // This will contain any tool response 
	public List<IToolResponse<?>> getToolResponses();

}

