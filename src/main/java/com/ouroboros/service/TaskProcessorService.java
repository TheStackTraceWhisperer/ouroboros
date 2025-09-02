package com.ouroboros.service;

import com.ouroboros.llm.LLMClient;
import com.ouroboros.llm.LLMClientFactory;
import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import com.ouroboros.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TaskProcessorService {

    private static final Logger log = LoggerFactory.getLogger(TaskProcessorService.class);
    
    private final LLMClientFactory llmClientFactory;
    
    @Value("${llm.default.model-id:gpt-4}")
    private String defaultModelId;

    @Autowired
    public TaskProcessorService(LLMClientFactory llmClientFactory) {
        this.llmClientFactory = llmClientFactory;
    }

    public void processTask(Task task) {
        log.info("Starting processing for task ID: {}", task.id());
        try {
            // First, simulate the original work based on task complexity
            TimeUnit.MILLISECONDS.sleep(task.complexity() * 100L);
            
            // Now use LLM to process the task description
            processTaskWithLLM(task);
            
            log.info("Finished processing for task ID: {}", task.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing was interrupted for task ID: {}", task.id(), e);
        }
    }
    
    private void processTaskWithLLM(Task task) {
        try {
            LLMClient client = llmClientFactory.getDefaultClient();
            
            if (!client.isAvailable()) {
                log.warn("LLM client not available for task {}, skipping LLM processing", task.id());
                return;
            }
            
            String prompt = "Analyze this task and provide insights: " + task.description();
            LLMRequest request = LLMRequest.of(prompt, defaultModelId);
            
            log.info("Sending task {} to LLM for analysis", task.id());
            LLMResponse response = client.generate(request);
            
            if (response.isSuccess()) {
                log.info("LLM analysis for task {}: {} (tokens: {})", 
                        task.id(), response.content(), response.tokenUsage().totalTokens());
            } else {
                log.error("LLM analysis failed for task {}: {}", task.id(), response.error());
            }
            
        } catch (Exception e) {
            log.error("Error during LLM processing for task {}: {}", task.id(), e.getMessage(), e);
        }
    }
}