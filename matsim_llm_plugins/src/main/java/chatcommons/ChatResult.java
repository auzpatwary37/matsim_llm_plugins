package chatcommons;

import java.util.Map;

import tools.IToolResponse;

public class ChatResult {
    public Map<String, IToolResponse<?>> toolResponses;
    public ChatStats stats;

    public ChatResult(Map<String, IToolResponse<?>> toolResponses, ChatStats stats) {
        this.toolResponses = toolResponses;
        this.stats = stats;
    }
}
