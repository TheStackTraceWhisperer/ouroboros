package com.ouroboros.service;

import com.ouroboros.model.Goal;
import com.ouroboros.model.GoalStatus;
import com.ouroboros.repository.GoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the complete agent goal processing flow.
 * Tests the end-to-end functionality from goal creation to completion.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "agent.poll.interval=1000",  // Faster polling for tests
    "llm.openai.api-key=test-key"  // Mock API key for tests
})
class AgentIntegrationTest {
    
    @Autowired
    private AgentService agentService;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @BeforeEach
    void setUp() {
        // Clear any existing goals before each test
        if (goalRepository instanceof com.ouroboros.repository.InMemoryGoalRepository) {
            ((com.ouroboros.repository.InMemoryGoalRepository) goalRepository).clear();
        }
    }
    
    @Test
    void testCompleteGoalProcessingFlow() {
        // GIVEN a pending goal is created
        Goal pendingGoal = Goal.pending("Create a simple REST endpoint");
        Goal savedGoal = goalRepository.save(pendingGoal);
        
        // Verify goal is saved as PENDING
        assertThat(savedGoal.id()).isNotNull();
        assertThat(savedGoal.status()).isEqualTo(GoalStatus.PENDING);
        
        // WHEN the agent processes the goal
        agentService.processGoal(savedGoal);
        
        // THEN the goal should be marked as COMPLETED
        Goal updatedGoal = goalRepository.findById(savedGoal.id()).orElseThrow();
        assertThat(updatedGoal.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(updatedGoal.result()).contains("Code generated and published successfully");
        assertThat(updatedGoal.updatedAt()).isAfter(savedGoal.updatedAt());
    }
    
    @Test
    void testGoalProcessingWithScheduledPolling() {
        // GIVEN a pending goal is created
        Goal pendingGoal = Goal.pending("Generate a data validation function");
        Goal savedGoal = goalRepository.save(pendingGoal);
        
        // WHEN we wait for the scheduled polling to pick up the goal
        await().atMost(5, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   Goal currentGoal = goalRepository.findById(savedGoal.id()).orElseThrow();
                   assertThat(currentGoal.status()).isIn(GoalStatus.IN_PROGRESS, GoalStatus.COMPLETED);
               });
        
        // THEN eventually the goal should be completed
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   Goal finalGoal = goalRepository.findById(savedGoal.id()).orElseThrow();
                   assertThat(finalGoal.status()).isEqualTo(GoalStatus.COMPLETED);
                   assertThat(finalGoal.result()).isNotNull();
               });
    }
    
    @Test
    void testFetchNextGoalFunctionality() {
        // GIVEN multiple goals with different statuses
        Goal goal1 = goalRepository.save(Goal.pending("First goal"));
        Goal goal2 = goalRepository.save(Goal.pending("Second goal").markInProgress());
        Goal goal3 = goalRepository.save(Goal.pending("Third goal"));
        
        // WHEN fetching the next goal
        var nextGoal = goalRepository.fetchNextGoal();
        
        // THEN it should return one of the pending goals
        assertThat(nextGoal).isPresent();
        assertThat(nextGoal.get().status()).isEqualTo(GoalStatus.PENDING);
        assertThat(nextGoal.get().id()).isIn(goal1.id(), goal3.id());
    }
    
    @Test
    void testGoalStatusTransitions() {
        // GIVEN a pending goal
        Goal originalGoal = goalRepository.save(Goal.pending("Test status transitions"));
        
        // WHEN marking it as in progress
        Goal inProgressGoal = originalGoal.markInProgress();
        Goal savedInProgress = goalRepository.updateGoalStatus(inProgressGoal);
        
        // THEN it should be updated
        assertThat(savedInProgress.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(savedInProgress.updatedAt()).isAfter(originalGoal.updatedAt());
        
        // WHEN marking it as completed
        Goal completedGoal = savedInProgress.markCompleted("Task completed successfully");
        Goal savedCompleted = goalRepository.updateGoalStatus(completedGoal);
        
        // THEN it should be completed with result
        assertThat(savedCompleted.status()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(savedCompleted.result()).isEqualTo("Task completed successfully");
        assertThat(savedCompleted.updatedAt()).isAfter(savedInProgress.updatedAt());
    }
}