package com.ouroboros.controller;

import com.ouroboros.github.GitHubIntegrationService;
import org.kohsuke.github.GHIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for GitHub integration operations.
 * Provides endpoints to manage epics, tasks, and project boards.
 */
@RestController
@RequestMapping("/api/github")
public class GitHubController {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubController.class);
    
    private final GitHubIntegrationService githubService;
    
    @Autowired
    public GitHubController(GitHubIntegrationService githubService) {
        this.githubService = githubService;
    }
    
    /**
     * Creates a new epic with project board and Kanban columns.
     */
    @PostMapping("/epics")
    public ResponseEntity<?> createEpic(@RequestBody Map<String, String> request) {
        try {
            String title = request.get("title");
            String description = request.get("description");
            
            if (title == null || title.isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            
            GHIssue epic = githubService.createEpic(title, description);
            
            return ResponseEntity.ok(Map.of(
                "issueNumber", epic.getNumber(),
                "title", epic.getTitle(),
                "url", epic.getHtmlUrl().toString()
            ));
            
        } catch (IOException e) {
            log.error("Failed to create epic", e);
            return ResponseEntity.internalServerError().body("Failed to create epic: " + e.getMessage());
        }
    }
    
    /**
     * Populates backlog with sub-tasks for a given project.
     */
    @PostMapping("/projects/{projectName}/backlog")
    public ResponseEntity<?> populateBacklog(
            @PathVariable String projectName, 
            @RequestBody Map<String, List<String>> request) {
        try {
            List<String> subTasks = request.get("subTasks");
            
            if (subTasks == null || subTasks.isEmpty()) {
                return ResponseEntity.badRequest().body("subTasks list is required");
            }
            
            List<GHIssue> createdIssues = githubService.populateBacklog(projectName, subTasks);
            
            List<Map<String, Object>> result = createdIssues.stream()
                .map(issue -> Map.of(
                    "issueNumber", (Object) issue.getNumber(),
                    "title", issue.getTitle(),
                    "url", issue.getHtmlUrl().toString()
                ))
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "created", result.size(),
                "issues", result
            ));
            
        } catch (IOException e) {
            log.error("Failed to populate backlog for project: {}", projectName, e);
            return ResponseEntity.internalServerError().body("Failed to populate backlog: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Fetches the next available task from To Do columns.
     */
    @GetMapping("/tasks/next")
    public ResponseEntity<?> fetchNextTask() {
        GHIssue nextTask = githubService.fetchNextAvailableTask();
        
        if (nextTask == null) {
            return ResponseEntity.ok(Map.of("message", "No tasks available"));
        }
        
        return ResponseEntity.ok(Map.of(
            "issueNumber", nextTask.getNumber(),
            "title", nextTask.getTitle(),
            "url", nextTask.getHtmlUrl().toString(),
            "body", nextTask.getBody() != null ? nextTask.getBody() : ""
        ));
    }
    
    /**
     * Moves an issue to a different column.
     */
    @PostMapping("/projects/{projectName}/issues/{issueNumber}/move")
    public ResponseEntity<?> moveIssue(
            @PathVariable String projectName,
            @PathVariable int issueNumber,
            @RequestBody Map<String, String> request) {
        try {
            String targetColumn = request.get("column");
            
            if (targetColumn == null || targetColumn.isEmpty()) {
                return ResponseEntity.badRequest().body("column is required");
            }
            
            githubService.moveIssueToColumn(issueNumber, projectName, targetColumn);
            
            return ResponseEntity.ok(Map.of(
                "message", "Issue moved successfully",
                "issueNumber", issueNumber,
                "column", targetColumn
            ));
            
        } catch (IOException e) {
            log.error("Failed to move issue #{} in project {}", issueNumber, projectName, e);
            return ResponseEntity.internalServerError().body("Failed to move issue: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Marks an issue as in progress.
     */
    @PostMapping("/projects/{projectName}/issues/{issueNumber}/start")
    public ResponseEntity<?> startWork(
            @PathVariable String projectName,
            @PathVariable int issueNumber) {
        try {
            githubService.markInProgress(issueNumber, projectName);
            
            return ResponseEntity.ok(Map.of(
                "message", "Issue marked as in progress",
                "issueNumber", issueNumber
            ));
            
        } catch (IOException e) {
            log.error("Failed to mark issue #{} as in progress", issueNumber, e);
            return ResponseEntity.internalServerError().body("Failed to start work: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Marks an issue as completed.
     */
    @PostMapping("/projects/{projectName}/issues/{issueNumber}/complete")
    public ResponseEntity<?> completeWork(
            @PathVariable String projectName,
            @PathVariable int issueNumber) {
        try {
            githubService.markCompleted(issueNumber, projectName);
            
            return ResponseEntity.ok(Map.of(
                "message", "Issue marked as completed",
                "issueNumber", issueNumber
            ));
            
        } catch (IOException e) {
            log.error("Failed to mark issue #{} as completed", issueNumber, e);
            return ResponseEntity.internalServerError().body("Failed to complete work: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}