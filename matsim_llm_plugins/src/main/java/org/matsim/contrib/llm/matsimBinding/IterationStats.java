package org.matsim.contrib.llm.matsimBinding;

public class IterationStats {
    public long totalAgents;
    public long success;
    public long failed;

    public long totalToolCalls;
    public long parsingFail;
    public long verificationFail;
    public long executionFail;

    public long totalDurationMs;

    public long promptTokens;
    public long completionTokens;
    public long reasoningTokens;
    public long totalTokens;

    public double avgDurationMs;
    public double avgTokensPerReplan;
    public double tokensPerSec;
}