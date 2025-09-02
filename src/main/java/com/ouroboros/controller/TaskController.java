package com.ouroboros.controller;

import com.ouroboros.messaging.TaskListener;
import com.ouroboros.model.Task;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public TaskController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<Task> submitTask(@RequestBody Task taskRequest) {
        Task newTask = new Task(UUID.randomUUID(), taskRequest.description(), taskRequest.complexity());
        rabbitTemplate.convertAndSend(TaskListener.QUEUE_NAME, newTask);
        return new ResponseEntity<>(newTask, HttpStatus.ACCEPTED);
    }
}