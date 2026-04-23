package org.matsim.contrib.llm.chatresponse;

/**
 * Token usage statistics from the LLM response.
 */
public interface IUsage {
    /** Tokens used in the prompt. */
    int getPromptTokens();

    /** Tokens generated in the response. */
    int getCompletionTokens();

    /** Total tokens (prompt + completion). */
    int getTotalTokens();

    /** Reasoning tokens (if supported by model). */
    int getReasoningTokens();
}
