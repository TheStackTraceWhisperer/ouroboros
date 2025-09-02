package com.ouroboros.service;

import com.ouroboros.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskProcessingIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testTaskSubmission() {
        // GIVEN a task to be submitted
        Task requestTask = new Task(null, "Process financial report", 5);

        // WHEN the task is submitted to the API endpoint
        ResponseEntity<Task> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/tasks",
                requestTask,
                Task.class
        );

        // THEN the API should accept the task and return it with a generated ID
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Task acceptedTask = response.getBody();
        assertThat(acceptedTask).isNotNull();
        assertThat(acceptedTask.id()).isNotNull();
        assertThat(acceptedTask.description()).isEqualTo("Process financial report");
        assertThat(acceptedTask.complexity()).isEqualTo(5);
    }
}