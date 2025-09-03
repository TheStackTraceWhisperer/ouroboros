# Agent System Usage Guide

This document provides a comprehensive guide for using the Ouroboros self-enhancing agent system.

## Overview

The agent system provides:
- **Issue Management**: Create, track, and process issues through their lifecycle
- **Automated Processing**: Scheduled polling and processing of pending issues
- **LLM Integration**: Code generation using configurable LLM providers
- **Self-Publishing**: Mock implementation of publishing generated code
- **GitHub Integration**: Bidirectional synchronization with GitHub issues and projects

## Architecture

The system follows a clean architecture pattern:

- **Model**: `Issue`, `IssueStatus` - Core domain objects
- **Repository**: `IssueRepository` - Data access abstraction  
- **Service**: `AgentService`, `SelfPublishService` - Business logic
- **LLM**: `LLMClient`, `LLMClientFactory` - External AI integration
- **GitHub**: `GitHubIntegrationService` - GitHub API integration
- **Config**: `EnvironmentConfig` - Configuration management

All components are Spring-managed beans with dependency injection for easy testing and extension.

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
1. Create 3 sample issues
2. Process one issue manually to show the flow
3. Leave remaining issues for automatic processing
4. Display status updates

## API Usage

### Issue Repository

The `IssueRepository` provides methods to manage issues:

```java
@Autowired
private IssueRepository issueRepository;

// Create a new issue
Issue issue = new Issue();
issue.setDescription("Create user authentication system");
issue.setStatus(IssueStatus.PENDING);
Issue savedIssue = issueRepository.save(issue);

// Find issues by status
List<Issue> pendingIssues = issueRepository.findByStatus(IssueStatus.PENDING);

// Find unsynced issues (not yet synced to GitHub)
List<Issue> unsyncedIssues = issueRepository.findByGithubIssueIdIsNull();
```

### Agent Service

The `AgentService` handles issue processing:

```java
@Autowired
private AgentService agentService;

// Manually trigger issue processing (normally happens automatically)
agentService.triggerIssueProcessing();
```

## Issue Lifecycle

Issues progress through these states:

1. **PENDING** - Issue created and waiting for processing
2. **IN_PROGRESS** - Issue picked up by agent and being processed  
3. **COMPLETED** - Issue successfully processed and code published
4. **FAILED** - Issue processing failed with error message

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
mvn test -Dtest=IssueRepositoryTest
```

### Integration Tests
Run end-to-end tests:
```bash
# Test complete issue processing flow
mvn test -Dtest=AgentIntegrationTest

# Test GitHub integration
mvn test -Dtest=GitHubIntegrationTest
```

### All Tests
Run the complete test suite:
```bash
mvn test
```

## Monitoring

### Logs
The agent logs its activities:
- Issue creation and status changes
- LLM interactions and token usage
- Self-publish actions
- Polling cycles and errors
- GitHub synchronization events

### Application Logs
Access recent logs via REST API:
```bash
curl http://localhost:8080/logs
```

## Extending the System

### Custom Issue Repository
Replace the default repository with a custom database implementation:

```java
@Repository
public class CustomIssueRepository implements IssueRepository {
    // Implement using JPA, MongoDB, etc.
    // All standard JpaRepository methods are inherited
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
    @Override
    public LLMResponse generate(LLMRequest request) {
        // Implement custom LLM integration
    }
    
    @Override
    public String getSupportedModelId() {
        return "custom-model-id";
    }
    
    @Override
    public boolean isAvailable() {
        // Check if client is properly configured
        return true;
    }
}
```

### Custom GitHub Integration
Extend GitHub functionality:

```java
@Service
public class ExtendedGitHubIntegrationService extends GitHubIntegrationService {
    // Add custom GitHub workflows
    // Implement additional project management features
    // Add custom webhook handling
}
```

## Architecture

The system follows a clean architecture pattern:

- **Model**: `Issue`, `IssueStatus` - Core domain objects
- **Repository**: `IssueRepository` - Data access abstraction  
- **Service**: `AgentService`, `SelfPublishService` - Business logic
- **LLM**: `LLMClient`, `LLMClientFactory` - External AI integration
- **GitHub**: `GitHubIntegrationService` - GitHub API integration
- **Config**: `EnvironmentConfig` - Configuration management

All components are Spring-managed beans with dependency injection for easy testing and extension.

## REST API Integration

### Create Issues
```bash
POST /api/issues
Content-Type: application/json

{
  "description": "Implement user authentication system",
  "createdBy": "development-agent"
}
```

### Monitor Issue Status
```bash
# Get all issues
GET /api/issues

# Get pending issues
GET /api/issues/status/PENDING

# Get specific issue
GET /api/issues/{id}

# Update issue status
PUT /api/issues/{id}/status
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

### Task Submission
```bash
POST /api/tasks
Content-Type: application/json

{
  "description": "Optimize database queries",
  "complexity": "MEDIUM"
}
```

For complete GitHub integration features, see the [GitHub Integration Guide](GITHUB_INTEGRATION_GUIDE.md).