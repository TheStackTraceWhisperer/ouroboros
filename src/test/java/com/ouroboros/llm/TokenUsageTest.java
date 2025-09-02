package com.ouroboros.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUsageTest {

    @Test
    void of_shouldCalculateTotalTokens() {
        // WHEN creating token usage
        TokenUsage usage = TokenUsage.of(50, 100);
        
        // THEN total should be calculated correctly
        assertThat(usage.promptTokens()).isEqualTo(50);
        assertThat(usage.completionTokens()).isEqualTo(100);
        assertThat(usage.totalTokens()).isEqualTo(150);
    }
    
    @Test
    void constructor_shouldAcceptAllFields() {
        // WHEN creating with all fields
        TokenUsage usage = new TokenUsage(30, 70, 100);
        
        // THEN all fields should be set
        assertThat(usage.promptTokens()).isEqualTo(30);
        assertThat(usage.completionTokens()).isEqualTo(70);
        assertThat(usage.totalTokens()).isEqualTo(100);
    }
    
    @Test
    void record_shouldSupportEquality() {
        // GIVEN two identical token usages
        TokenUsage usage1 = new TokenUsage(10, 20, 30);
        TokenUsage usage2 = new TokenUsage(10, 20, 30);
        
        // THEN they should be equal
        assertThat(usage1).isEqualTo(usage2);
        assertThat(usage1.hashCode()).isEqualTo(usage2.hashCode());
    }
}