package com.ouroboros.service;

import com.ouroboros.model.LLMRequest;
import com.ouroboros.model.LLMResponse;

public interface LLMService {
    /**
     * Generates a text response from an LLM based on a given request.
     *
     * @param request The request containing the prompt and model parameters.
     * @return The response from the LLM.
     */
    LLMResponse generate(LLMRequest request);

    /**
     * Identifies which LLM provider this service implementation supports.
     *
     * @return The name of the provider (e.g., "mock", "openai", "gemini").
     */
    String getProvider();
}