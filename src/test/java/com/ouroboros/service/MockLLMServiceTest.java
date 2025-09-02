package com.ouroboros.service;

import com.ouroboros.model.LLMRequest;
import com.ouroboros.model.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class MockLLMServiceTest {

    private final MockLLMService mockLlmService = new MockLLMService();

    @Test
    void generate_shouldReturnMockedText() {
        // GIVEN a request
        LLMRequest request = new LLMRequest("mock-model", "What is the capital of Arkansas?", Collections.emptyMap());

        // WHEN generating a response
        LLMResponse response = mockLlmService.generate(request);

        // THEN the response should contain the mocked text and the original prompt
        assertThat(response.modelId()).isEqualTo("mock-model");
        assertThat(response.generatedText()).contains("This is a mock response");
        assertThat(response.generatedText()).contains("What is the capital of Arkansas?");
    }

    @Test
    void getProvider_shouldReturnMock() {
        // WHEN getting the provider
        String provider = mockLlmService.getProvider();

        // THEN the provider should be "mock"
        assertThat(provider).isEqualTo("mock");
    }
}