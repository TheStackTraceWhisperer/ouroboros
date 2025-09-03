# GitHub Integration Configuration Guide

## Overview
The GitHub Integration Service provides seamless synchronization between the Ouroboros agent system and GitHub, enabling human oversight and collaboration through familiar GitHub interfaces.

## Key Features

### Issue Synchronization
- **Automatic Issue Creation**: Internal task proposals become GitHub Issues automatically
- **Real-time Status Updates**: Status changes appear as GitHub issue comments
- **Automatic Issue Closure**: Completed/failed tasks close their corresponding GitHub issues
- **Label Management**: Issues are tagged with relevant status and category labels

### Project Management
- **GitHub Projects Integration**: Large features can be organized into GitHub Projects
- **Issue-to-Project Linking**: Automatic assignment of issues to appropriate projects
- **Project Status Tracking**: Real-time updates for project item statuses
- **Workflow Support**: Integration with GitHub's project workflow features

## Configuration

### Required Properties
Add these properties to your `application.properties` file:

```properties
# GitHub Integration Configuration
github.integration.enabled=true
github.integration.token=${OUROBOROS_GITHUB_PAT:}
github.integration.repository.owner=${GITHUB_REPOSITORY_OWNER:TheStackTraceWhisperer}
github.integration.repository.name=${GITHUB_REPOSITORY_NAME:ouroboros}
github.integration.sync.interval=60000
```

### GitHub Personal Access Token Setup
1. Go to GitHub Settings → Developer Settings → Personal Access Tokens → Tokens (classic)
2. Generate a new token with these permissions:
   - `repo` (Full control of private repositories)
   - `public_repo` (Access to public repositories)
   - `project` (Full control of projects) - **New for Projects support**
3. Copy the token and set it as the `OUROBOROS_GITHUB_PAT` environment variable or GitHub Actions secret

### Repository Configuration
- `github.integration.repository.owner`: GitHub username or organization (uses `GITHUB_REPOSITORY_OWNER` env var or defaults to `TheStackTraceWhisperer`)
- `github.integration.repository.name`: Repository name for issue creation (uses `GITHUB_REPOSITORY_NAME` env var or defaults to `ouroboros`)
- `github.integration.sync.interval`: Synchronization interval in milliseconds (default: 60000 = 1 minute)

### GitHub Actions Integration
For GitHub Actions environments, the integration automatically uses the `OUROBOROS_GITHUB_PAT` secret:

1. Go to repository Settings → Secrets and variables → Actions
2. Add repository secret named `OUROBOROS_GITHUB_PAT`
3. Set value to your GitHub Personal Access Token
4. The application will automatically use this token in CI/CD environments

## Synchronization Flow

### 1. Task Proposal → GitHub Issue
- Agent creates internal issue with status `PENDING`
- Integration service creates GitHub Issue:
  - Title: "🤖 Agent Task: [issue description]" 
  - Body: Detailed task information and metadata
  - Status: Open
  - Labels: Applied based on task type and priority

### 2. Status Changes → Issue Comments
- Status changes to `IN_PROGRESS`: Comment "🤖 Status Update: Task moved to IN_PROGRESS"
- Progress updates: Regular comments with processing details
- Error states: Comments with error information and retry status

### 3. Task Completion → Issue Resolution
- Status `COMPLETED`: Summary comment with results + `status:completed` label + issue closure
- Status `FAILED`: Error summary comment + `status:failed` label + issue closure

## API Endpoints

### Issue Management (Standard)
```bash
# Create issue (triggers GitHub sync)
POST /api/issues
{
  "description": "Implement OAuth2 authentication",
  "createdBy": "agent-system"
}

# Update status (triggers GitHub comment)
PUT /api/issues/{id}/status
{
  "status": "IN_PROGRESS"
}
```

### GitHub Projects (Advanced)
```bash
# Create feature project
POST /api/github/projects
{
  "featureName": "User Authentication System",
  "description": "OAuth2 implementation with social login support"
}

# Add issue to project  
POST /api/github/projects/{projectId}/issues/{issueId}

# Update project item status
PUT /api/github/projects/{projectId}/items/{itemId}/status
{
  "status": "In Progress"
}
```

### Webhook Integration
```bash
# GitHub webhook endpoint (configured in repository settings)
POST /webhook/github
# Handles: issue updates, project changes, pull request events
```

## Example Workflow

### Basic Issue Tracking

1. **Agent creates issue:**
```bash
curl -X POST http://localhost:8080/api/issues \
  -H "Content-Type: application/json" \
  -d '{"description": "Fix payment processing bug", "createdBy": "agent"}'
```

2. **GitHub Issue created automatically** (within 1 minute sync cycle)

3. **Agent processes and updates status:**
```bash
curl -X PUT http://localhost:8080/api/issues/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

4. **GitHub Issue updated with comment** (within 1 minute)

5. **Task completion:**
```bash
curl -X PUT http://localhost:8080/api/issues/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

6. **GitHub Issue closed with summary** (within 1 minute)

### Advanced Project Management

1. **Create feature project for complex tasks**
2. **Issues automatically assigned to relevant projects**
3. **Project boards track progress across multiple related issues**
4. **Webhook events provide real-time updates back to the agent system**

## Database Schema

The `goal_proposals` table stores issue data:
- `id`: UUID primary key
- `description`: Task description (max 1000 chars)
- `status`: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `github_issue_id`: Linked GitHub Issue number (null if not synced)
- `created_at`, `updated_at`: Timestamp tracking
- `created_by`: Agent or system identifier

## Monitoring and Troubleshooting

### Monitoring
Check application logs for:
- `GitHubIntegrationService`: Sync activity and status
- `GitHubApiClientImpl`: API interactions and rate limiting
- Warnings for misconfigurations or API unavailability

### Common Issues

**GitHub API Not Available**
- Verify token permissions (repo, public_repo, project scopes)
- Check repository owner/name configuration
- Ensure token has access to target repository

**Sync Delays** 
- Confirm `github.integration.enabled=true`
- Check sync interval configuration
- Review GitHubIntegrationService logs for errors

**Rate Limiting**
- GitHub API: 5000 requests/hour for authenticated users
- Service automatically respects rate limits
- Consider increasing sync interval if hitting limits frequently

**Webhook Issues**
- Verify webhook URL is accessible from GitHub
- Check webhook secret configuration
- Review webhook delivery logs in GitHub repository settings