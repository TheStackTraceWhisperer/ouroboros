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
 * Anthropic AI (Claude) LLM client implementation.
 * Handles interactions with Anthropic's Claude models.
 */
@Service
public class AnthropicAIClient implements LLMClient {
    
    private static final Logger log = LoggerFactory.getLogger(AnthropicAIClient.class);
    
    @Value("${llm.anthropic.api-key:}")
    private String apiKey;
    
    @Value("${llm.anthropic.model-id:claude-3-haiku}")
    private String modelId;
    
    @Override
    public LLMResponse generate(LLMRequest request) {
        log.info("Generating response using Anthropic AI model: {}", request.modelId());
        
        if (!isAvailable()) {
            return LLMResponse.error("Anthropic AI client not configured - missing API key");
        }
        
        // TODO: Replace with actual Anthropic API call
        // For now, simulate a response
        try {
            Thread.sleep(400); // Simulate API call delay
            String response = "Generated response for: " + request.prompt() + " (via Anthropic " + request.modelId() + ")";
            TokenUsage usage = TokenUsage.of(55, 110);
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