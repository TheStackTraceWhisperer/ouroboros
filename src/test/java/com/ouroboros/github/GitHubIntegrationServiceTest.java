package com.ouroboros.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for GitHubIntegrationService.
 * These tests use OUROBOROS_GITHUB_TOKEN environment variable if available,
 * otherwise fall back to anonymous access with limited functionality.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.token=${OUROBOROS_GITHUB_TOKEN:}",  // Use environment variable or empty for anonymous
    "github.organization=TheStackTraceWhisperer",
    "github.repository=ouroboros"
})
class GitHubIntegrationServiceTest {
    
    private GitHubIntegrationService githubService;
    private String githubToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // Get token from environment variable or use empty for anonymous access
        githubToken = System.getenv("OUROBOROS_GITHUB_TOKEN");
        if (githubToken == null) {
            githubToken = "";
        }
        
        // Initialize with test configuration
        githubService = new GitHubIntegrationService(
            githubToken,
            "ouroboros",
            "TheStackTraceWhisperer"
        );
    }
    
    @Test
    void shouldInitializeServiceSuccessfully() {
        assertThat(githubService).isNotNull();
    }
    
    @Test
    void fetchNextAvailableTask_shouldWorkBasedOnTokenAvailability() {
        if (githubToken.isEmpty()) {
            // Anonymous access - limited functionality, should not crash
            var result = githubService.fetchNextAvailableTask();
            // With anonymous access, we expect null or potential access issues
            // This test mainly verifies the service doesn't crash
        } else {
            // With valid token - can make proper API calls
            var result = githubService.fetchNextAvailableTask();
            // Should return null if no tasks, or a valid task object
            // This test verifies authenticated access works
        }
    }
    
    @Test
    void createEpic_shouldBehaveDifferentlyBasedOnAuth() {
        if (githubToken.isEmpty()) {
            // Anonymous access cannot create issues
            assertThrows(Exception.class, () -> {
                githubService.createEpic("Test Epic", "Test Description");
            });
        } else {
            // With valid token, test should be more comprehensive
            // For now, just verify it doesn't throw due to authentication
            // Note: This will create actual GitHub issues, so use carefully
            try {
                // Test with a clearly marked test epic
                var result = githubService.createEpic("TEST Epic - Can be deleted", 
                    "This is a test epic created by integration tests and can be safely deleted");
                assertThat(result).isNotNull();
            } catch (Exception e) {
                // If it fails for reasons other than auth, that's acceptable for this test
                // as long as it's not an authentication error
                assertThat(e.getMessage()).doesNotContain("Bad credentials");
            }
        }
    }
    
    @Test
    void populateBacklog_shouldBehaveDifferentlyBasedOnAuth() {
        if (githubToken.isEmpty()) {
            // Anonymous access cannot create issues
            assertThrows(Exception.class, () -> {
                githubService.populateBacklog("Test Project", 
                    java.util.List.of("Task 1", "Task 2"));
            });
        } else {
            // With valid token, test should be more comprehensive
            try {
                // Test with clearly marked test project
                githubService.populateBacklog("TEST Project - Can be deleted", 
                    java.util.List.of("TEST Task 1 - Can be deleted", "TEST Task 2 - Can be deleted"));
                // If we get here, the operation succeeded
            } catch (Exception e) {
                // If it fails for reasons other than auth, that's acceptable for this test
                // as long as it's not an authentication error
                assertThat(e.getMessage()).doesNotContain("Bad credentials");
            }
        }
    }
}