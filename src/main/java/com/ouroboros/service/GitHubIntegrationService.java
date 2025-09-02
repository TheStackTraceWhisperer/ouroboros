package com.ouroboros.service;

import com.ouroboros.github.GitHubApiClient;
import com.ouroboros.github.GitHubApiException;
import com.ouroboros.model.GoalProposal;
import com.ouroboros.model.GoalProposalStatus;
import com.ouroboros.repository.GoalProposalRepository;
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
 * Service responsible for synchronizing goal proposals with GitHub Issues.
 * Provides observability and control over agent activities through GitHub.
 */
@Service
public class GitHubIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationService.class);
    
    private final GoalProposalRepository proposalRepository;
    private final GitHubApiClient gitHubApiClient;
    private final GitHubProjectsService gitHubProjectsService;
    
    @Value("${github.integration.enabled:true}")
    private boolean integrationEnabled;
    
    private LocalDateTime lastSyncTime = LocalDateTime.now().minusHours(1);
    
    @Autowired
    public GitHubIntegrationService(GoalProposalRepository proposalRepository, 
                                   GitHubApiClient gitHubApiClient,
                                   GitHubProjectsService gitHubProjectsService) {
        this.proposalRepository = proposalRepository;
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubProjectsService = gitHubProjectsService;
    }
    
    /**
     * Main synchronization method called periodically.
     * Handles all aspects of goal proposal to GitHub issue synchronization.
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
            // 1. Sync new proposals to GitHub issues
            syncNewProposalsToIssues();
            
            // 2. Sync status changes to issue comments
            syncStatusChangesToComments();
            
            // 3. Sync completed/failed proposals to issue closure
            syncCompletedProposalsToIssueClosure();
            
            lastSyncTime = LocalDateTime.now();
            log.info("GitHub synchronization completed successfully");
            
        } catch (Exception e) {
            log.error("GitHub synchronization failed", e);
        }
    }
    
    /**
     * Sync new PENDING proposals to GitHub issues.
     */
    private void syncNewProposalsToIssues() {
        List<GoalProposal> newProposals = proposalRepository.findByGithubIssueIdIsNull();
        
        for (GoalProposal proposal : newProposals) {
            try {
                String title = generateIssueTitle(proposal);
                String body = generateIssueBody(proposal);
                
                Long issueId = gitHubApiClient.createIssue(title, body);
                proposal.setGithubIssueId(issueId);
                proposalRepository.save(proposal);
                
                log.info("Created GitHub issue #{} for proposal {}", issueId, proposal.getId());
                
            } catch (GitHubApiException e) {
                log.error("Failed to create GitHub issue for proposal {}", proposal.getId(), e);
            }
        }
    }
    
    /**
     * Sync status changes to GitHub issue comments.
     */
    private void syncStatusChangesToComments() {
        List<GoalProposal> updatedProposals = proposalRepository.findSyncedProposalsUpdatedSince(lastSyncTime);
        
        for (GoalProposal proposal : updatedProposals) {
            if (proposal.getGithubIssueId() != null && 
                proposal.getStatus() != GoalProposalStatus.PENDING &&
                proposal.getStatus() != GoalProposalStatus.COMPLETED &&
                proposal.getStatus() != GoalProposalStatus.FAILED) { // Skip final states - they get handled separately
                try {
                    String comment = generateStatusUpdateComment(proposal);
                    gitHubApiClient.addComment(proposal.getGithubIssueId(), comment);
                    
                    log.info("Added status update comment to GitHub issue #{} for proposal {}", 
                            proposal.getGithubIssueId(), proposal.getId());
                    
                } catch (GitHubApiException e) {
                    log.error("Failed to add comment to GitHub issue #{} for proposal {}", 
                             proposal.getGithubIssueId(), proposal.getId(), e);
                }
            }
        }
    }
    
    /**
     * Sync completed/failed proposals to GitHub issue closure.
     */
    private void syncCompletedProposalsToIssueClosure() {
        List<GoalProposal> completedProposals = proposalRepository.findByStatusAndUpdatedAtGreaterThan(
                GoalProposalStatus.COMPLETED, lastSyncTime);
        List<GoalProposal> failedProposals = proposalRepository.findByStatusAndUpdatedAtGreaterThan(
                GoalProposalStatus.FAILED, lastSyncTime);
        
        // Handle completed proposals
        for (GoalProposal proposal : completedProposals) {
            closeProposalIssue(proposal, "status:completed");
        }
        
        // Handle failed proposals
        for (GoalProposal proposal : failedProposals) {
            closeProposalIssue(proposal, "status:failed");
        }
    }
    
    /**
     * Close a GitHub issue for a completed or failed proposal.
     */
    private void closeProposalIssue(GoalProposal proposal, String statusLabel) {
        if (proposal.getGithubIssueId() != null) {
            try {
                // Add final summary comment
                String finalComment = generateFinalSummaryComment(proposal);
                gitHubApiClient.addComment(proposal.getGithubIssueId(), finalComment);
                
                // Add status label
                gitHubApiClient.addLabels(proposal.getGithubIssueId(), List.of(statusLabel));
                
                // Close the issue
                gitHubApiClient.closeIssue(proposal.getGithubIssueId());
                
                log.info("Closed GitHub issue #{} for {} proposal {}", 
                        proposal.getGithubIssueId(), proposal.getStatus(), proposal.getId());
                
            } catch (GitHubApiException e) {
                log.error("Failed to close GitHub issue #{} for proposal {}", 
                         proposal.getGithubIssueId(), proposal.getId(), e);
            }
        }
    }
    
    /**
     * Generate GitHub issue title for a proposal.
     */
    private String generateIssueTitle(GoalProposal proposal) {
        String truncatedDescription = proposal.getDescription().length() > 100 
                ? proposal.getDescription().substring(0, 100) + "..."
                : proposal.getDescription();
        return String.format("🤖 Agent Task: %s", truncatedDescription);
    }
    
    /**
     * Generate GitHub issue body for a proposal.
     */
    private String generateIssueBody(GoalProposal proposal) {
        return String.format("""
                ## Agent Goal Proposal
                
                **Description:** %s
                
                **Status:** %s
                **Created by:** %s
                **Created at:** %s
                **Proposal ID:** %s
                
                ---
                *This issue was automatically created by the Agent Observability system to track goal proposal execution.*
                """, 
                proposal.getDescription(),
                proposal.getStatus(),
                proposal.getCreatedBy() != null ? proposal.getCreatedBy() : "System",
                proposal.getCreatedAt(),
                proposal.getId());
    }
    
    /**
     * Generate status update comment for a proposal.
     */
    private String generateStatusUpdateComment(GoalProposal proposal) {
        return String.format("🤖 **Status Update:** Task moved to **%s** at %s", 
                proposal.getStatus(), proposal.getUpdatedAt());
    }
    
    /**
     * Generate final summary comment for a completed/failed proposal.
     */
    private String generateFinalSummaryComment(GoalProposal proposal) {
        String emoji = proposal.getStatus() == GoalProposalStatus.COMPLETED ? "✅" : "❌";
        return String.format("%s **Task %s** at %s\n\n*This issue is now closed as the goal proposal has reached its final state.*", 
                emoji, proposal.getStatus().name().toLowerCase(), proposal.getUpdatedAt());
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