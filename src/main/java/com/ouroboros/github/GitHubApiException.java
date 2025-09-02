package com.ouroboros.github;

/**
 * Exception thrown when GitHub API operations fail.
 */
public class GitHubApiException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public GitHubApiException(String message) {
        super(message);
    }
    
    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}