package com.ouroboros.service;

import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the complete agent issue processing flow.
 * Tests the end-to-end functionality from issue creation to completion.
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
    private IssueRepository issueRepository;
    
    @BeforeEach
    void setUp() {
        // Clear any existing issues before each test
        issueRepository.deleteAll();
    }
    
    @Test
    void testCompleteIssueProcessingFlow() throws InterruptedException {
        // GIVEN a pending issue is created
        Issue pendingIssue = new Issue("Create a simple REST endpoint", "test-agent");
        Issue savedIssue = issueRepository.save(pendingIssue);
        
        // Verify issue is saved as PENDING
        assertThat(savedIssue.getId()).isNotNull();
        assertThat(savedIssue.getStatus()).isEqualTo(IssueStatus.PENDING);
        
        // Add small delay to ensure timestamp difference
        Thread.sleep(10);
        
        // WHEN the agent processes the issue
        agentService.processIssue(savedIssue);
        
        // THEN the issue should be marked as COMPLETED
        Issue updatedIssue = issueRepository.findById(savedIssue.getId()).orElseThrow();
        assertThat(updatedIssue.getStatus()).isEqualTo(IssueStatus.COMPLETED);
    }
    
    @Test
    void testIssueProcessingWithScheduledPolling() {
        // GIVEN a pending issue is created
        Issue pendingIssue = new Issue("Generate a data validation function", "test-agent");
        Issue savedIssue = issueRepository.save(pendingIssue);
        
        // WHEN we wait for the scheduled polling to pick up the issue
        await().atMost(5, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   Issue currentIssue = issueRepository.findById(savedIssue.getId()).orElseThrow();
                   assertThat(currentIssue.getStatus()).isIn(IssueStatus.IN_PROGRESS, IssueStatus.COMPLETED);
               });
        
        // THEN eventually the issue should be completed
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .untilAsserted(() -> {
                   Issue finalIssue = issueRepository.findById(savedIssue.getId()).orElseThrow();
                   assertThat(finalIssue.getStatus()).isEqualTo(IssueStatus.COMPLETED);
               });
    }
    
    @Test
    void testFetchNextIssueFunctionality() {
        // GIVEN multiple issues with different statuses
        Issue issue1 = issueRepository.save(new Issue("First issue", "test-agent"));
        Issue issue2 = new Issue("Second issue", "test-agent");
        issue2.setStatus(IssueStatus.IN_PROGRESS);
        issueRepository.save(issue2);
        Issue issue3 = issueRepository.save(new Issue("Third issue", "test-agent"));
        
        // WHEN fetching the next issue
        var pendingIssues = issueRepository.findByStatus(IssueStatus.PENDING);
        
        // THEN it should return the pending issues
        assertThat(pendingIssues).hasSize(2);
        assertThat(pendingIssues).extracting(Issue::getId).containsExactlyInAnyOrder(issue1.getId(), issue3.getId());
    }
    
    @Test
    void testIssueStatusTransitions() {
        // GIVEN a pending issue
        Issue originalIssue = issueRepository.save(new Issue("Test status transitions", "test-agent"));
        
        // WHEN marking it as in progress
        originalIssue.setStatus(IssueStatus.IN_PROGRESS);
        Issue savedInProgress = issueRepository.save(originalIssue);
        
        // THEN it should be updated
        assertThat(savedInProgress.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(savedInProgress.getUpdatedAt()).isAfterOrEqualTo(originalIssue.getCreatedAt());
        
        // WHEN marking it as completed
        savedInProgress.setStatus(IssueStatus.COMPLETED);
        Issue savedCompleted = issueRepository.save(savedInProgress);
        
        // THEN it should be completed
        assertThat(savedCompleted.getStatus()).isEqualTo(IssueStatus.COMPLETED);
        assertThat(savedCompleted.getUpdatedAt()).isAfterOrEqualTo(savedInProgress.getCreatedAt());
    }
}