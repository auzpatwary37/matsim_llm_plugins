package chatresponse;

import java.util.List;

import chatcommons.IChatMessage;
import tools.IToolCall;

/**
 * A message received from the model, possibly including tool calls.
 */
public interface IResponseMessage extends IChatMessage {
    List<IToolCall> getToolCalls(); // null or empty if not a tool call
}

