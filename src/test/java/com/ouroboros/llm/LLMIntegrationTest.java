package com.ouroboros.llm;

import com.ouroboros.llm.client.OpenAIClient;
import com.ouroboros.model.Task;
import com.ouroboros.service.TaskProcessorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test for LLM abstraction layer with Spring Boot context.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "llm.openai.api-key=test-key",
        "llm.openai.model-id=gpt-4",
        "llm.default.model-id=gpt-4"
})
class LLMIntegrationTest {

    @Autowired
    private LLMClientFactory llmClientFactory;
    
    @Autowired
    private TaskProcessorService taskProcessorService;
    
    @Autowired
    private OpenAIClient openAIClient;

    @Test
    void llmClientFactory_shouldBeConfiguredCorrectly() {
        // WHEN getting available model IDs
        var modelIds = llmClientFactory.getAvailableModelIds();
        
        // THEN all three providers should be available
        assertThatCode(() -> {
            assert modelIds.contains("gpt-4");
            assert modelIds.contains("gemini-pro");
            assert modelIds.contains("claude-3-haiku");
        }).doesNotThrowAnyException();
    }
    
    @Test
    void llmClientFactory_shouldReturnWorkingClients() {
        // WHEN getting each type of client
        LLMClient openAI = llmClientFactory.getClient("gpt-4");
        LLMClient gemini = llmClientFactory.getClient("gemini-pro");
        LLMClient claude = llmClientFactory.getClient("claude-3-haiku");
        
        // THEN they should be properly configured
        assertThatCode(() -> {
            assert openAI.isAvailable(); // Should be true due to test property
            assert !gemini.isAvailable(); // Should be false - no API key
            assert !claude.isAvailable(); // Should be false - no API key
        }).doesNotThrowAnyException();
    }
    
    @Test
    void taskProcessorService_shouldProcessTaskWithLLM() {
        // GIVEN a task
        Task task = new Task(UUID.randomUUID(), "Analyze customer feedback", 2);
        
        // WHEN processing the task (which should include LLM processing)
        // THEN it should complete without errors
        assertThatCode(() -> taskProcessorService.processTask(task))
                .doesNotThrowAnyException();
    }
    
    @Test
    void openAIClient_shouldGenerateResponse() {
        // GIVEN a request
        LLMRequest request = LLMRequest.of("What is the capital of France?", "gpt-4");
        
        // WHEN generating a response
        LLMResponse response = openAIClient.generate(request);
        
        // THEN it should be successful (simulated response)
        assertThatCode(() -> {
            assert response.isSuccess();
            assert response.content().contains("Generated response for: What is the capital of France?");
            assert response.tokenUsage().totalTokens() > 0;
        }).doesNotThrowAnyException();
    }
}