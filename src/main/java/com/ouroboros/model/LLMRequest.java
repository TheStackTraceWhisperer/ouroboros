package com.ouroboros.model;

import java.util.Map;

public record LLMRequest(
    String modelId,
    String prompt,
    Map<String, Object> parameters
) {}