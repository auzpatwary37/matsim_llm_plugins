package org.matsim.contrib.llm.chatresponse;

import java.util.List;

import org.matsim.contrib.llm.chatcommons.IChatMessage;
import org.matsim.contrib.llm.tools.IToolCall;

/**
 * A message received from the model, possibly including tool calls.
 */
public interface IResponseMessage extends IChatMessage {
    List<IToolCall> getToolCalls(); // null or empty if not a tool call
}

