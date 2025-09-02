package com.ouroboros.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an issue that can be synchronized with GitHub Issues.
 * This serves as the foundation for agent observability and control.
 */
@Entity
@Table(name = "goal_proposals")
public class Issue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false, length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;
    
    @Column(name = "github_issue_id")
    private Long githubIssueId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    // Default constructor required by JPA
    protected Issue() {}
    
    public Issue(String description, String createdBy) {
        this.description = description;
        this.status = IssueStatus.PENDING;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public IssueStatus getStatus() {
        return status;
    }
    
    public void setStatus(IssueStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public Long getGithubIssueId() {
        return githubIssueId;
    }
    
    public void setGithubIssueId(Long githubIssueId) {
        this.githubIssueId = githubIssueId;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}