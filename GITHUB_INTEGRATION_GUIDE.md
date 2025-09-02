# GitHub Integration Service Configuration

## Overview
The GitHub Integration Service provides centralized observability and control for agent tasks by synchronizing goal proposals with GitHub Issues. This allows human oversight through a familiar GitHub interface.

## Configuration

### Required Properties
Add these properties to your `application.properties` file:

```properties
# GitHub Integration Configuration
github.integration.enabled=true
github.integration.token=ghp_your_github_personal_access_token_here
github.integration.repository.owner=your-github-username
github.integration.repository.name=your-repo-name
github.integration.sync.interval=60000
```

### GitHub Personal Access Token Setup
1. Go to GitHub Settings → Developer Settings → Personal Access Tokens → Tokens (classic)
2. Generate a new token with these permissions:
   - `repo` (Full control of private repositories)
   - `public_repo` (Access to public repositories) 
3. Copy the token and set it in the `github.integration.token` property

### Repository Configuration
- `github.integration.repository.owner`: Your GitHub username or organization name
- `github.integration.repository.name`: The repository name where issues will be created
- `github.integration.sync.interval`: Sync interval in milliseconds (default: 60000 = 1 minute)

## How It Works

### 1. Goal Proposal Creation → GitHub Issue
- When a new goal proposal is created with status `PENDING`
- The service creates a GitHub Issue with:
  - Title: "🤖 Agent Task: [proposal description]"
  - Body: Detailed information about the proposal
  - Status: Open

### 2. Status Changes → Issue Comments
- When proposal status changes to `IN_PROGRESS`
- The service adds a comment: "🤖 Status Update: Task moved to IN_PROGRESS"

### 3. Task Completion → Issue Closure
- When proposal status changes to `COMPLETED` or `FAILED`
- The service:
  - Adds a final summary comment
  - Adds a label (`status:completed` or `status:failed`)
  - Closes the GitHub Issue

## API Endpoints

### Create Goal Proposal
```bash
POST /api/goal-proposals
Content-Type: application/json

{
  "description": "Implement feature X to improve user experience",
  "createdBy": "product-insight-agent"
}
```

### Get All Proposals
```bash
GET /api/goal-proposals
```

### Get Proposal by ID
```bash
GET /api/goal-proposals/{id}
```

### Update Proposal Status
```bash
PUT /api/goal-proposals/{id}/status
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

### Get Proposals by Status
```bash
GET /api/goal-proposals/status/PENDING
GET /api/goal-proposals/status/IN_PROGRESS
GET /api/goal-proposals/status/COMPLETED
GET /api/goal-proposals/status/FAILED
```

## Example Workflow

1. **Agent creates proposal:**
```bash
curl -X POST http://localhost:8080/api/goal-proposals \
  -H "Content-Type: application/json" \
  -d '{"description": "Fix critical bug in payment system", "createdBy": "self-enhancing-agent"}'
```

2. **GitHub Issue is created automatically** (within 1 minute)

3. **Agent updates status:**
```bash
curl -X PUT http://localhost:8080/api/goal-proposals/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

4. **Comment is added to GitHub Issue** (within 1 minute)

5. **Agent completes task:**
```bash
curl -X PUT http://localhost:8080/api/goal-proposals/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

6. **GitHub Issue is closed with summary** (within 1 minute)

## Database Schema

The `goal_proposals` table is automatically created with:
- `id`: UUID primary key
- `description`: Task description (max 1000 chars)
- `status`: PENDING, IN_PROGRESS, COMPLETED, FAILED
- `github_issue_id`: Link to GitHub Issue number
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp
- `created_by`: Agent or system that created the proposal

## Monitoring

Check application logs for:
- `GitHubIntegrationService`: Sync activity and errors
- `GitHubApiClientImpl`: GitHub API interactions
- Warnings when GitHub API is not available or misconfigured

## Troubleshooting

### GitHub API Not Available
- Verify token is correct and has required permissions
- Check repository owner/name configuration
- Ensure repository exists and token has access

### Sync Not Working
- Check `github.integration.enabled=true`
- Verify sync interval configuration
- Look for error logs in GitHubIntegrationService

### Rate Limiting
- GitHub API has rate limits (5000 requests/hour for authenticated users)
- The service respects these limits automatically
- Consider increasing sync interval if needed