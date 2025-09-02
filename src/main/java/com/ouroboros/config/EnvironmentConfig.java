package com.ouroboros.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Configuration class that loads environment variables from .env file.
 * This provides support for local development environment configuration.
 */
@Configuration
public class EnvironmentConfig {
    
    private static final Logger log = LoggerFactory.getLogger(EnvironmentConfig.class);
    private static final String ENV_FILE = ".env";
    
    @PostConstruct
    public void loadEnvironment() {
        loadEnvFile();
    }
    
    private void loadEnvFile() {
        Path envPath = Paths.get(ENV_FILE);
        
        if (!Files.exists(envPath)) {
            log.info("No .env file found at {}. Using system environment and application.properties only.", 
                     envPath.toAbsolutePath());
            return;
        }
        
        try (Stream<String> lines = Files.lines(envPath)) {
            lines
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> line.contains("="))
                .forEach(this::setEnvironmentVariable);
            
            log.info("Loaded environment variables from {}", envPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to load .env file: {}", e.getMessage());
        }
    }
    
    private void setEnvironmentVariable(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();
            
            // Only set if not already defined in system environment
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value);
                log.debug("Set environment variable: {}", key);
            } else {
                log.debug("Environment variable {} already defined, skipping", key);
            }
        }
    }
}