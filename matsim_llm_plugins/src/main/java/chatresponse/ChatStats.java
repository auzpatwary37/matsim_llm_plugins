package chatresponse;

/**
 * Statistics tracking for a single chat interaction.
 * Tracks LLM rounds, tool calls, failures, retries, duration, and token usage.
 */
public class ChatStats {
    public int llmRounds;
    public int totalToolCalls;

    public int toolParsingFailures;
    public int toolVerificationFailures;
    public int toolExecutionFailures;
    public int externalValidationFailures;

    public int noToolCallRetries;
    public boolean hitMaxIterations;

    public long durationMs;

    public boolean success;
    public String failureType;

    public int promptTokens;
    public int completionTokens;
    public int reasoningTokens;
    public int totalTokens;
}