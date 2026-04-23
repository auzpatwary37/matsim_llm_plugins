package org.matsim.contrib.llm.chatresponse;

import java.util.Map;

import org.matsim.contrib.llm.tools.IToolResponse;

/**
 * Container for chat interaction results.
 * Holds tool responses and associated statistics.
 */
public class ChatResult {
    public Map<String, IToolResponse<?>> toolResponses;
    public ChatStats stats;

    public ChatResult(Map<String, IToolResponse<?>> toolResponses, ChatStats stats) {
        this.toolResponses = toolResponses;
        this.stats = stats;
    }
}
