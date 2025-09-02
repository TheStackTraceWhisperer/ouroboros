package com.ouroboros.controller;

import com.ouroboros.github.GitHubIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GitHubController.
 */
@WebMvcTest(GitHubController.class)
class GitHubControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GitHubIntegrationService githubService;
    
    @Test
    void fetchNextTask_shouldReturnNoTasksMessage() throws Exception {
        when(githubService.fetchNextAvailableTask()).thenReturn(null);
        
        mockMvc.perform(get("/api/github/tasks/next"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("No tasks available"));
    }
    
    @Test
    void createEpic_shouldReturnBadRequestWhenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/github/epics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Title is required"));
    }
    
    @Test
    void createEpic_shouldReturnBadRequestWhenTitleEmpty() throws Exception {
        mockMvc.perform(post("/api/github/epics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Title is required"));
    }
    
    @Test
    void populateBacklog_shouldReturnBadRequestWhenSubTasksMissing() throws Exception {
        mockMvc.perform(post("/api/github/projects/test-project/backlog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("subTasks list is required"));
    }
    
    @Test
    void moveIssue_shouldReturnBadRequestWhenColumnMissing() throws Exception {
        mockMvc.perform(post("/api/github/projects/test-project/issues/123/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("column is required"));
    }
}