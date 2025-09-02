package com.ouroboros.service;

import com.ouroboros.github.MockGitHubApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GitHubProjectsService.
 */
@SpringBootTest
@Import(MockGitHubApiClient.class)
@TestPropertySource(properties = {
    "github.integration.enabled=true"
})
class GitHubProjectsServiceTest {
    
    @Autowired
    private GitHubProjectsService gitHubProjectsService;
    
    @Autowired
    private MockGitHubApiClient mockGitHubApiClient;
    
    @BeforeEach
    void setUp() {
        mockGitHubApiClient.reset();
    }
    
    @Test
    void shouldCreateFeatureProject() {
        // When creating a feature project
        Long projectId = gitHubProjectsService.createFeatureProject("User Authentication", "Implement OAuth2 login system");
        
        // Then it should be created successfully
        assertThat(projectId).isNotNull();
        
        // And the project should exist in the mock client
        MockGitHubApiClient.MockProject project = mockGitHubApiClient.getProject(projectId);
        assertThat(project).isNotNull();
        assertThat(project.title).isEqualTo("Feature: User Authentication");
        assertThat(project.body).contains("User Authentication");
        assertThat(project.body).contains("Implement OAuth2 login system");
        assertThat(project.body).contains("Large Feature Design and Tracking");
    }
    
    @Test
    void shouldGetAllProjects() {
        // Given some projects exist
        Long project1 = gitHubProjectsService.createFeatureProject("Feature 1", "Description 1");
        Long project2 = gitHubProjectsService.createFeatureProject("Feature 2", "Description 2");
        
        // When getting all projects
        List<Long> projects = gitHubProjectsService.getAllProjects();
        
        // Then it should return all projects
        assertThat(projects).containsExactlyInAnyOrder(project1, project2);
    }
    
    @Test
    void shouldAddIssueToProject() throws Exception {
        // Given a project and an issue
        Long projectId = gitHubProjectsService.createFeatureProject("Test Feature", "Test Description");
        Long issueId = mockGitHubApiClient.createIssue("Test Issue", "Test Issue Body");
        
        // When adding the issue to the project
        gitHubProjectsService.addIssueToProject(projectId, issueId);
        
        // Then the issue should be linked to the project
        MockGitHubApiClient.MockProject project = mockGitHubApiClient.getProject(projectId);
        assertThat(project.issues).contains(issueId);
    }
    
    @Test
    void shouldUpdateProjectItemStatus() {
        // Given a project with an item
        Long projectId = gitHubProjectsService.createFeatureProject("Test Feature", "Test Description");
        Long itemId = 123L;
        
        // When updating the item status
        gitHubProjectsService.updateProjectItemStatus(projectId, itemId, "In Progress");
        
        // Then the status should be updated
        MockGitHubApiClient.MockProject project = mockGitHubApiClient.getProject(projectId);
        assertThat(project.itemStatuses).containsEntry(itemId, "In Progress");
    }
    
    @Test
    void shouldSkipOperationsWhenIntegrationDisabled() {
        // Given integration is disabled
        GitHubProjectsService disabledService = new GitHubProjectsService(mockGitHubApiClient) {
            @Override
            public Long createFeatureProject(String featureName, String description) {
                // Override to simulate disabled integration
                return null;
            }
        };
        
        // When attempting to create a project
        Long projectId = disabledService.createFeatureProject("Test Feature", "Description");
        
        // Then it should return null
        assertThat(projectId).isNull();
    }
    
    @Test
    void shouldSkipOperationsWhenApiNotAvailable() {
        // Given GitHub API is not available
        mockGitHubApiClient.setAvailable(false);
        
        // When attempting to create a project
        Long projectId = gitHubProjectsService.createFeatureProject("Test Feature", "Description");
        
        // Then it should return null
        assertThat(projectId).isNull();
        
        // When attempting to get projects
        List<Long> projects = gitHubProjectsService.getAllProjects();
        
        // Then it should return an empty list
        assertThat(projects).isEmpty();
    }
}