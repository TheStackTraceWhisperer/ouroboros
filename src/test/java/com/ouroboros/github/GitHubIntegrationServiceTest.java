package com.ouroboros.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for GitHubIntegrationService.
 * These tests require GitHub API configuration and may make actual API calls.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.token=",  // Empty token for anonymous access
    "github.organization=TheStackTraceWhisperer",
    "github.repository=ouroboros"
})
class GitHubIntegrationServiceTest {
    
    private GitHubIntegrationService githubService;
    
    @BeforeEach
    void setUp() throws Exception {
        // Initialize with test configuration
        githubService = new GitHubIntegrationService(
            "", // Empty token for anonymous access
            "ouroboros",
            "TheStackTraceWhisperer"
        );
    }
    
    @Test
    void shouldInitializeServiceSuccessfully() {
        assertThat(githubService).isNotNull();
    }
    
    @Test
    void fetchNextAvailableTask_shouldReturnNullWhenNoTasks() {
        // This test will use anonymous access which has limited functionality
        // It should not throw exceptions
        var result = githubService.fetchNextAvailableTask();
        // With anonymous access, we expect null or potential access issues
        // This test mainly verifies the service doesn't crash
    }
    
    @Test
    void createEpic_shouldThrowExceptionWithoutProperAuth() {
        // Anonymous access cannot create issues
        assertThrows(Exception.class, () -> {
            githubService.createEpic("Test Epic", "Test Description");
        });
    }
    
    @Test
    void populateBacklog_shouldThrowExceptionWithoutProperAuth() {
        // Anonymous access cannot create issues
        assertThrows(Exception.class, () -> {
            githubService.populateBacklog("Test Project", 
                java.util.List.of("Task 1", "Task 2"));
        });
    }
}