package com.ouroboros.github;

import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GitHubApiClient using the GitHub API library.
 * Handles authentication and core GitHub operations for goal proposal synchronization.
 */
@Service
public class GitHubApiClientImpl implements GitHubApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubApiClientImpl.class);
    
    @Value("${github.integration.token:}")
    private String githubToken;
    
    @Value("${github.integration.repository.owner:}")
    private String repositoryOwner;
    
    @Value("${github.integration.repository.name:}")
    private String repositoryName;
    
    private GitHub github;
    private GHRepository repository;
    
    /**
     * Initialize GitHub client connection.
     */
    private void initializeGitHub() throws GitHubApiException {
        if (github == null) {
            if (githubToken == null || githubToken.trim().isEmpty()) {
                throw new GitHubApiException("GitHub token is not configured");
            }
            
            try {
                github = new GitHubBuilder().withOAuthToken(githubToken).build();
                repository = github.getRepository(repositoryOwner + "/" + repositoryName);
                log.info("Successfully connected to GitHub repository: {}/{}", repositoryOwner, repositoryName);
            } catch (IOException e) {
                throw new GitHubApiException("Failed to connect to GitHub", e);
            }
        }
    }
    
    @Override
    public Long createIssue(String title, String body) throws GitHubApiException {
        try {
            initializeGitHub();
            
            GHIssueBuilder issueBuilder = repository.createIssue(title);
            if (body != null && !body.trim().isEmpty()) {
                issueBuilder.body(body);
            }
            
            GHIssue issue = issueBuilder.create();
            log.info("Created GitHub issue #{} with title: {}", issue.getNumber(), title);
            return (long) issue.getNumber();
            
        } catch (IOException e) {
            throw new GitHubApiException("Failed to create GitHub issue", e);
        }
    }
    
    @Override
    public void addComment(Long issueId, String comment) throws GitHubApiException {
        try {
            initializeGitHub();
            
            GHIssue issue = repository.getIssue(issueId.intValue());
            issue.comment(comment);
            log.info("Added comment to GitHub issue #{}", issueId);
            
        } catch (IOException e) {
            throw new GitHubApiException("Failed to add comment to GitHub issue #" + issueId, e);
        }
    }
    
    @Override
    public void closeIssue(Long issueId) throws GitHubApiException {
        try {
            initializeGitHub();
            
            GHIssue issue = repository.getIssue(issueId.intValue());
            issue.close();
            log.info("Closed GitHub issue #{}", issueId);
            
        } catch (IOException e) {
            throw new GitHubApiException("Failed to close GitHub issue #" + issueId, e);
        }
    }
    
    @Override
    public void addLabels(Long issueId, List<String> labels) throws GitHubApiException {
        try {
            initializeGitHub();
            
            GHIssue issue = repository.getIssue(issueId.intValue());
            issue.addLabels(labels.toArray(new String[0]));
            log.info("Added labels {} to GitHub issue #{}", labels, issueId);
            
        } catch (IOException e) {
            throw new GitHubApiException("Failed to add labels to GitHub issue #" + issueId, e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        try {
            initializeGitHub();
            return true;
        } catch (GitHubApiException e) {
            log.warn("GitHub API client is not available: {}", e.getMessage());
            return false;
        }
    }
    
    // GitHub Projects V2 API Methods
    
    @Override
    public List<Long> getProjects() throws GitHubApiException {
        try {
            initializeGitHub();
            
            // Note: GitHub Projects V2 API requires GraphQL or REST API v2
            // For now, we'll return an empty list and log that this feature needs GraphQL implementation
            log.warn("GitHub Projects V2 API requires GraphQL implementation - returning empty project list");
            return new ArrayList<>();
            
        } catch (Exception e) {
            throw new GitHubApiException("Failed to get projects", e);
        }
    }
    
    @Override
    public Long createProject(String title, String body) throws GitHubApiException {
        try {
            initializeGitHub();
            
            // Note: GitHub Projects V2 API requires GraphQL
            // For now, we'll simulate creating a project by logging and returning a dummy ID
            log.warn("GitHub Projects V2 API requires GraphQL implementation - simulating project creation");
            log.info("Would create project with title: {} and body: {}", title, body);
            
            // Return a dummy project ID for now
            return System.currentTimeMillis();
            
        } catch (Exception e) {
            throw new GitHubApiException("Failed to create project", e);
        }
    }
    
    @Override
    public void addIssueToProject(Long projectId, Long issueId) throws GitHubApiException {
        try {
            initializeGitHub();
            
            // Note: GitHub Projects V2 API requires GraphQL
            log.warn("GitHub Projects V2 API requires GraphQL implementation - simulating adding issue to project");
            log.info("Would add issue #{} to project #{}", issueId, projectId);
            
        } catch (Exception e) {
            throw new GitHubApiException("Failed to add issue to project", e);
        }
    }
    
    @Override
    public void updateProjectItemStatus(Long projectId, Long itemId, String status) throws GitHubApiException {
        try {
            initializeGitHub();
            
            // Note: GitHub Projects V2 API requires GraphQL
            log.warn("GitHub Projects V2 API requires GraphQL implementation - simulating status update");
            log.info("Would update project #{} item #{} status to: {}", projectId, itemId, status);
            
        } catch (Exception e) {
            throw new GitHubApiException("Failed to update project item status", e);
        }
    }

    @Override
    public String getIssueStatus(Long issueId) throws GitHubApiException {
        try {
            initializeGitHub();
            GHIssue issue = repository.getIssue(issueId.intValue());
            return issue.getState().toString(); // GHIssueState enum -> String
        } catch (IOException e) {
            throw new GitHubApiException("Failed to get status for GitHub issue #" + issueId, e);
        }
    }
}