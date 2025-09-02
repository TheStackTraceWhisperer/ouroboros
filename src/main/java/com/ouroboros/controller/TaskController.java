package com.ouroboros.controller;

import com.ouroboros.model.Task;
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

    @PostMapping
    public ResponseEntity<Task> submitTask(@RequestBody Task taskRequest) {
        Task newTask = new Task(UUID.randomUUID(), taskRequest.description(), taskRequest.complexity());
        // TODO: Implement task processing without RabbitMQ
        return new ResponseEntity<>(newTask, HttpStatus.ACCEPTED);
    }
}