package com.ouroboros.llm;

/**
 * Standardized response structure for LLM interactions.
 * Contains the generated content and metadata about the response.
 */
public record LLMResponse(
    String content,
    TokenUsage tokenUsage,
    String finishReason,
    String error
) {
    
    /**
     * Creates a successful LLM response.
     */
    public static LLMResponse success(String content, TokenUsage tokenUsage, String finishReason) {
        return new LLMResponse(content, tokenUsage, finishReason, null);
    }
    
    /**
     * Creates an error LLM response.
     */
    public static LLMResponse error(String error) {
        return new LLMResponse(null, null, null, error);
    }
    
    /**
     * Returns true if this response represents an error.
     */
    public boolean isError() {
        return error != null;
    }
    
    /**
     * Returns true if this response is successful.
     */
    public boolean isSuccess() {
        return error == null && content != null;
    }
}