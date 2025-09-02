package com.ouroboros.service;

import com.ouroboros.llm.LLMClient;
import com.ouroboros.llm.LLMClientFactory;
import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Self-enhancing agent service that polls for issues and processes them.
 * This is the main kernel of the agent system.
 */
@Service
@EnableAsync
public class AgentService {
    
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    
    private final IssueRepository issueRepository;
    private final LLMClientFactory llmClientFactory;
    private final SelfPublishService selfPublishService;
    
    @Value("${agent.poll.interval:10000}")
    private long pollIntervalMs;
    
    @Value("${agent.max.retries:3}")
    private int maxRetries;
    
    @Autowired
    public AgentService(
            IssueRepository issueRepository, 
            LLMClientFactory llmClientFactory,
            SelfPublishService selfPublishService) {
        this.issueRepository = issueRepository;
        this.llmClientFactory = llmClientFactory;
        this.selfPublishService = selfPublishService;
    }
    
    /**
     * Main agent loop that polls for pending issues and processes them.
     * This method is scheduled to run periodically.
     */
    @Scheduled(fixedDelayString = "${agent.poll.interval:10000}")
    @Async
    public void pollAndProcessIssues() {
        log.debug("Polling for pending issues...");
        
        Optional<Issue> pendingIssue = getNextPendingIssue();
        if (pendingIssue.isPresent()) {
            processIssue(pendingIssue.get());
        } else {
            log.debug("No pending issues found");
        }
    }
    
    /**
     * Get the next pending issue from the repository.
     */
    private Optional<Issue> getNextPendingIssue() {
        return issueRepository.findByStatus(IssueStatus.PENDING)
                .stream()
                .findFirst();
    }
    
    /**
     * Processes a single issue through the complete lifecycle.
     * 
     * @param issue the issue to process
     */
    public void processIssue(Issue issue) {
        log.info("Starting to process issue {}: {}", issue.getId(), issue.getDescription());
        
        try {
            // 1. Mark issue as IN_PROGRESS
            issue.setStatus(IssueStatus.IN_PROGRESS);
            issueRepository.save(issue);
            
            // 2. Generate code using LLM
            String generatedCode = generateCode(issue.getDescription());
            
            // 3. Perform self-publish action
            boolean publishSuccess = selfPublishService.publish(generatedCode);
            
            // 4. Update issue status based on result
            if (publishSuccess) {
                issue.setStatus(IssueStatus.COMPLETED);
                issueRepository.save(issue);
                log.info("Successfully completed issue {}", issue.getId());
            } else {
                issue.setStatus(IssueStatus.FAILED);
                issueRepository.save(issue);
                log.error("Failed to complete issue {}: self-publish failed", issue.getId());
            }
            
        } catch (Exception e) {
            log.error("Error processing issue {}: {}", issue.getId(), e.getMessage(), e);
            issue.setStatus(IssueStatus.FAILED);
            issueRepository.save(issue);
        }
    }
    
    /**
     * Generates code for the given issue description using the LLM.
     * 
     * @param issueDescription the description of what to generate
     * @return the generated code
     * @throws RuntimeException if code generation fails
     */
    private String generateCode(String issueDescription) {
        log.info("Generating code for issue: {}", issueDescription);
        
        LLMClient defaultClient = llmClientFactory.getDefaultClient();
        String prompt = "Generate code for the following issue: " + issueDescription;
        LLMRequest request = LLMRequest.of(prompt, defaultClient.getSupportedModelId());
        
        LLMResponse response = defaultClient.generate(request);
        
        if (response.isSuccess()) {
            log.info("Code generation successful. Tokens used: {}", response.tokenUsage().totalTokens());
            return response.content();
        } else {
            throw new RuntimeException("Code generation failed: " + response.error());
        }
    }
    
    /**
     * Manually trigger issue processing (useful for testing).
     */
    public void triggerIssueProcessing() {
        log.info("Manually triggering issue processing");
        pollAndProcessIssues();
    }
}