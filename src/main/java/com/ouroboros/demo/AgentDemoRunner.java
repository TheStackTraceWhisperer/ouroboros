package com.ouroboros.demo;

import com.ouroboros.model.Goal;
import com.ouroboros.model.GoalStatus;
import com.ouroboros.repository.GoalRepository;
import com.ouroboros.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Demo runner that demonstrates the agent's goal processing capabilities.
 * This will run automatically when the application starts if the demo profile is active.
 */
@Component
public class AgentDemoRunner implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(AgentDemoRunner.class);
    
    private final GoalRepository goalRepository;
    private final AgentService agentService;
    
    public AgentDemoRunner(GoalRepository goalRepository, AgentService agentService) {
        this.goalRepository = goalRepository;
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
        // Create some sample goals
        log.info("📝 Creating sample goals...");
        
        Goal goal1 = goalRepository.save(Goal.pending("Create a REST API endpoint for user management"));
        Goal goal2 = goalRepository.save(Goal.pending("Implement data validation for input forms"));
        Goal goal3 = goalRepository.save(Goal.pending("Generate unit tests for service layer"));
        
        log.info("✅ Created {} goals:", goalRepository.findByStatus(GoalStatus.PENDING).size());
        goalRepository.findByStatus(GoalStatus.PENDING).forEach(goal -> 
            log.info("   - Goal {}: {}", goal.id(), goal.description()));
        
        // Demonstrate manual goal processing
        log.info("\n🔧 Demonstrating manual goal processing...");
        agentService.processGoal(goal1);
        
        // Wait for status to update
        Thread.sleep(1000);
        
        Goal processedGoal = goalRepository.findById(goal1.id()).orElseThrow();
        log.info("✅ Goal {} status: {} - {}", 
                processedGoal.id(), 
                processedGoal.status(), 
                processedGoal.result());
        
        // Show automatic polling will pick up remaining goals
        log.info("\n⏰ Remaining goals will be processed automatically by the scheduled agent polling...");
        log.info("📊 Current status summary:");
        for (GoalStatus status : GoalStatus.values()) {
            int count = goalRepository.findByStatus(status).size();
            if (count > 0) {
                log.info("   - {}: {} goals", status, count);
            }
        }
        
        log.info("\n🎉 Demo complete! The agent will continue processing remaining goals in the background.");
        log.info("💡 Check the logs to see automatic goal processing happen every 10 seconds.");
    }
}