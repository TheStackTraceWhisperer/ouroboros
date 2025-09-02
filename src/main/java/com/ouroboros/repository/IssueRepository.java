package com.ouroboros.repository;

import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing issues.
 */
@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {
    
    /**
     * Find all issues with a specific status.
     */
    List<Issue> findByStatus(IssueStatus status);
    
    /**
     * Find issues that don't have a GitHub issue ID (not yet synced).
     */
    List<Issue> findByGithubIssueIdIsNull();
    
    /**
     * Find issues that have been updated since a specific time.
     */
    List<Issue> findByUpdatedAtGreaterThan(LocalDateTime since);
    
    /**
     * Find issues by status that have been updated since a specific time.
     */
    @Query("SELECT i FROM Issue i WHERE i.status = :status AND i.updatedAt > :since")
    List<Issue> findByStatusAndUpdatedAtGreaterThan(@Param("status") IssueStatus status, 
                                                   @Param("since") LocalDateTime since);
    
    /**
     * Find issues that have a GitHub issue ID but status changed since last sync.
     */
    @Query("SELECT i FROM Issue i WHERE i.githubIssueId IS NOT NULL AND i.updatedAt > :since")
    List<Issue> findSyncedIssuesUpdatedSince(@Param("since") LocalDateTime since);
}