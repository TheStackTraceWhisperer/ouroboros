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
    
    // GitHub Projects V2 API Methods
    
    /**
     * Get all projects for the repository.
     * 
     * @return List of project IDs
     * @throws GitHubApiException if the operation fails
     */
    List<Long> getProjects() throws GitHubApiException;
    
    /**
     * Create a new project.
     * 
     * @param title The project title
     * @param body The project description
     * @return The project ID
     * @throws GitHubApiException if the operation fails
     */
    Long createProject(String title, String body) throws GitHubApiException;
    
    /**
     * Add an issue to a project.
     * 
     * @param projectId The project ID
     * @param issueId The issue ID
     * @throws GitHubApiException if the operation fails
     */
    void addIssueToProject(Long projectId, Long issueId) throws GitHubApiException;
    
    /**
     * Update project item status.
     * 
     * @param projectId The project ID
     * @param itemId The project item ID
     * @param status The new status
     * @throws GitHubApiException if the operation fails
     */
    void updateProjectItemStatus(Long projectId, Long itemId, String status) throws GitHubApiException;
}