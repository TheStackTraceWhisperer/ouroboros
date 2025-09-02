package com.ouroboros.github;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock implementation of GitHubApiClient for testing.
 * Simulates GitHub API operations without making actual network calls.
 */
@TestConfiguration
public class MockGitHubApiClient implements GitHubApiClient {
    
    private final AtomicLong issueIdCounter = new AtomicLong(1);
    private final Map<Long, MockIssue> issues = new HashMap<>();
    private boolean available = true;
    private boolean shouldFailOperations = false;
    
    @Bean
    @Primary
    public GitHubApiClient mockGitHubApiClient() {
        return this;
    }
    
    @Override
    public Long createIssue(String title, String body) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: createIssue");
        }
        
        Long issueId = issueIdCounter.getAndIncrement();
        MockIssue issue = new MockIssue(issueId, title, body);
        issues.put(issueId, issue);
        return issueId;
    }
    
    @Override
    public void addComment(Long issueId, String comment) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: addComment");
        }
        
        MockIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new GitHubApiException("Issue not found: " + issueId);
        }
        
        issue.comments.add(comment);
    }
    
    @Override
    public void closeIssue(Long issueId) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: closeIssue");
        }
        
        MockIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new GitHubApiException("Issue not found: " + issueId);
        }
        
        issue.closed = true;
    }
    
    @Override
    public void addLabels(Long issueId, List<String> labels) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: addLabels");
        }
        
        MockIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new GitHubApiException("Issue not found: " + issueId);
        }
        
        issue.labels.addAll(labels);
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    // GitHub Projects V2 API Methods (Mock implementations)
    
    private final AtomicLong projectIdCounter = new AtomicLong(1);
    private final Map<Long, MockProject> projects = new HashMap<>();
    
    @Override
    public List<Long> getProjects() throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: getProjects");
        }
        
        return new ArrayList<>(projects.keySet());
    }
    
    @Override
    public Long createProject(String title, String body) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: createProject");
        }
        
        Long projectId = projectIdCounter.getAndIncrement();
        MockProject project = new MockProject(projectId, title, body);
        projects.put(projectId, project);
        return projectId;
    }
    
    @Override
    public void addIssueToProject(Long projectId, Long issueId) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: addIssueToProject");
        }
        
        MockProject project = projects.get(projectId);
        if (project == null) {
            throw new GitHubApiException("Project not found: " + projectId);
        }
        
        MockIssue issue = issues.get(issueId);
        if (issue == null) {
            throw new GitHubApiException("Issue not found: " + issueId);
        }
        
        project.issues.add(issueId);
    }
    
    @Override
    public void updateProjectItemStatus(Long projectId, Long itemId, String status) throws GitHubApiException {
        if (shouldFailOperations) {
            throw new GitHubApiException("Mock failure: updateProjectItemStatus");
        }
        
        MockProject project = projects.get(projectId);
        if (project == null) {
            throw new GitHubApiException("Project not found: " + projectId);
        }
        
        project.itemStatuses.put(itemId, status);
    }
    
    // Test helper methods
    public MockIssue getIssue(Long issueId) {
        return issues.get(issueId);
    }
    
    public MockProject getProject(Long projectId) {
        return projects.get(projectId);
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public void setShouldFailOperations(boolean shouldFailOperations) {
        this.shouldFailOperations = shouldFailOperations;
    }
    
    public void reset() {
        issues.clear();
        projects.clear();
        issueIdCounter.set(1);
        projectIdCounter.set(1);
        available = true;
        shouldFailOperations = false;
    }
    
    public static class MockIssue {
        public final Long id;
        public final String title;
        public final String body;
        public final List<String> comments = new ArrayList<>();
        public final List<String> labels = new ArrayList<>();
        public boolean closed = false;
        
        public MockIssue(Long id, String title, String body) {
            this.id = id;
            this.title = title;
            this.body = body;
        }
    }
    
    public static class MockProject {
        public final Long id;
        public final String title;
        public final String body;
        public final List<Long> issues = new ArrayList<>();
        public final Map<Long, String> itemStatuses = new HashMap<>();
        
        public MockProject(Long id, String title, String body) {
            this.id = id;
            this.title = title;
            this.body = body;
        }
    }
}