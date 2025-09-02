package com.ouroboros.llm.client;

import com.ouroboros.llm.LLMClient;
import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import com.ouroboros.llm.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI LLM client implementation.
 * Handles interactions with OpenAI's GPT models.
 */
@Service
public class OpenAIClient implements LLMClient {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIClient.class);
    
    @Value("${llm.openai.api-key:}")
    private String apiKey;
    
    @Value("${llm.openai.model-id:gpt-4}")
    private String modelId;
    
    @Override
    public LLMResponse generate(LLMRequest request) {
        log.info("Generating response using OpenAI model: {}", request.modelId());
        
        if (!isAvailable()) {
            return LLMResponse.error("OpenAI client not configured - missing API key");
        }
        
        // TODO: Replace with actual OpenAI API call
        // For now, simulate a response
        try {
            Thread.sleep(500); // Simulate API call delay
            String response = "Generated response for: " + request.prompt() + " (via OpenAI " + request.modelId() + ")";
            TokenUsage usage = TokenUsage.of(50, 100);
            return LLMResponse.success(response, usage, "stop");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LLMResponse.error("Request interrupted");
        }
    }
    
    @Override
    public String getSupportedModelId() {
        return modelId;
    }
    
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}