# GitHub Integration for Agent Orchestration

This document describes Phase 1 implementation of the GitHub Integration Service for migrating agent orchestration from internal database queuing to GitHub Issues and Projects.

## Overview

The GitHubIntegrationService provides a complete workflow management system using GitHub as the single source of truth for all agent work. It replaces the internal goal_proposals table with GitHub Issues and Project boards.

## Configuration

Add the following configuration to `application.properties`:

```properties
# GitHub Integration Configuration
github.token=your_github_token_here
github.organization=TheStackTraceWhisperer
github.repository=ouroboros
```

**Note**: Without a valid GitHub token, the service operates in anonymous mode with limited functionality due to GitHub API rate limiting.

## Features Implemented

### 1. Epic Creation
- Creates GitHub Project boards with Kanban columns (Backlog, To Do, In Progress, Done)
- Creates master epic issues with proper labeling
- Links epic issues to project boards

### 2. Backlog Population
- Accepts lists of sub-tasks and creates corresponding GitHub Issues
- Automatically adds issues to the "Backlog" column
- Maintains proper issue labeling and organization

### 3. Work Fetching
- Queries GitHub Project "To Do" columns to find available work
- Returns highest-priority issues ready for processing
- Supports multiple concurrent projects

### 4. Kanban Automation
- Moves issues between project board columns
- Supports standard workflow transitions
- Provides convenience methods for common state changes

## API Endpoints

### Create Epic
```bash
POST /api/github/epics
Content-Type: application/json

{
  "title": "Feature: User Authentication System",
  "description": "Implement comprehensive user authentication with OAuth2 support"
}
```

### Populate Backlog
```bash
POST /api/github/projects/Feature:%20User%20Authentication%20System%20-%20Project%20Board/backlog
Content-Type: application/json

{
  "subTasks": [
    "Create user model and database schema",
    "Implement OAuth2 integration",
    "Add user registration endpoint",
    "Create login/logout functionality",
    "Add password reset feature"
  ]
}
```

### Fetch Next Task
```bash
GET /api/github/tasks/next
```

### Move Issue to Column
```bash
POST /api/github/projects/{projectName}/issues/{issueNumber}/move
Content-Type: application/json

{
  "column": "In Progress"
}
```

### Mark Issue as In Progress
```bash
POST /api/github/projects/{projectName}/issues/{issueNumber}/start
```

### Mark Issue as Completed
```bash
POST /api/github/projects/{projectName}/issues/{issueNumber}/complete
```

## Example Workflow

1. **Create an Epic**: Start by creating a new epic which sets up a project board
2. **Populate Backlog**: Add sub-tasks to the project's backlog
3. **Move to To Do**: Manually move ready tasks from Backlog to To Do column
4. **Fetch Work**: Agents call `/tasks/next` to get their next assignment
5. **Update Status**: As agents work, they update issue status via the API
6. **Complete Work**: Mark issues as done when finished

## Integration with Agents

Future phases will integrate this service with:
- **Product Insight Agent**: Will create epics based on user feedback analysis
- **Self-Enhancing Agent**: Will fetch tasks and report progress automatically

## Error Handling

The service includes comprehensive error handling for:
- GitHub API rate limiting
- Network connectivity issues
- Invalid project/issue references
- Authentication failures

## Testing

Run the test suite to validate functionality:

```bash
mvn test
```

Tests include:
- Unit tests for controller validation
- Integration tests for GitHub API interaction
- Mock-based testing for error scenarios

## Limitations

- Anonymous GitHub API access has strict rate limits
- Creating issues and projects requires authenticated access
- Some GitHub API features may require specific permissions

## Future Enhancements

Phase 2 will include:
- Direct agent integration with this service
- Automated progress reporting
- Issue commenting for detailed status updates
- Advanced project board management features