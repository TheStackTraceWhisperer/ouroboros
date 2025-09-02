package com.ouroboros.llm;

/**
 * Standardized request structure for LLM interactions.
 * Contains all necessary parameters for making LLM API calls.
 */
public record LLMRequest(
    String prompt,
    String modelId,
    Double temperature,
    Integer maxTokens
) {
    
    /**
     * Creates a basic LLM request with default parameters.
     */
    public static LLMRequest of(String prompt, String modelId) {
        return new LLMRequest(prompt, modelId, 0.7, 1000);
    }
    
    /**
     * Creates an LLM request with custom temperature.
     */
    public static LLMRequest withTemperature(String prompt, String modelId, double temperature) {
        return new LLMRequest(prompt, modelId, temperature, 1000);
    }
}