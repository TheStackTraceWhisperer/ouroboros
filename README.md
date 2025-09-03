# Ouroboros - Self-Enhancing Agent System

A sophisticated Spring Boot 3.5.x agent system that processes tasks autonomously using Large Language Models (LLMs) and integrates with GitHub for issue tracking and collaboration.

## Overview

Ouroboros is a self-enhancing agent system that:
- **Autonomously processes tasks/issues** using configurable LLM providers (OpenAI, Google AI, Anthropic)
- **Integrates with GitHub** for issue tracking, status synchronization, and project management
- **Generates and publishes code** through mock self-publishing capabilities
- **Provides observability** through REST APIs and logging

## Key Features

### Agent System
- **Automatic Issue Processing**: Polls for pending issues and processes them using LLMs
- **Multi-LLM Support**: Configurable support for OpenAI, Google AI, and Anthropic models
- **Self-Publishing**: Mock implementation for code generation and publishing
- **Configurable Polling**: Adjustable intervals and retry logic

### GitHub Integration
- **Issue Synchronization**: Automatically creates GitHub issues from internal task proposals
- **Status Tracking**: Real-time status updates reflected in GitHub issue comments
- **Project Management**: Support for GitHub Projects to organize large features
- **Webhook Support**: Handles GitHub webhook events

### Technical Stack
- **Java 21** with modern language features
- **Spring Boot 3.5.x** with WebMVC and JPA
- **H2 Database** for development (easily replaceable with production databases)
- **Maven** build system with comprehensive testing
- **Docker** support with Alpine-based images

## API Endpoints

### Core Application
- `GET /` - Application status message
- `GET /logs` - Recent application logs

### Issue Management
- `POST /api/issues` - Create a new issue for agent processing
- `GET /api/issues` - List all issues
- `GET /api/issues/{id}` - Get specific issue details
- `PUT /api/issues/{id}/status` - Update issue status
- `GET /api/issues/status/{status}` - Get issues by status (PENDING, IN_PROGRESS, COMPLETED, FAILED)

### Task Management  
- `POST /api/tasks` - Submit a new task for processing

### GitHub Integration
- `POST /api/github/projects` - Create a GitHub project for feature organization
- `POST /api/github/projects/{projectId}/issues/{issueId}` - Add issue to project
- `PUT /api/github/projects/{projectId}/items/{itemId}/status` - Update project item status
- `POST /webhook/github` - GitHub webhook endpoint for event handling

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- (Optional) GitHub Personal Access Token for GitHub integration

### Basic Setup

1. **Clone and build:**
```bash
git clone <repository-url>
cd ouroboros
mvn clean compile
```

2. **Run tests:**
```bash
mvn test
```

3. **Start the application:**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Agent Configuration

Create a `.env` file for LLM provider configuration:

```bash
cp .env.example .env
# Edit .env with your API keys
```

Example configuration:
```properties
LLM_OPENAI_API_KEY=sk-your-openai-key-here
LLM_GOOGLE_API_KEY=your-google-ai-key-here
LLM_ANTHROPIC_API_KEY=your-anthropic-key-here
```

### GitHub Integration Setup

For GitHub integration, configure these properties:

```properties
github.integration.enabled=true
github.integration.token=${OUROBOROS_GITHUB_PAT:}
github.integration.repository.owner=${GITHUB_REPOSITORY_OWNER:}
github.integration.repository.name=${GITHUB_REPOSITORY_NAME:}
```

See [GitHub Integration Guide](GITHUB_INTEGRATION_GUIDE.md) for detailed setup instructions.

## Docker Support

### Production Docker Image

Multi-stage Dockerfile using Alpine Java images for building and running with Java 21:

```bash
# Build the Docker image
docker build -t ouroboros .

# Run the container
docker run -p 8080:8080 ouroboros
```

### Development Docker Image

For development purposes, use the development Dockerfile with Alpine + Java 21 + Maven:

```bash
# Build the development image
docker build -f Dockerfile.dev -t ouroboros-dev .

# Run development container with volume mounting
docker run -it --rm -p 8080:8080 -v $(pwd):/home/developer/workspace ouroboros-dev

# Inside the container, you can run:
mvn clean compile
mvn test
mvn spring-boot:run
```

## CI/CD

The repository includes GitHub Actions workflow that:
- Builds and tests the application
- Creates Docker images  
- Publishes to GitHub Container Registry (GHCR)

Images are published to: `ghcr.io/thestacktracewhisperer/ouroboros`

## Development Commands

```bash
# Run tests
mvn test

# Build application
mvn clean package

# Run application
mvn spring-boot:run

# Run with demo mode (creates sample issues)
mvn spring-boot:run -Dspring-boot.run.arguments="--demo"
```

## Architecture Overview

### Core Components

- **Agent Service**: Main polling and processing engine
- **Issue Repository**: Data access layer for issue management  
- **LLM Integration**: Pluggable LLM client factory with multiple providers
- **GitHub Integration**: Bidirectional sync with GitHub issues and projects
- **Self-Publishing Service**: Mock implementation for code deployment

### Database Schema

Issues are stored in the `goal_proposals` table with:
- `id`: UUID primary key
- `description`: Task description
- `status`: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `github_issue_id`: Link to GitHub Issue (if synced)
- `created_at`, `updated_at`: Timestamps
- `created_by`: Agent or system identifier

## Documentation

- **[Agent Usage Guide](AGENT_USAGE_GUIDE.md)** - Detailed guide for using the agent system
- **[GitHub Integration Guide](GITHUB_INTEGRATION_GUIDE.md)** - Setup and configuration for GitHub features

## Contributing

1. Ensure Java 21 is installed and configured
2. Run `mvn clean compile` to verify setup  
3. Run `mvn test` to execute the test suite
4. Make changes and test thoroughly
5. Submit pull requests with clear descriptions

## License

This project is part of the Ouroboros system for autonomous software development.