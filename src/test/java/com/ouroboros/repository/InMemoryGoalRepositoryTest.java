package com.ouroboros.repository;

import com.ouroboros.model.Goal;
import com.ouroboros.model.GoalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryGoalRepositoryTest {
    
    private InMemoryGoalRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new InMemoryGoalRepository();
    }
    
    @Test
    void save_shouldAssignIdToNewGoal() {
        // GIVEN a new goal without ID
        Goal newGoal = Goal.pending("Test goal");
        assertThat(newGoal.id()).isNull();
        
        // WHEN saving the goal
        Goal savedGoal = repository.save(newGoal);
        
        // THEN it should have an assigned ID
        assertThat(savedGoal.id()).isNotNull();
        assertThat(savedGoal.description()).isEqualTo("Test goal");
        assertThat(savedGoal.status()).isEqualTo(GoalStatus.PENDING);
    }
    
    @Test
    void save_shouldUpdateExistingGoal() {
        // GIVEN an existing goal
        Goal originalGoal = repository.save(Goal.pending("Original goal"));
        Goal updatedGoal = originalGoal.markInProgress();
        
        // WHEN saving the updated goal
        Goal savedGoal = repository.save(updatedGoal);
        
        // THEN it should maintain the same ID but update other fields
        assertThat(savedGoal.id()).isEqualTo(originalGoal.id());
        assertThat(savedGoal.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(savedGoal.updatedAt()).isAfter(originalGoal.updatedAt());
    }
    
    @Test
    void findById_shouldReturnGoalWhenExists() {
        // GIVEN a saved goal
        Goal savedGoal = repository.save(Goal.pending("Test goal"));
        
        // WHEN finding by ID
        Optional<Goal> foundGoal = repository.findById(savedGoal.id());
        
        // THEN it should return the goal
        assertThat(foundGoal).isPresent();
        assertThat(foundGoal.get()).isEqualTo(savedGoal);
    }
    
    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // WHEN finding a non-existent goal
        Optional<Goal> foundGoal = repository.findById(999L);
        
        // THEN it should return empty
        assertThat(foundGoal).isEmpty();
    }
    
    @Test
    void fetchNextGoal_shouldReturnFirstPendingGoal() {
        // GIVEN multiple goals with different statuses
        Goal pendingGoal1 = repository.save(Goal.pending("First pending"));
        Goal inProgressGoal = repository.save(Goal.pending("In progress").markInProgress());
        Goal pendingGoal2 = repository.save(Goal.pending("Second pending"));
        
        // WHEN fetching next goal
        Optional<Goal> nextGoal = repository.fetchNextGoal();
        
        // THEN it should return one of the pending goals
        assertThat(nextGoal).isPresent();
        assertThat(nextGoal.get().status()).isEqualTo(GoalStatus.PENDING);
        assertThat(nextGoal.get().id()).isIn(pendingGoal1.id(), pendingGoal2.id());
    }
    
    @Test
    void fetchNextGoal_shouldReturnEmptyWhenNoPendingGoals() {
        // GIVEN goals that are not pending
        repository.save(Goal.pending("Test").markCompleted("Done"));
        repository.save(Goal.pending("Test").markFailed("Error"));
        
        // WHEN fetching next goal
        Optional<Goal> nextGoal = repository.fetchNextGoal();
        
        // THEN it should return empty
        assertThat(nextGoal).isEmpty();
    }
    
    @Test
    void findByStatus_shouldReturnGoalsWithMatchingStatus() {
        // GIVEN goals with different statuses
        Goal pending1 = repository.save(Goal.pending("Pending 1"));
        Goal pending2 = repository.save(Goal.pending("Pending 2"));
        Goal completed = repository.save(Goal.pending("Completed").markCompleted("Done"));
        
        // WHEN finding by PENDING status
        List<Goal> pendingGoals = repository.findByStatus(GoalStatus.PENDING);
        
        // THEN it should return only pending goals
        assertThat(pendingGoals).hasSize(2);
        assertThat(pendingGoals).extracting(Goal::id).containsExactlyInAnyOrder(pending1.id(), pending2.id());
    }
    
    @Test
    void updateGoalStatus_shouldUpdateExistingGoal() {
        // GIVEN a saved goal
        Goal originalGoal = repository.save(Goal.pending("Test goal"));
        Goal updatedGoal = originalGoal.markInProgress();
        
        // WHEN updating the goal status
        Goal result = repository.updateGoalStatus(updatedGoal);
        
        // THEN it should be updated in the repository
        assertThat(result.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        
        Optional<Goal> foundGoal = repository.findById(originalGoal.id());
        assertThat(foundGoal).isPresent();
        assertThat(foundGoal.get().status()).isEqualTo(GoalStatus.IN_PROGRESS);
    }
    
    @Test
    void updateGoalStatus_shouldThrowExceptionWhenIdIsNull() {
        // GIVEN a goal without ID
        Goal goalWithoutId = Goal.pending("Test goal");
        
        // WHEN trying to update status
        // THEN it should throw an exception
        assertThatThrownBy(() -> repository.updateGoalStatus(goalWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("goal ID is null");
    }
    
    @Test
    void clear_shouldRemoveAllGoals() {
        // GIVEN some saved goals
        repository.save(Goal.pending("Goal 1"));
        repository.save(Goal.pending("Goal 2"));
        assertThat(repository.findAll()).hasSize(2);
        
        // WHEN clearing the repository
        repository.clear();
        
        // THEN it should be empty
        assertThat(repository.findAll()).isEmpty();
        assertThat(repository.fetchNextGoal()).isEmpty();
    }
}