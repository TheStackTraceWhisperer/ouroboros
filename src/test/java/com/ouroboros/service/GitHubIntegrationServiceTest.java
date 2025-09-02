package com.ouroboros.service;

import com.ouroboros.github.MockGitHubApiClient;
import com.ouroboros.model.GoalProposal;
import com.ouroboros.model.GoalProposalStatus;
import com.ouroboros.repository.GoalProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GitHubIntegrationService.
 * Tests the synchronization logic with a mock GitHub API client.
 */
@DataJpaTest
@Import({GitHubIntegrationService.class, GitHubProjectsService.class, MockGitHubApiClient.class})
@TestPropertySource(properties = {
    "github.integration.enabled=true",
    "github.integration.sync.interval=1000"
})
class GitHubIntegrationServiceTest {
    
    @Autowired
    private GitHubIntegrationService gitHubIntegrationService;
    
    @Autowired
    private GoalProposalRepository proposalRepository;
    
    @Autowired
    private MockGitHubApiClient mockGitHubApiClient;
    
    @BeforeEach
    void setUp() {
        mockGitHubApiClient.reset();
    }
    
    @Test
    void shouldCreateGitHubIssueForNewProposal() {
        // Given a new proposal
        GoalProposal proposal = new GoalProposal("Test proposal description", "test-agent");
        proposalRepository.save(proposal);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then a GitHub issue should be created
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNotNull();
        
        MockGitHubApiClient.MockIssue issue = mockGitHubApiClient.getIssue(updated.getGithubIssueId());
        assertThat(issue).isNotNull();
        assertThat(issue.title).contains("Agent Task");
        assertThat(issue.body).contains("Test proposal description");
        assertThat(issue.body).contains("test-agent");
    }
    
    @Test
    void shouldAddCommentWhenProposalStatusChanges() {
        // Given a proposal with an existing GitHub issue
        GoalProposal proposal = new GoalProposal("Test proposal", "test-agent");
        proposalRepository.save(proposal);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        assertThat(issueId).isNotNull();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue issue = mockGitHubApiClient.getIssue(issueId);
        issue.comments.clear();
        
        // When status changes
        updated.setStatus(GoalProposalStatus.IN_PROGRESS);
        proposalRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then a comment should be added
        assertThat(issue.comments).hasSize(1);
        assertThat(issue.comments.get(0)).contains("Status Update").contains("IN_PROGRESS");
    }
    
    @Test
    void shouldCloseIssueWhenProposalCompleted() {
        // Given a proposal with an existing GitHub issue
        GoalProposal proposal = new GoalProposal("Test proposal", "test-agent");
        proposalRepository.save(proposal);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue issue = mockGitHubApiClient.getIssue(issueId);
        issue.comments.clear();
        
        // When proposal is completed
        updated.setStatus(GoalProposalStatus.COMPLETED);
        proposalRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the issue should be closed with appropriate labels and comments
        assertThat(issue.closed).isTrue();
        assertThat(issue.labels).contains("status:completed");
        assertThat(issue.comments).hasSize(1);
        assertThat(issue.comments.get(0)).contains("Task completed");
    }
    
    @Test
    void shouldCloseIssueWhenProposalFailed() {
        // Given a proposal with an existing GitHub issue
        GoalProposal proposal = new GoalProposal("Test proposal", "test-agent");
        proposalRepository.save(proposal);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue issue = mockGitHubApiClient.getIssue(issueId);
        issue.comments.clear();
        
        // When proposal fails
        updated.setStatus(GoalProposalStatus.FAILED);
        proposalRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the issue should be closed with appropriate labels and comments
        assertThat(issue.closed).isTrue();
        assertThat(issue.labels).contains("status:failed");
        assertThat(issue.comments).hasSize(1);
        assertThat(issue.comments.get(0)).contains("Task failed");
    }
    
    @Test
    void shouldSkipSyncWhenGitHubApiNotAvailable() {
        // Given GitHub API is not available
        mockGitHubApiClient.setAvailable(false);
        
        // And a new proposal
        GoalProposal proposal = new GoalProposal("Test proposal", "test-agent");
        proposalRepository.save(proposal);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then no GitHub issue should be created
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNull();
    }
    
    @Test
    void shouldHandleGitHubApiFailuresGracefully() {
        // Given GitHub API operations will fail
        mockGitHubApiClient.setShouldFailOperations(true);
        
        // And a new proposal
        GoalProposal proposal = new GoalProposal("Test proposal", "test-agent");
        proposalRepository.save(proposal);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the proposal should remain unchanged (no GitHub issue ID)
        GoalProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNull();
    }
}