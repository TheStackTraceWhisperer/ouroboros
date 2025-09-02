package com.ouroboros.github;

import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
}