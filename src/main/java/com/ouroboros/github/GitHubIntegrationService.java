package com.ouroboros.github;

import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for integrating with GitHub Issues and Projects as the primary work dispatcher.
 * This service replaces the internal database queue with GitHub as the single source of truth.
 */
@Service
public class GitHubIntegrationService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubIntegrationService.class);
    
    // Standard Kanban column names
    private static final String BACKLOG_COLUMN = "Backlog";
    private static final String TODO_COLUMN = "To Do";
    private static final String IN_PROGRESS_COLUMN = "In Progress";
    private static final String DONE_COLUMN = "Done";
    
    private final GitHub github;
    private final String repositoryName;
    private final String organizationName;
    
    public GitHubIntegrationService(
            @Value("${github.token:#{null}}") String githubToken,
            @Value("${github.repository}") String repositoryName,
            @Value("${github.organization}") String organizationName) throws IOException {
        
        this.repositoryName = repositoryName;
        this.organizationName = organizationName;
        
        if (githubToken != null && !githubToken.isEmpty()) {
            this.github = new GitHubBuilder().withOAuthToken(githubToken).build();
            log.info("GitHub integration initialized with OAuth token");
        } else {
            this.github = GitHub.connectAnonymously();
            log.warn("GitHub integration initialized anonymously - limited functionality");
        }
    }
    
    /**
     * Creates a new GitHub Project board with Kanban columns and a master epic issue.
     * 
     * @param epicTitle the title for the epic
     * @param epicDescription the description for the epic
     * @return the created epic issue
     * @throws IOException if GitHub API operation fails
     */
    public GHIssue createEpic(String epicTitle, String epicDescription) throws IOException {
        log.info("Creating epic: {}", epicTitle);
        
        GHRepository repo = github.getRepository(organizationName + "/" + repositoryName);
        
        // Create the epic issue
        GHIssueBuilder issueBuilder = repo.createIssue(epicTitle)
                .body(epicDescription)
                .label("epic");
        
        GHIssue epicIssue = issueBuilder.create();
        log.info("Created epic issue #{}: {}", epicIssue.getNumber(), epicTitle);
        
        // Create project board for this epic
        try {
            GHProject project = repo.createProject(epicTitle + " - Project Board", 
                    "Project board for tracking epic: " + epicTitle);
            
            // Create Kanban columns
            project.createColumn(BACKLOG_COLUMN);
            project.createColumn(TODO_COLUMN);  
            project.createColumn(IN_PROGRESS_COLUMN);
            project.createColumn(DONE_COLUMN);
            
            log.info("Created project board '{}' with Kanban columns", project.getName());
            
            // Add epic issue to the project board in the Backlog column
            GHProjectColumn backlogColumn = getProjectColumn(project, BACKLOG_COLUMN);
            if (backlogColumn != null) {
                backlogColumn.createCard(epicIssue);
                log.info("Added epic issue to Backlog column");
            }
            
        } catch (IOException e) {
            log.error("Failed to create project board for epic", e);
            // Don't fail the entire operation if project creation fails
        }
        
        return epicIssue;
    }
    
    /**
     * Creates GitHub Issues for a list of sub-tasks and adds them to the Backlog column.
     * 
     * @param projectName the name of the project board
     * @param subTasks list of sub-task descriptions
     * @return list of created issues
     * @throws IOException if GitHub API operation fails
     */
    public List<GHIssue> populateBacklog(String projectName, List<String> subTasks) throws IOException {
        log.info("Populating backlog for project '{}' with {} sub-tasks", projectName, subTasks.size());
        
        GHRepository repo = github.getRepository(organizationName + "/" + repositoryName);
        GHProject project = findProjectByName(repo, projectName);
        
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectName);
        }
        
        GHProjectColumn backlogColumn = getProjectColumn(project, BACKLOG_COLUMN);
        if (backlogColumn == null) {
            throw new IllegalStateException("Backlog column not found in project: " + projectName);
        }
        
        return subTasks.stream().map(taskDescription -> {
            try {
                GHIssue issue = repo.createIssue(taskDescription)
                        .label("task")
                        .create();
                
                backlogColumn.createCard(issue);
                log.debug("Created sub-task issue #{}: {}", issue.getNumber(), taskDescription);
                return issue;
                
            } catch (IOException e) {
                log.error("Failed to create sub-task: {}", taskDescription, e);
                throw new RuntimeException("Failed to create sub-task", e);
            }
        }).toList();
    }
    
    /**
     * Fetches the next available task from the "To Do" column of any project.
     * 
     * @return the highest-priority issue ready for work, or null if none available
     */
    public GHIssue fetchNextAvailableTask() {
        log.debug("Fetching next available task from To Do columns");
        
        try {
            GHRepository repo = github.getRepository(organizationName + "/" + repositoryName);
            
            // Get all projects in the repository
            for (GHProject project : repo.listProjects().toList()) {
                GHProjectColumn todoColumn = getProjectColumn(project, TODO_COLUMN);
                if (todoColumn != null) {
                    
                    // Get cards from the To Do column
                    List<GHProjectCard> cards = todoColumn.listCards().toList();
                    if (!cards.isEmpty()) {
                        // Return the first card's associated issue (highest priority)
                        GHProjectCard card = cards.get(0);
                        if (card.getContentUrl() != null) {
                            String contentUrl = card.getContentUrl().toString();
                            if (contentUrl.contains("/issues/")) {
                                String issueNumber = contentUrl.substring(
                                        contentUrl.lastIndexOf("/") + 1);
                                GHIssue issue = repo.getIssue(Integer.parseInt(issueNumber));
                                log.info("Found next available task: Issue #{}", issue.getNumber());
                                return issue;
                            }
                        }
                    }
                }
            }
            
            log.debug("No tasks available in To Do columns");
            return null;
            
        } catch (IOException e) {
            log.error("Failed to fetch next available task", e);
            return null;
        }
    }
    
    /**
     * Moves an issue between columns on a project board.
     * 
     * @param issueNumber the issue number to move
     * @param projectName the project board name
     * @param targetColumn the target column name (e.g., "In Progress", "Done")
     * @throws IOException if GitHub API operation fails
     */
    public void moveIssueToColumn(int issueNumber, String projectName, String targetColumn) throws IOException {
        log.info("Moving issue #{} to column '{}' in project '{}'", issueNumber, targetColumn, projectName);
        
        GHRepository repo = github.getRepository(organizationName + "/" + repositoryName);
        GHProject project = findProjectByName(repo, projectName);
        
        if (project == null) {
            throw new IllegalArgumentException("Project not found: " + projectName);
        }
        
        GHProjectColumn target = getProjectColumn(project, targetColumn);
        if (target == null) {
            throw new IllegalArgumentException("Column not found: " + targetColumn);
        }
        
        // Find the card for this issue across all columns
        GHProjectCard cardToMove = findCardForIssue(project, issueNumber);
        if (cardToMove != null) {
            // Delete the old card and create a new one in the target column
            cardToMove.delete();
            GHIssue issue = repo.getIssue(issueNumber);
            target.createCard(issue);
            log.info("Successfully moved issue #{} to '{}' column", issueNumber, targetColumn);
        } else {
            // If card doesn't exist, create it in the target column
            GHIssue issue = repo.getIssue(issueNumber);
            target.createCard(issue);
            log.info("Created new card for issue #{} in '{}' column", issueNumber, targetColumn);
        }
    }
    
    /**
     * Convenience method to move issue to "In Progress" column.
     */
    public void markInProgress(int issueNumber, String projectName) throws IOException {
        moveIssueToColumn(issueNumber, projectName, IN_PROGRESS_COLUMN);
    }
    
    /**
     * Convenience method to move issue to "Done" column.
     */
    public void markCompleted(int issueNumber, String projectName) throws IOException {
        moveIssueToColumn(issueNumber, projectName, DONE_COLUMN);
    }
    
    // Helper methods
    
    private GHProject findProjectByName(GHRepository repo, String projectName) throws IOException {
        for (GHProject project : repo.listProjects().toList()) {
            if (project.getName().equals(projectName)) {
                return project;
            }
        }
        return null;
    }
    
    private GHProjectColumn getProjectColumn(GHProject project, String columnName) {
        try {
            for (GHProjectColumn column : project.listColumns().toList()) {
                if (column.getName().equals(columnName)) {
                    return column;
                }
            }
        } catch (IOException e) {
            log.error("Failed to list columns for project", e);
        }
        return null;
    }
    
    private GHProjectCard findCardForIssue(GHProject project, int issueNumber) {
        try {
            for (GHProjectColumn column : project.listColumns().toList()) {
                for (GHProjectCard card : column.listCards().toList()) {
                    if (card.getContentUrl() != null) {
                        String contentUrl = card.getContentUrl().toString();
                        if (contentUrl.endsWith("/issues/" + issueNumber)) {
                            return card;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to find card for issue #{}", issueNumber, e);
        }
        return null;
    }
}