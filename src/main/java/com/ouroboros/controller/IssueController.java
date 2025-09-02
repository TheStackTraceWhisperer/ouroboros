package com.ouroboros.controller;

import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing issues.
 * Provides endpoints for creating and updating issues that will be synced to GitHub.
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {
    
    private final IssueRepository issueRepository;
    
    @Autowired
    public IssueController(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }
    
    /**
     * Create a new issue.
     */
    @PostMapping
    public ResponseEntity<Issue> createIssue(@RequestBody CreateIssueRequest request) {
        Issue issue = new Issue(request.description(), request.createdBy());
        Issue saved = issueRepository.save(issue);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Get all issues.
     */
    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues() {
        List<Issue> issues = issueRepository.findAll();
        return ResponseEntity.ok(issues);
    }
    
    /**
     * Get a specific issue by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssue(@PathVariable UUID id) {
        return issueRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update the status of an issue.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Issue> updateStatus(@PathVariable UUID id, 
                                             @RequestBody UpdateStatusRequest request) {
        return issueRepository.findById(id)
                .map(issue -> {
                    issue.setStatus(request.status());
                    Issue saved = issueRepository.save(issue);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get issues by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Issue>> getIssuesByStatus(@PathVariable IssueStatus status) {
        List<Issue> issues = issueRepository.findByStatus(status);
        return ResponseEntity.ok(issues);
    }
    
    /**
     * Request object for creating a new issue.
     */
    public record CreateIssueRequest(String description, String createdBy) {}
    
    /**
     * Request object for updating issue status.
     */
    public record UpdateStatusRequest(IssueStatus status) {}
}