package chatcommons;


/**
 * Represents a generic message in a chat exchange, regardless of direction.
 * Both request-side and response-side messages extend this.
 */
public interface IChatMessage {
    Role getRole();       // "user", "assistant", "tool", or "system"
    String getContent();    // Main message content or tool result
}

