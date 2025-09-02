package com.ouroboros.service;

import com.ouroboros.github.GitHubApiClient;
import com.ouroboros.github.GitHubApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for managing GitHub Projects integration.
 * Provides project-based organization for large feature design and tracking.
 */
@Service
public class GitHubProjectsService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubProjectsService.class);
    
    @Value("${github.integration.enabled:false}")
    private boolean integrationEnabled;
    
    private final GitHubApiClient gitHubApiClient;
    
    @Autowired
    public GitHubProjectsService(GitHubApiClient gitHubApiClient) {
        this.gitHubApiClient = gitHubApiClient;
    }
    
    /**
     * Create a new project for large feature tracking.
     */
    public Long createFeatureProject(String featureName, String description) {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping project creation");
            return null;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping project creation");
            return null;
        }
        
        try {
            String title = "Feature: " + featureName;
            String body = String.format("""
                    ## Feature Project
                    
                    **Feature Name:** %s
                    
                    **Description:** %s
                    
                    **Project Type:** Large Feature Design and Tracking
                    
                    ---
                    *This project was automatically created by the Agent Observability system for feature tracking.*
                    """, featureName, description);
            
            Long projectId = gitHubApiClient.createProject(title, body);
            log.info("Created GitHub project #{} for feature: {}", projectId, featureName);
            return projectId;
            
        } catch (GitHubApiException e) {
            log.error("Failed to create GitHub project for feature: {}", featureName, e);
            return null;
        }
    }
    
    /**
     * Get all projects for the repository.
     */
    public List<Long> getAllProjects() {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, returning empty project list");
            return List.of();
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, returning empty project list");
            return List.of();
        }
        
        try {
            return gitHubApiClient.getProjects();
        } catch (GitHubApiException e) {
            log.error("Failed to get GitHub projects", e);
            return List.of();
        }
    }
    
    /**
     * Add an issue to a project for tracking.
     */
    public void addIssueToProject(Long projectId, Long issueId) {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping add issue to project");
            return;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping add issue to project");
            return;
        }
        
        try {
            gitHubApiClient.addIssueToProject(projectId, issueId);
            log.info("Added issue #{} to project #{}", issueId, projectId);
        } catch (GitHubApiException e) {
            log.error("Failed to add issue #{} to project #{}", issueId, projectId, e);
        }
    }
    
    /**
     * Update the status of a project item.
     */
    public void updateProjectItemStatus(Long projectId, Long itemId, String status) {
        if (!integrationEnabled) {
            log.debug("GitHub integration is disabled, skipping project item status update");
            return;
        }
        
        if (!gitHubApiClient.isAvailable()) {
            log.warn("GitHub API client is not available, skipping project item status update");
            return;
        }
        
        try {
            gitHubApiClient.updateProjectItemStatus(projectId, itemId, status);
            log.info("Updated project #{} item #{} status to: {}", projectId, itemId, status);
        } catch (GitHubApiException e) {
            log.error("Failed to update project #{} item #{} status", projectId, itemId, e);
        }
    }
}