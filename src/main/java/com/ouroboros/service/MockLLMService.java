package com.ouroboros.service;

import com.ouroboros.model.LLMRequest;
import com.ouroboros.model.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test") // This service will only be active when the "test" Spring profile is enabled
public class MockLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(MockLLMService.class);

    @Override
    public LLMResponse generate(LLMRequest request) {
        log.info("MockLLMService generating response for model '{}' with prompt: '{}'", request.modelId(), request.prompt());
        String generatedText = "This is a mock response to the prompt: '" + request.prompt() + "'";
        return new LLMResponse(request.modelId(), generatedText);
    }

    @Override
    public String getProvider() {
        return "mock";
    }
}