package com.ouroboros.llm;

import com.ouroboros.llm.client.AnthropicAIClient;
import com.ouroboros.llm.client.GoogleAIClient;
import com.ouroboros.llm.client.OpenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating LLM client instances based on model IDs.
 * Responsible for routing requests to the appropriate LLM provider.
 */
@Service
public class LLMClientFactory {
    
    private static final Logger log = LoggerFactory.getLogger(LLMClientFactory.class);
    
    private final Map<String, LLMClient> clientMap;
    
    @Value("${llm.default.model-id:gpt-4}")
    private String defaultModelId;
    
    @Autowired
    public LLMClientFactory(
            OpenAIClient openAIClient,
            GoogleAIClient googleAIClient,
            AnthropicAIClient anthropicAIClient) {
        
        List<LLMClient> clients = List.of(openAIClient, googleAIClient, anthropicAIClient);
        
        // Create mapping from model IDs to clients
        this.clientMap = clients.stream()
                .collect(Collectors.toMap(
                        LLMClient::getSupportedModelId,
                        Function.identity()
                ));
        
        log.info("Initialized LLM Client Factory with {} clients: {}", 
                clientMap.size(), clientMap.keySet());
    }
    
    /**
     * Gets an LLM client for the specified model ID.
     * 
     * @param modelId the model identifier
     * @return the appropriate LLM client
     * @throws IllegalArgumentException if no client supports the model ID
     */
    public LLMClient getClient(String modelId) {
        LLMClient client = clientMap.get(modelId);
        if (client == null) {
            throw new IllegalArgumentException("No client available for model ID: " + modelId + 
                    ". Available models: " + clientMap.keySet());
        }
        
        if (!client.isAvailable()) {
            log.warn("Client for model {} is not available (likely missing configuration)", modelId);
        }
        
        return client;
    }
    
    /**
     * Gets the default LLM client.
     * 
     * @return the default LLM client
     */
    public LLMClient getDefaultClient() {
        return getClient(defaultModelId);
    }
    
    /**
     * Returns all available model IDs.
     * 
     * @return set of supported model IDs
     */
    public java.util.Set<String> getAvailableModelIds() {
        return clientMap.keySet();
    }
}