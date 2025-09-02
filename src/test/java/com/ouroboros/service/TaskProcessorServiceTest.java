package com.ouroboros.service;

import com.ouroboros.llm.LLMClientFactory;
import com.ouroboros.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class TaskProcessorServiceTest {

    @Mock
    private LLMClientFactory llmClientFactory;
    
    private TaskProcessorService taskProcessorService;
    
    @BeforeEach
    void setUp() {
        taskProcessorService = new TaskProcessorService(llmClientFactory);
    }

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