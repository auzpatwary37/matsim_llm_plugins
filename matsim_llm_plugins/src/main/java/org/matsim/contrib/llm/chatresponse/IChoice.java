package org.matsim.contrib.llm.chatresponse;

/**
 * Represents a single choice in the LLM's response.
 * Contains the assistant's message (content and optional tool calls).
 */
public interface IChoice {
    IResponseMessage getMessage();
}

