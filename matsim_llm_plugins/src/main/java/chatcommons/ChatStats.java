package chatcommons;

public class ChatStats {
    public int llmRounds;
    public int totalToolCalls;

    public int toolParsingFailures;
    public int toolVerificationFailures;
    public int toolExecutionFailures;

    public int noToolCallRetries;
    public boolean hitMaxIterations;

    public long durationMs;

    public boolean success;
    public String failureType;
}