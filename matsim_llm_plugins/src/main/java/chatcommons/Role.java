package chatcommons;

/**
 * Enum representing the allowed roles in a chat message.
 */
public enum Role {
    USER("user"),
    SYSTEM("system"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    TOOL_RESPONSE("tool_response");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    /**
     * Returns the string value used in the API.
     */
    public String getValue() {
        return value;
    }

    /**
     * Parses a string role name into a Role enum, or throws if invalid.
     */
    public static Role fromValue(String value) {
        for (Role r : values()) {
            if (r.value.equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
