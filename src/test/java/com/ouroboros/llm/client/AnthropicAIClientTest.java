package com.ouroboros.llm.client;

import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicAIClientTest {

    private AnthropicAIClient client;

    @BeforeEach
    void setUp() {
        client = new AnthropicAIClient();
        // Set test values using reflection to avoid Spring context
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "modelId", "claude-3-haiku");
    }

    @Test
    void generate_shouldReturnSuccessfulResponse() {
        // GIVEN a valid request
        LLMRequest request = LLMRequest.of("test prompt", "claude-3-haiku");
        
        // WHEN generating a response
        LLMResponse response = client.generate(request);
        
        // THEN it should be successful
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isError()).isFalse();
        assertThat(response.content()).contains("Generated response for: test prompt");
        assertThat(response.content()).contains("via Anthropic claude-3-haiku");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().totalTokens()).isEqualTo(165);
        assertThat(response.finishReason()).isEqualTo("stop");
    }
    
    @Test
    void generate_shouldReturnErrorWhenNotAvailable() {
        // GIVEN a client without API key
        ReflectionTestUtils.setField(client, "apiKey", "");
        LLMRequest request = LLMRequest.of("test prompt", "claude-3-haiku");
        
        // WHEN generating a response
        LLMResponse response = client.generate(request);
        
        // THEN it should return an error
        assertThat(response.isError()).isTrue();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.error()).contains("Anthropic AI client not configured");
    }
    
    @Test
    void getSupportedModelId_shouldReturnConfiguredModel() {
        // WHEN getting the supported model ID
        String modelId = client.getSupportedModelId();
        
        // THEN it should return the configured model
        assertThat(modelId).isEqualTo("claude-3-haiku");
    }
    
    @Test
    void isAvailable_shouldReturnTrueWhenApiKeyIsSet() {
        // WHEN API key is set
        // THEN client should be available
        assertThat(client.isAvailable()).isTrue();
    }
    
    @Test
    void isAvailable_shouldReturnFalseWhenApiKeyIsEmpty() {
        // GIVEN empty API key
        ReflectionTestUtils.setField(client, "apiKey", "");
        
        // WHEN checking availability
        // THEN client should not be available
        assertThat(client.isAvailable()).isFalse();
    }
    
    @Test
    void isAvailable_shouldReturnFalseWhenApiKeyIsNull() {
        // GIVEN null API key
        ReflectionTestUtils.setField(client, "apiKey", null);
        
        // WHEN checking availability
        // THEN client should not be available
        assertThat(client.isAvailable()).isFalse();
    }
}