package com.ouroboros.service;

import com.ouroboros.llm.LLMClient;
import com.ouroboros.llm.LLMClientFactory;
import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import com.ouroboros.model.Goal;
import com.ouroboros.repository.GoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Self-enhancing agent service that polls for goals and processes them.
 * This is the main kernel of the agent system.
 */
@Service
@EnableAsync
public class AgentService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    
    private final GoalRepository goalRepository;
    private final LLMClientFactory llmClientFactory;
    private final SelfPublishService selfPublishService;
    
    @Value("${agent.poll.interval:10000}")
    private long pollIntervalMs;
    
    @Value("${agent.max.retries:3}")
    private int maxRetries;
    
    @Autowired
    public AgentService(
            GoalRepository goalRepository, 
            LLMClientFactory llmClientFactory,
            SelfPublishService selfPublishService) {
        this.goalRepository = goalRepository;
        this.llmClientFactory = llmClientFactory;
        this.selfPublishService = selfPublishService;
    }
    
    /**
     * Main agent loop that polls for pending goals and processes them.
     * This method is scheduled to run periodically.
     */
    @Scheduled(fixedDelayString = "${agent.poll.interval:10000}")
    @Async
    public void pollAndProcessGoals() {
        log.debug("Polling for pending goals...");
        
        Optional<Goal> pendingGoal = goalRepository.fetchNextGoal();
        if (pendingGoal.isPresent()) {
            processGoal(pendingGoal.get());
        } else {
            log.debug("No pending goals found");
        }
    }
    
    /**
     * Processes a single goal through the complete lifecycle.
     * 
     * @param goal the goal to process
     */
    public void processGoal(Goal goal) {
        log.info("Starting to process goal {}: {}", goal.id(), goal.description());
        
        try {
            // 1. Mark goal as IN_PROGRESS
            Goal inProgressGoal = goal.markInProgress();
            goalRepository.updateGoalStatus(inProgressGoal);
            
            // 2. Generate code using LLM
            String generatedCode = generateCode(goal.description());
            
            // 3. Perform self-publish action
            boolean publishSuccess = selfPublishService.publish(generatedCode);
            
            // 4. Update goal status based on result
            if (publishSuccess) {
                Goal completedGoal = inProgressGoal.markCompleted("Code generated and published successfully");
                goalRepository.updateGoalStatus(completedGoal);
                log.info("Successfully completed goal {}", goal.id());
            } else {
                Goal failedGoal = inProgressGoal.markFailed("Self-publish action failed");
                goalRepository.updateGoalStatus(failedGoal);
                log.error("Failed to complete goal {}: self-publish failed", goal.id());
            }
            
        } catch (Exception e) {
            log.error("Error processing goal {}: {}", goal.id(), e.getMessage(), e);
            Goal failedGoal = goal.markFailed("Unexpected error: " + e.getMessage());
            goalRepository.updateGoalStatus(failedGoal);
        }
    }
    
    /**
     * Generates code for the given goal description using the LLM.
     * 
     * @param goalDescription the description of what to generate
     * @return the generated code
     * @throws RuntimeException if code generation fails
     */
    private String generateCode(String goalDescription) {
        log.info("Generating code for goal: {}", goalDescription);
        
        LLMClient defaultClient = llmClientFactory.getDefaultClient();
        String prompt = "Generate code for the following goal: " + goalDescription;
        LLMRequest request = LLMRequest.of(prompt, defaultClient.getSupportedModelId());
        
        LLMResponse response = defaultClient.generate(request);
        
        if (response.isSuccess()) {
            log.info("Code generation successful. Tokens used: {}", response.tokenUsage().totalTokens());
            return response.content();
        } else {
            throw new RuntimeException("Code generation failed: " + response.error());
        }
    }
    
    /**
     * Manually trigger goal processing (useful for testing).
     */
    public void triggerGoalProcessing() {
        log.info("Manually triggering goal processing");
        pollAndProcessGoals();
    }
}