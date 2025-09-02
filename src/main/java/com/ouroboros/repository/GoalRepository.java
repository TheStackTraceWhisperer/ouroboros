package com.ouroboros.repository;

import com.ouroboros.model.Goal;
import com.ouroboros.model.GoalStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing goals.
 * Provides methods to fetch pending goals and update their status.
 */
public interface GoalRepository {
    
    /**
     * Fetches the next pending goal from the repository.
     * 
     * @return Optional containing the next pending goal, or empty if none available
     */
    Optional<Goal> fetchNextGoal();
    
    /**
     * Updates the status of a goal.
     * 
     * @param goal the goal to update
     * @return the updated goal
     */
    Goal updateGoalStatus(Goal goal);
    
    /**
     * Saves a new goal to the repository.
     * 
     * @param goal the goal to save
     * @return the saved goal with assigned ID
     */
    Goal save(Goal goal);
    
    /**
     * Finds a goal by its ID.
     * 
     * @param id the goal ID
     * @return Optional containing the goal if found
     */
    Optional<Goal> findById(Long id);
    
    /**
     * Finds all goals with the specified status.
     * 
     * @param status the goal status to filter by
     * @return list of goals with the specified status
     */
    List<Goal> findByStatus(GoalStatus status);
}