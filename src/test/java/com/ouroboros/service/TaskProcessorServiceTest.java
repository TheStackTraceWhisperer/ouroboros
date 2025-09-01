package com.ouroboros.service;

import com.ouroboros.model.Task;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

class TaskProcessorServiceTest {

    private final TaskProcessorService taskProcessorService = new TaskProcessorService();

    @Test
    void processTask_shouldRunWithoutErrors() {
        // GIVEN a new task
        Task task = new Task(UUID.randomUUID(), "Test task", 1);

        // WHEN the task is processed
        // THEN no exception should be thrown
        assertThatCode(() -> taskProcessorService.processTask(task))
                .doesNotThrowAnyException();
    }

    @Test
    void processTask_withZeroComplexity_shouldRunWithoutErrors() {
        // GIVEN a task with zero complexity
        Task task = new Task(UUID.randomUUID(), "Zero complexity task", 0);

        // WHEN the task is processed
        // THEN no exception should be thrown
        assertThatCode(() -> taskProcessorService.processTask(task))
                .doesNotThrowAnyException();
    }
}