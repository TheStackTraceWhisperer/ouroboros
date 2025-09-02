package com.ouroboros.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    // Inject the CodeGenerationService here later

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(@RequestHeader("X-GitHub-Event") String githubEvent,
                                                      @RequestBody String payload) {
        log.info("Received GitHub webhook event: {}", githubEvent);

        // We are only interested in the "issues" event
        if ("issues".equals(githubEvent)) {
            // Simple and insecure parsing for now.
            // A real implementation MUST verify the signature and use a proper JSON parser.
            if (payload.contains("\"action\":\"labeled\"") && payload.contains("\"name\":\"plan-approved\"")) {
                log.info("Detected 'plan-approved' label was added to an issue.");
                // TODO:
                // 1. Parse the payload properly to get the issue ID and body.
                // 2. Parse the issue body to extract the first incomplete checklist item.
                // 3. Call codeGenerationService.generateCodeForSubTask(subTask, issueId);
            }
        }
        return ResponseEntity.ok("Webhook received.");
    }
}