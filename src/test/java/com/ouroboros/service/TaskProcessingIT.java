package com.ouroboros.service;

import com.ouroboros.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TaskProcessingIT {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoSpyBean
    private TaskProcessorService taskProcessorService;

    @Test
    void testTaskSubmissionAndProcessing() {
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

        // AND the TaskProcessorService should eventually be called with the same task data
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(taskProcessorService).processTask(acceptedTask);
        });
    }
}