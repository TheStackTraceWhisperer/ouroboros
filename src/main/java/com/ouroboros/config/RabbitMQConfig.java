package com.ouroboros.config;

import com.ouroboros.messaging.HelloListener;
import com.ouroboros.messaging.TaskListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue helloQueue() {
        return new Queue(HelloListener.QUEUE_NAME, false);
    }

    @Bean
    public Queue taskProcessingQueue() {
        return new Queue(TaskListener.QUEUE_NAME, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Configure a message converter to handle sending/receiving of our Task record as JSON
        return new Jackson2JsonMessageConverter();
    }
}