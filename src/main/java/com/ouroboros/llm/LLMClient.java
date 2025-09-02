package com.ouroboros.llm;

/**
 * Unified interface for LLM client implementations.
 * This interface provides a standard way to interact with different LLM providers.
 */
public interface LLMClient {
    
    /**
     * Generates a response from the LLM based on the provided request.
     * 
     * @param request the LLM request containing prompt and parameters
     * @return the LLM response containing generated content or error information
     */
    LLMResponse generate(LLMRequest request);
    
    /**
     * Returns the model ID that this client supports.
     * 
     * @return the model identifier string
     */
    String getSupportedModelId();
    
    /**
     * Returns true if this client is properly configured and ready to use.
     * 
     * @return true if the client is available, false otherwise
     */
    boolean isAvailable();
}