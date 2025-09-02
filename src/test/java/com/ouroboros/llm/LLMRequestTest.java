package com.ouroboros.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LLMRequestTest {

    @Test
    void of_shouldCreateRequestWithDefaults() {
        // WHEN creating a basic request
        LLMRequest request = LLMRequest.of("test prompt", "gpt-4");
        
        // THEN it should have correct values
        assertThat(request.prompt()).isEqualTo("test prompt");
        assertThat(request.modelId()).isEqualTo("gpt-4");
        assertThat(request.temperature()).isEqualTo(0.7);
        assertThat(request.maxTokens()).isEqualTo(1000);
    }
    
    @Test
    void withTemperature_shouldCreateRequestWithCustomTemperature() {
        // WHEN creating a request with custom temperature
        LLMRequest request = LLMRequest.withTemperature("test prompt", "gpt-4", 0.9);
        
        // THEN it should have the custom temperature
        assertThat(request.prompt()).isEqualTo("test prompt");
        assertThat(request.modelId()).isEqualTo("gpt-4");
        assertThat(request.temperature()).isEqualTo(0.9);
        assertThat(request.maxTokens()).isEqualTo(1000);
    }
    
    @Test
    void record_shouldSupportEquality() {
        // GIVEN two identical requests
        LLMRequest request1 = new LLMRequest("prompt", "model", 0.5, 500);
        LLMRequest request2 = new LLMRequest("prompt", "model", 0.5, 500);
        
        // THEN they should be equal
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }
}