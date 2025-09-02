package com.ouroboros.model;

import java.time.LocalDateTime;

/**
 * Represents a goal that the agent should work on.
 * Goals are stored in the goal_proposals table and processed by the agent.
 */
public record Goal(
    Long id,
    String description,
    GoalStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String result
) {
    
    /**
     * Creates a new pending goal with the given description.
     */
    public static Goal pending(String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Goal(null, description, GoalStatus.PENDING, now, now, null);
    }
    
    /**
     * Creates a copy of this goal with the status changed to IN_PROGRESS.
     */
    public Goal markInProgress() {
        return new Goal(id, description, GoalStatus.IN_PROGRESS, createdAt, LocalDateTime.now(), result);
    }
    
    /**
     * Creates a copy of this goal with the status changed to COMPLETED and result set.
     */
    public Goal markCompleted(String result) {
        return new Goal(id, description, GoalStatus.COMPLETED, createdAt, LocalDateTime.now(), result);
    }
    
    /**
     * Creates a copy of this goal with the status changed to FAILED and result set.
     */
    public Goal markFailed(String errorMessage) {
        return new Goal(id, description, GoalStatus.FAILED, createdAt, LocalDateTime.now(), errorMessage);
    }
}