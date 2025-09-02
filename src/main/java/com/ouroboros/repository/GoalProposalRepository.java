package com.ouroboros.repository;

import com.ouroboros.model.GoalProposal;
import com.ouroboros.model.GoalProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing goal proposals.
 */
@Repository
public interface GoalProposalRepository extends JpaRepository<GoalProposal, UUID> {
    
    /**
     * Find all proposals with a specific status.
     */
    List<GoalProposal> findByStatus(GoalProposalStatus status);
    
    /**
     * Find proposals that don't have a GitHub issue ID (not yet synced).
     */
    List<GoalProposal> findByGithubIssueIdIsNull();
    
    /**
     * Find proposals that have been updated since a specific time.
     */
    List<GoalProposal> findByUpdatedAtGreaterThan(LocalDateTime since);
    
    /**
     * Find proposals by status that have been updated since a specific time.
     */
    @Query("SELECT gp FROM GoalProposal gp WHERE gp.status = :status AND gp.updatedAt > :since")
    List<GoalProposal> findByStatusAndUpdatedAtGreaterThan(@Param("status") GoalProposalStatus status, 
                                                          @Param("since") LocalDateTime since);
    
    /**
     * Find proposals that have a GitHub issue ID but status changed since last sync.
     */
    @Query("SELECT gp FROM GoalProposal gp WHERE gp.githubIssueId IS NOT NULL AND gp.updatedAt > :since")
    List<GoalProposal> findSyncedProposalsUpdatedSince(@Param("since") LocalDateTime since);
}