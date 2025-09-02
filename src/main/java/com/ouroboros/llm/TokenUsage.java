package com.ouroboros.llm;

/**
 * Token usage information for LLM responses.
 */
public record TokenUsage(
    int promptTokens,
    int completionTokens,
    int totalTokens
) {
    
    public static TokenUsage of(int promptTokens, int completionTokens) {
        return new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
    }
}