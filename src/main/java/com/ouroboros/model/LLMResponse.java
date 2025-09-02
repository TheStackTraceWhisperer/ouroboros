package com.ouroboros.model;

public record LLMResponse(
    String modelId,
    String generatedText
) {}