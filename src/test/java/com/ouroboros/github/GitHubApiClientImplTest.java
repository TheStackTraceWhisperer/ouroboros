package com.ouroboros.github;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GitHubApiClientImpl.
 */
class GitHubApiClientImplTest {
    
    @Test
    void shouldReturnFalseWhenTokenNotConfigured() {
        // Given a client with no token
        GitHubApiClientImpl client = new GitHubApiClientImpl();
        ReflectionTestUtils.setField(client, "githubToken", "");
        ReflectionTestUtils.setField(client, "repositoryOwner", "test-owner");
        ReflectionTestUtils.setField(client, "repositoryName", "test-repo");
        
        // When checking availability
        boolean available = client.isAvailable();
        
        // Then it should not be available
        assertThat(available).isFalse();
    }
    
    @Test
    void shouldReturnFalseWhenTokenIsNull() {
        // Given a client with null token
        GitHubApiClientImpl client = new GitHubApiClientImpl();
        ReflectionTestUtils.setField(client, "githubToken", null);
        ReflectionTestUtils.setField(client, "repositoryOwner", "test-owner");
        ReflectionTestUtils.setField(client, "repositoryName", "test-repo");
        
        // When checking availability
        boolean available = client.isAvailable();
        
        // Then it should not be available
        assertThat(available).isFalse();
    }
}