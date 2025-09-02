package com.ouroboros.llm;

import com.ouroboros.llm.client.AnthropicAIClient;
import com.ouroboros.llm.client.GoogleAIClient;
import com.ouroboros.llm.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LLMClientFactoryTest {

    @Mock
    private OpenAIClient openAIClient;
    
    @Mock
    private GoogleAIClient googleAIClient;
    
    @Mock
    private AnthropicAIClient anthropicAIClient;
    
    private LLMClientFactory factory;

    @BeforeEach
    void setUp() {
        when(openAIClient.getSupportedModelId()).thenReturn("gpt-4");
        when(googleAIClient.getSupportedModelId()).thenReturn("gemini-pro");
        when(anthropicAIClient.getSupportedModelId()).thenReturn("claude-3-haiku");
        
        factory = new LLMClientFactory(openAIClient, googleAIClient, anthropicAIClient);
        // Set the default model ID via reflection
        ReflectionTestUtils.setField(factory, "defaultModelId", "gpt-4");
    }

    @Test
    void getClient_shouldReturnCorrectClientForModelId() {
        // WHEN getting clients for different models
        LLMClient gptClient = factory.getClient("gpt-4");
        LLMClient geminiClient = factory.getClient("gemini-pro");
        LLMClient claudeClient = factory.getClient("claude-3-haiku");
        
        // THEN the correct clients should be returned
        assertThat(gptClient).isSameAs(openAIClient);
        assertThat(geminiClient).isSameAs(googleAIClient);
        assertThat(claudeClient).isSameAs(anthropicAIClient);
    }
    
    @Test
    void getClient_shouldThrowExceptionForUnsupportedModel() {
        // WHEN trying to get a client for unsupported model
        // THEN it should throw an exception
        assertThatThrownBy(() -> factory.getClient("unsupported-model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No client available for model ID: unsupported-model")
                .hasMessageContaining("Available models:");
    }
    
    @Test
    void getAvailableModelIds_shouldReturnAllSupportedModels() {
        // WHEN getting available model IDs
        var modelIds = factory.getAvailableModelIds();
        
        // THEN all models should be included
        assertThat(modelIds).containsExactlyInAnyOrder("gpt-4", "gemini-pro", "claude-3-haiku");
    }
    
    @Test
    void getDefaultClient_shouldReturnGpt4Client() {
        // WHEN getting the default client
        LLMClient defaultClient = factory.getDefaultClient();
        
        // THEN it should return the GPT-4 client (as configured in test)
        assertThat(defaultClient).isSameAs(openAIClient);
    }
}