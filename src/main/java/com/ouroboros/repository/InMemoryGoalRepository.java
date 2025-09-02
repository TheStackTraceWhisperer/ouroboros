package com.ouroboros.repository;

import com.ouroboros.model.Goal;
import com.ouroboros.model.GoalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of GoalRepository.
 * This is a simple implementation for development and testing purposes.
 * In production, this would be replaced with a database-backed implementation.
 */
@Repository
public class InMemoryGoalRepository implements GoalRepository {
    
    private static final Logger log = LoggerFactory.getLogger(InMemoryGoalRepository.class);
    
    private final Map<Long, Goal> goals = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Optional<Goal> fetchNextGoal() {
        return goals.values().stream()
                .filter(goal -> goal.status() == GoalStatus.PENDING)
                .findFirst();
    }
    
    @Override
    public Goal updateGoalStatus(Goal goal) {
        if (goal.id() == null) {
            throw new IllegalArgumentException("Cannot update goal status: goal ID is null");
        }
        
        goals.put(goal.id(), goal);
        log.info("Updated goal {} status to {}", goal.id(), goal.status());
        return goal;
    }
    
    @Override
    public Goal save(Goal goal) {
        Goal savedGoal;
        if (goal.id() == null) {
            // New goal - assign ID
            Long id = idGenerator.getAndIncrement();
            savedGoal = new Goal(id, goal.description(), goal.status(), goal.createdAt(), goal.updatedAt(), goal.result());
        } else {
            // Existing goal - update
            savedGoal = goal;
        }
        
        goals.put(savedGoal.id(), savedGoal);
        log.info("Saved goal {}: {}", savedGoal.id(), savedGoal.description());
        return savedGoal;
    }
    
    @Override
    public Optional<Goal> findById(Long id) {
        return Optional.ofNullable(goals.get(id));
    }
    
    @Override
    public List<Goal> findByStatus(GoalStatus status) {
        return goals.values().stream()
                .filter(goal -> goal.status() == status)
                .toList();
    }
    
    /**
     * Utility method to get all goals (for testing/debugging).
     */
    public List<Goal> findAll() {
        return new ArrayList<>(goals.values());
    }
    
    /**
     * Utility method to clear all goals (for testing).
     */
    public void clear() {
        goals.clear();
        idGenerator.set(1);
        log.info("Cleared all goals from repository");
    }
}