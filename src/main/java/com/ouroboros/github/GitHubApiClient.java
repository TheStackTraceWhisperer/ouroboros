package com.ouroboros.github;

import java.util.List;

/**
 * Interface for GitHub API operations needed for goal proposal synchronization.
 */
public interface GitHubApiClient {
    
    /**
     * Create a new GitHub issue.
     * 
     * @param title The issue title
     * @param body The issue body/description
     * @return The GitHub issue ID
     * @throws GitHubApiException if the operation fails
     */
    Long createIssue(String title, String body) throws GitHubApiException;
    
    /**
     * Add a comment to an existing GitHub issue.
     * 
     * @param issueId The GitHub issue ID
     * @param comment The comment text
     * @throws GitHubApiException if the operation fails
     */
    void addComment(Long issueId, String comment) throws GitHubApiException;
    
    /**
     * Close a GitHub issue.
     * 
     * @param issueId The GitHub issue ID
     * @throws GitHubApiException if the operation fails
     */
    void closeIssue(Long issueId) throws GitHubApiException;
    
    /**
     * Add labels to a GitHub issue.
     * 
     * @param issueId The GitHub issue ID
     * @param labels The labels to add
     * @throws GitHubApiException if the operation fails
     */
    void addLabels(Long issueId, List<String> labels) throws GitHubApiException;
    
    /**
     * Check if the client is properly configured and can connect to GitHub.
     * 
     * @return true if the client is available
     */
    boolean isAvailable();
}