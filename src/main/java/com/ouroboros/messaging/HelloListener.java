package com.ouroboros.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class HelloListener {

    private static final Logger log = LoggerFactory.getLogger(HelloListener.class);

    public static final String QUEUE_NAME = "hello.queue";

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveMessage(String message) {
        log.info("Received message: '{}'", message);
    }
}