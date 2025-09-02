package com.ouroboros.service;

import com.ouroboros.llm.LLMClient;
import com.ouroboros.llm.LLMClientFactory;
import com.ouroboros.llm.LLMRequest;
import com.ouroboros.llm.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CodeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationService.class);
    private final LLMClientFactory llmClientFactory;
    // You will need to inject the GitHubApiClient here later to create branches/PRs

    @Autowired
    public CodeGenerationService(LLMClientFactory llmClientFactory) {
        this.llmClientFactory = llmClientFactory;
    }

    /**
     * Attempts to generate code for a specific, actionable sub-task.
     * @param subTaskDescription The detailed description of the sub-task.
     * @param issueId The ID of the parent issue for context.
     */
    public void generateCodeForSubTask(String subTaskDescription, Long issueId) {
        log.info("Attempting to generate code for sub-task of issue #{}: {}", issueId, subTaskDescription);

        try {
            String generatedCode = generateCode(subTaskDescription);
            log.info("Successfully generated code for sub-task of issue #{}. Code length: {}", issueId, generatedCode.length());

            // TODO: Implement the Git workflow:
            // 1. Create a new branch (e.g., "feature/issue-123-subtask-1")
            // 2. Apply the generated code to the relevant files.
            // 3. Commit the changes.
            // 4. Push the branch to the remote repository.
            // 5. Create a Pull Request back to the main branch, linking it to the issue.
            log.warn("Git workflow not yet implemented. Skipping pull request creation.");

        } catch (Exception e) {
            log.error("Failed to generate code for sub-task of issue #{}", issueId, e);
        }
    }

    private String generateCode(String subTaskDescription) {
        LLMClient client = llmClientFactory.getDefaultClient();
        String prompt = String.format(
            "You are an expert Java and Spring Boot developer. Based on the current project structure, generate the complete code needed to implement the following task. Only output the raw code, without any explanation or markdown formatting. Task: %s",
            subTaskDescription
        );
        LLMRequest request = LLMRequest.of(prompt, client.getSupportedModelId());
        LLMResponse response = client.generate(request);

        if (response.isSuccess()) {
            return response.content();
        } else {
            throw new RuntimeException("Code generation failed: " + response.error());
        }
    }
}