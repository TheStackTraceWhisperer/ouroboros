package com.ouroboros.service;

import com.ouroboros.github.GitHubApiClient;
import com.ouroboros.github.GitHubApiException;
import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for synchronizing issues with GitHub Issues.
 * Provides observability and control over agent activities through GitHub.
 */
@Service
public class GitHubIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationService.class);
    
    private final IssueRepository issueRepository;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubProjectsService gitHubProjectsService;
    
    @Value("${github.integration.enabled:true}")
    private boolean integrationEnabled;
    
    private LocalDateTime lastSyncTime = LocalDateTime.now().minusHours(1);
    
    @Autowired
    public GitHubIntegrationService(IssueRepository issueRepository, 
                                   GitHubApiClient gitHubApiClient,
                                   GitHubProjectsService gitHubProjectsService) {
        this.issueRepository = issueRepository;
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubProjectsService = gitHubProjectsService;
    }
    
    /**
     * Main synchronization method called periodically.
     * Handles all aspects of issue to GitHub issue synchronization.
     */
    @Scheduled(fixedRateString = "${github.integration.sync.interval:60000}")
    @Transactional
    public void synchronizeWithGitHub() {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping synchronization");
            return;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping synchronization");
            return;
        }
        
        log.info("Starting GitHub synchronization");
        
        try {
            // 1. Sync new issues to GitHub issues
            syncNewIssuesToGitHub();
            
            // 2. Sync status changes to issue comments
            syncStatusChangesToComments();
            
            // 3. Sync completed/failed issues to issue closure
            syncCompletedIssuesToClosure();
            
            lastSyncTime = LocalDateTime.now();
            log.info("GitHub synchronization completed successfully");
            
        } catch (Exception e) {
            log.error("GitHub synchronization failed", e);
        }
    }
    
    /**
     * Sync new PENDING issues to GitHub issues.
     */
    private void syncNewIssuesToGitHub() {
        List<Issue> newIssues = issueRepository.findByGithubIssueIdIsNull();
        
        for (Issue issue : newIssues) {
            try {
                String title = generateIssueTitle(issue);
                String body = generateIssueBody(issue);
                
                Long issueId = gitHubApiClient.createIssue(title, body);
                issue.setGithubIssueId(issueId);
                issueRepository.save(issue);
                
                log.info("Created GitHub issue #{} for issue {}", issueId, issue.getId());
                
            } catch (GitHubApiException e) {
                log.error("Failed to create GitHub issue for issue {}", issue.getId(), e);
            }
        }
    }
    
    /**
     * Sync status changes to GitHub issue comments.
     */
    private void syncStatusChangesToComments() {
        List<Issue> updatedIssues = issueRepository.findSyncedIssuesUpdatedSince(lastSyncTime);
        
        for (Issue issue : updatedIssues) {
            if (issue.getGithubIssueId() != null && 
                issue.getStatus() != IssueStatus.PENDING &&
                issue.getStatus() != IssueStatus.COMPLETED &&
                issue.getStatus() != IssueStatus.FAILED) { // Skip final states - they get handled separately
                try {
                    String comment = generateStatusUpdateComment(issue);
                    gitHubApiClient.addComment(issue.getGithubIssueId(), comment);
                    
                    log.info("Added status update comment to GitHub issue #{} for issue {}", 
                            issue.getGithubIssueId(), issue.getId());
                    
                } catch (GitHubApiException e) {
                    log.error("Failed to add comment to GitHub issue #{} for issue {}", 
                             issue.getGithubIssueId(), issue.getId(), e);
                }
            }
        }
    }
    
    /**
     * Sync completed/failed issues to GitHub issue closure.
     */
    private void syncCompletedIssuesToClosure() {
        List<Issue> completedIssues = issueRepository.findByStatusAndUpdatedAtGreaterThan(
                IssueStatus.COMPLETED, lastSyncTime);
        List<Issue> failedIssues = issueRepository.findByStatusAndUpdatedAtGreaterThan(
                IssueStatus.FAILED, lastSyncTime);
        
        // Handle completed issues
        for (Issue issue : completedIssues) {
            closeIssue(issue, "status:completed");
        }
        
        // Handle failed issues
        for (Issue issue : failedIssues) {
            closeIssue(issue, "status:failed");
        }
    }
    
    /**
     * Close a GitHub issue for a completed or failed issue.
     */
    private void closeIssue(Issue issue, String statusLabel) {
        if (issue.getGithubIssueId() != null) {
            try {
                // Add final summary comment
                String finalComment = generateFinalSummaryComment(issue);
                gitHubApiClient.addComment(issue.getGithubIssueId(), finalComment);
                
                // Add status label
                gitHubApiClient.addLabels(issue.getGithubIssueId(), List.of(statusLabel));
                
                // Close the issue
                gitHubApiClient.closeIssue(issue.getGithubIssueId());
                
                log.info("Closed GitHub issue #{} for {} issue {}", 
                        issue.getGithubIssueId(), issue.getStatus(), issue.getId());
                
            } catch (GitHubApiException e) {
                log.error("Failed to close GitHub issue #{} for issue {}", 
                         issue.getGithubIssueId(), issue.getId(), e);
            }
        }
    }
    
    /**
     * Generate GitHub issue title for an issue.
     */
    private String generateIssueTitle(Issue issue) {
        String truncatedDescription = issue.getDescription().length() > 100 
                ? issue.getDescription().substring(0, 100) + "..."
                : issue.getDescription();
        return String.format("🤖 Agent Task: %s", truncatedDescription);
    }
    
    /**
     * Generate GitHub issue body for an issue.
     */
    private String generateIssueBody(Issue issue) {
        return String.format("""
                ## Agent Issue
                
                **Description:** %s
                
                **Status:** %s
                **Created by:** %s
                **Created at:** %s
                **Issue ID:** %s
                
                ---
                *This issue was automatically created by the Agent Observability system to track issue execution.*
                """, 
                issue.getDescription(),
                issue.getStatus(),
                issue.getCreatedBy() != null ? issue.getCreatedBy() : "System",
                issue.getCreatedAt(),
                issue.getId());
    }
    
    /**
     * Generate status update comment for an issue.
     */
    private String generateStatusUpdateComment(Issue issue) {
        return String.format("🤖 **Status Update:** Task moved to **%s** at %s", 
                issue.getStatus(), issue.getUpdatedAt());
    }
    
    /**
     * Generate final summary comment for a completed/failed issue.
     */
    private String generateFinalSummaryComment(Issue issue) {
        String emoji = issue.getStatus() == IssueStatus.COMPLETED ? "✅" : "❌";
        return String.format("%s **Task %s** at %s\n\n*This issue is now closed as the issue has reached its final state.*", 
                emoji, issue.getStatus().name().toLowerCase(), issue.getUpdatedAt());
    }
    
    /**
     * Create a GitHub project for large feature tracking.
     * This provides higher-level organization for complex initiatives.
     */
    public Long createFeatureProject(String featureName, String description) {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping feature project creation");
            return null;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping feature project creation");
            return null;
        }
        
        return gitHubProjectsService.createFeatureProject(featureName, description);
    }
    
    /**
     * Add an issue to a feature project for tracking.
     */
    public void addIssueToFeatureProject(Long projectId, Long issueId) {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping add issue to project");
            return;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping add issue to project");
            return;
        }
        
        gitHubProjectsService.addIssueToProject(projectId, issueId);
    }
}