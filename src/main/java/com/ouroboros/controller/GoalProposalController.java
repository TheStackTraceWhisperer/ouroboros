package com.ouroboros.controller;

import com.ouroboros.model.GoalProposal;
import com.ouroboros.model.GoalProposalStatus;
import com.ouroboros.repository.GoalProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing goal proposals.
 * Provides endpoints for creating and updating proposals that will be synced to GitHub.
 */
@RestController
@RequestMapping("/api/goal-proposals")
public class GoalProposalController {
    
    private final GoalProposalRepository proposalRepository;
    
    @Autowired
    public GoalProposalController(GoalProposalRepository proposalRepository) {
        this.proposalRepository = proposalRepository;
    }
    
    /**
     * Create a new goal proposal.
     */
    @PostMapping
    public ResponseEntity<GoalProposal> createProposal(@RequestBody CreateProposalRequest request) {
        GoalProposal proposal = new GoalProposal(request.description(), request.createdBy());
        GoalProposal saved = proposalRepository.save(proposal);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Get all goal proposals.
     */
    @GetMapping
    public ResponseEntity<List<GoalProposal>> getAllProposals() {
        List<GoalProposal> proposals = proposalRepository.findAll();
        return ResponseEntity.ok(proposals);
    }
    
    /**
     * Get a specific goal proposal by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GoalProposal> getProposal(@PathVariable UUID id) {
        return proposalRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update the status of a goal proposal.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<GoalProposal> updateStatus(@PathVariable UUID id, 
                                                     @RequestBody UpdateStatusRequest request) {
        return proposalRepository.findById(id)
                .map(proposal -> {
                    proposal.setStatus(request.status());
                    GoalProposal saved = proposalRepository.save(proposal);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get proposals by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<GoalProposal>> getProposalsByStatus(@PathVariable GoalProposalStatus status) {
        List<GoalProposal> proposals = proposalRepository.findByStatus(status);
        return ResponseEntity.ok(proposals);
    }
    
    /**
     * Request object for creating a new proposal.
     */
    public record CreateProposalRequest(String description, String createdBy) {}
    
    /**
     * Request object for updating proposal status.
     */
    public record UpdateStatusRequest(GoalProposalStatus status) {}
}