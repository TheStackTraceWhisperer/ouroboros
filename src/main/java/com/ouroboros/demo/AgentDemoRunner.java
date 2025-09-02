package com.ouroboros.demo;

import com.ouroboros.model.Issue;
import com.ouroboros.model.IssueStatus;
import com.ouroboros.repository.IssueRepository;
import com.ouroboros.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Demo runner that demonstrates the agent's issue processing capabilities.
 * This will run automatically when the application starts if the demo profile is active.
 */
@Component
public class AgentDemoRunner implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(AgentDemoRunner.class);
    
    private final IssueRepository issueRepository;
    private final AgentService agentService;
    
    public AgentDemoRunner(IssueRepository issueRepository, AgentService agentService) {
        this.issueRepository = issueRepository;
        this.agentService = agentService;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("demo")) {
            log.info("🚀 Starting Agent Demo...");
            runDemo();
        }
    }
    
    private void runDemo() throws InterruptedException {
        // Create some sample issues
        log.info("📝 Creating sample issues...");
        
        Issue issue1 = issueRepository.save(new Issue("Create a REST API endpoint for user management", "demo-runner"));
        Issue issue2 = issueRepository.save(new Issue("Implement data validation for input forms", "demo-runner"));
        Issue issue3 = issueRepository.save(new Issue("Generate unit tests for service layer", "demo-runner"));
        
        log.info("✅ Created {} issues:", issueRepository.findByStatus(IssueStatus.PENDING).size());
        issueRepository.findByStatus(IssueStatus.PENDING).forEach(issue -> 
            log.info("   - Issue {}: {}", issue.getId(), issue.getDescription()));
        
        // Demonstrate manual issue processing
        log.info("\n🔧 Demonstrating manual issue processing...");
        agentService.processIssue(issue1);
        
        // Wait for status to update
        Thread.sleep(1000);
        
        Issue processedIssue = issueRepository.findById(issue1.getId()).orElseThrow();
        log.info("✅ Issue {} status: {}", 
                processedIssue.getId(), 
                processedIssue.getStatus());
        
        // Show automatic polling will pick up remaining issues
        log.info("\n⏰ Remaining issues will be processed automatically by the scheduled agent polling...");
        log.info("📊 Current status summary:");
        for (IssueStatus status : IssueStatus.values()) {
            int count = issueRepository.findByStatus(status).size();
            if (count > 0) {
                log.info("   - {}: {} issues", status, count);
            }
        }
        
        log.info("\n🎉 Demo complete! The agent will continue processing remaining issues in the background.");
        log.info("💡 Check the logs to see automatic issue processing happen every 10 seconds.");
    }
}