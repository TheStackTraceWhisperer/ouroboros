package com.ouroboros.model;

import java.util.UUID;

public record Task(
    UUID id,
    String description,
    int complexity
) {}