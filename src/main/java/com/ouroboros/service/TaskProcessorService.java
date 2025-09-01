package com.ouroboros.service;

import com.ouroboros.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TaskProcessorService {

    private static final Logger log = LoggerFactory.getLogger(TaskProcessorService.class);

    public void processTask(Task task) {
        log.info("Starting processing for task ID: {}", task.id());
        try {
            // Simulate work based on task complexity
            TimeUnit.MILLISECONDS.sleep(task.complexity() * 100L);
            log.info("Finished processing for task ID: {}", task.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing was interrupted for task ID: {}", task.id(), e);
        }
    }
}