# Agent 1: Self-Enhancing Agent - Usage Guide

This document describes how to use the newly implemented self-enhancing agent functionality.

## Overview

The agent system provides:
- **Goal Management**: Create, track, and process goals through their lifecycle
- **Automated Processing**: Scheduled polling and processing of pending goals
- **LLM Integration**: Code generation using configurable LLM providers
- **Self-Publishing**: Mock implementation of publishing generated code

## Quick Start

### 1. Environment Configuration

Create a `.env` file based on `.env.example`:

```bash
cp .env.example .env
# Edit .env with your API keys
```

Example `.env` configuration:
```properties
LLM_OPENAI_API_KEY=sk-your-openai-key-here
LLM_GOOGLE_API_KEY=your-google-ai-key-here
LLM_ANTHROPIC_API_KEY=your-anthropic-key-here
```

### 2. Running the Application

Start the application normally:
```bash
mvn spring-boot:run
```

Or run with demo mode to see the agent in action:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--demo"
```

### 3. Demo Mode

The demo mode will:
1. Create 3 sample goals
2. Process one goal manually to show the flow
3. Leave remaining goals for automatic processing
4. Display status updates

## API Usage

### Goal Repository

The `GoalRepository` provides methods to manage goals:

```java
@Autowired
private GoalRepository goalRepository;

// Create a new goal
Goal goal = goalRepository.save(Goal.pending("Create user authentication system"));

// Fetch next pending goal
Optional<Goal> nextGoal = goalRepository.fetchNextGoal();

// Update goal status
Goal updatedGoal = goal.markInProgress();
goalRepository.updateGoalStatus(updatedGoal);

// Find goals by status
List<Goal> pendingGoals = goalRepository.findByStatus(GoalStatus.PENDING);
```

### Agent Service

The `AgentService` handles goal processing:

```java
@Autowired
private AgentService agentService;

// Manually process a specific goal
agentService.processGoal(goal);

// Trigger manual polling (normally happens automatically)
agentService.triggerGoalProcessing();
```

## Goal Lifecycle

Goals progress through these states:

1. **PENDING** - Goal created and waiting for processing
2. **IN_PROGRESS** - Goal picked up by agent and being processed
3. **COMPLETED** - Goal successfully processed and code published
4. **FAILED** - Goal processing failed with error message

## Configuration Options

The agent behavior can be configured via application properties or environment variables:

```properties
# Polling interval (milliseconds)
agent.poll.interval=10000

# Maximum retry attempts
agent.max.retries=3

# LLM configuration
llm.default.model-id=gpt-4
llm.openai.api-key=${LLM_OPENAI_API_KEY:}
llm.google.api-key=${LLM_GOOGLE_API_KEY:}
llm.anthropic.api-key=${LLM_ANTHROPIC_API_KEY:}
```

## Testing

### Unit Tests
Run individual component tests:
```bash
# Test specific LLM clients
mvn test -Dtest=OpenAIClientTest
mvn test -Dtest=GoogleAIClientTest
mvn test -Dtest=AnthropicAIClientTest

# Test repository functionality
mvn test -Dtest=InMemoryGoalRepositoryTest
```

### Integration Tests
Run end-to-end tests:
```bash
# Test complete goal processing flow
mvn test -Dtest=AgentIntegrationTest
```

### All Tests
Run the complete test suite:
```bash
mvn test
```

## Monitoring

### Logs
The agent logs its activities:
- Goal creation and status changes
- LLM interactions and token usage
- Self-publish actions
- Polling cycles and errors

### Health Check
Check application health (including LLM client availability):
```bash
curl http://localhost:8080/actuator/health
```

## Extending the System

### Custom Goal Repository
Replace the in-memory repository with a database implementation:

```java
@Repository
public class DatabaseGoalRepository implements GoalRepository {
    // Implement using JPA, MongoDB, etc.
}
```

### Custom Self-Publishing
Implement real publishing logic:

```java
@Service
public class RealSelfPublishService extends SelfPublishService {
    @Override
    public boolean publish(String generatedCode) {
        // Implement actual code publishing:
        // - Save to files
        // - Commit to Git
        // - Deploy to servers
        // - Send notifications
        return true;
    }
}
```

### Additional LLM Providers
Add new LLM clients by implementing the `LLMClient` interface:

```java
@Service
public class CustomLLMClient implements LLMClient {
    // Implement custom LLM integration
}
```

## Architecture

The system follows a clean architecture pattern:

- **Model**: `Goal`, `GoalStatus` - Core domain objects
- **Repository**: `GoalRepository` - Data access abstraction
- **Service**: `AgentService`, `SelfPublishService` - Business logic
- **LLM**: `LLMClient`, `LLMClientFactory` - External AI integration
- **Config**: `EnvironmentConfig` - Configuration management

All components are Spring-managed beans with dependency injection for easy testing and extension.