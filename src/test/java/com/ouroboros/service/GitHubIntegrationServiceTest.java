package com.ouroboros.service;

import com.ouroboros.github.MockGitHubApiClient;
import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
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
    private IssueRepository issueRepository;
    
    @Autowired
    private MockGitHubApiClient mockGitHubApiClient;
    
    @BeforeEach
    void setUp() {
        mockGitHubApiClient.reset();
    }
    
    @Test
    void shouldCreateGitHubIssueForNewIssue() {
        // Given a new issue
        Issue issue = new Issue("Test issue description", "test-agent");
        issueRepository.save(issue);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then a GitHub issue should be created
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNotNull();
        
        MockGitHubApiClient.MockIssue githubIssue = mockGitHubApiClient.getIssue(updated.getGithubIssueId());
        assertThat(githubIssue).isNotNull();
        assertThat(githubIssue.title).contains("Agent Task");
        assertThat(githubIssue.body).contains("Test issue description");
        assertThat(githubIssue.body).contains("test-agent");
    }
    
    @Test
    void shouldAddCommentWhenIssueStatusChanges() {
        // Given an issue with an existing GitHub issue
        Issue issue = new Issue("Test issue", "test-agent");
        issueRepository.save(issue);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        assertThat(issueId).isNotNull();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue githubIssue = mockGitHubApiClient.getIssue(issueId);
        githubIssue.comments.clear();
        
        // When status changes
        updated.setStatus(IssueStatus.IN_PROGRESS);
        issueRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then a comment should be added
        assertThat(githubIssue.comments).hasSize(1);
        assertThat(githubIssue.comments.get(0)).contains("Status Update").contains("IN_PROGRESS");
    }
    
    @Test
    void shouldCloseIssueWhenIssueCompleted() {
        // Given an issue with an existing GitHub issue
        Issue issue = new Issue("Test issue", "test-agent");
        issueRepository.save(issue);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue githubIssue = mockGitHubApiClient.getIssue(issueId);
        githubIssue.comments.clear();
        
        // When issue is completed
        updated.setStatus(IssueStatus.COMPLETED);
        issueRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the issue should be closed with appropriate labels and comments
        assertThat(githubIssue.closed).isTrue();
        assertThat(githubIssue.labels).contains("status:completed");
        assertThat(githubIssue.comments).hasSize(1);
        assertThat(githubIssue.comments.get(0)).contains("Task completed");
    }
    
    @Test
    void shouldCloseIssueWhenIssueFailed() {
        // Given an issue with an existing GitHub issue
        Issue issue = new Issue("Test issue", "test-agent");
        issueRepository.save(issue);
        
        // First sync to create the issue
        gitHubIntegrationService.synchronizeWithGitHub();
        
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        Long issueId = updated.getGithubIssueId();
        
        // Clear initial comments (from first sync)
        MockGitHubApiClient.MockIssue githubIssue = mockGitHubApiClient.getIssue(issueId);
        githubIssue.comments.clear();
        
        // When issue fails
        updated.setStatus(IssueStatus.FAILED);
        issueRepository.save(updated);
        
        // And synchronization runs again
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the issue should be closed with appropriate labels and comments
        assertThat(githubIssue.closed).isTrue();
        assertThat(githubIssue.labels).contains("status:failed");
        assertThat(githubIssue.comments).hasSize(1);
        assertThat(githubIssue.comments.get(0)).contains("Task failed");
    }
    
    @Test
    void shouldSkipSyncWhenGitHubApiNotAvailable() {
        // Given GitHub API is not available
        mockGitHubApiClient.setAvailable(false);
        
        // And a new issue
        Issue issue = new Issue("Test issue", "test-agent");
        issueRepository.save(issue);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then no GitHub issue should be created
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNull();
    }
    
    @Test
    void shouldHandleGitHubApiFailuresGracefully() {
        // Given GitHub API operations will fail
        mockGitHubApiClient.setShouldFailOperations(true);
        
        // And a new issue
        Issue issue = new Issue("Test issue", "test-agent");
        issueRepository.save(issue);
        
        // When synchronization runs
        gitHubIntegrationService.synchronizeWithGitHub();
        
        // Then the issue should remain unchanged (no GitHub issue ID)
        Issue updated = issueRepository.findById(issue.getId()).orElseThrow();
        assertThat(updated.getGithubIssueId()).isNull();
    }
}