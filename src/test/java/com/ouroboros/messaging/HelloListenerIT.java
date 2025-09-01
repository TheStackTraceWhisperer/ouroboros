package com.ouroboros.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class HelloListenerIT {

    @Container
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoSpyBean
    private HelloListener helloListener;

    @Test
    void testMessageIsReceived() {
        // GIVEN a message
        String message = "Hello from Testcontainers!";

        // WHEN the message is sent to the queue
        rabbitTemplate.convertAndSend(HelloListener.QUEUE_NAME, message);

        // THEN the listener's receiveMessage method is called with the correct message
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(helloListener).receiveMessage(message);
        });
    }
}