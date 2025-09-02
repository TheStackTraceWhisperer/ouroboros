package com.ouroboros.messaging;

import com.ouroboros.model.Task;
import com.ouroboros.service.TaskProcessorService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskListener {

    public static final String QUEUE_NAME = "task.processing.queue";

    private final TaskProcessorService taskProcessorService;

    @Autowired
    public TaskListener(TaskProcessorService taskProcessorService) {
        this.taskProcessorService = taskProcessorService;
    }

    @RabbitListener(queues = QUEUE_NAME)
    public void receiveTask(Task task) {
        taskProcessorService.processTask(task);
    }
}