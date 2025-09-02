package com.ouroboros.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LLMResponseTest {

    @Test
    void success_shouldCreateSuccessfulResponse() {
        // GIVEN token usage
        TokenUsage usage = TokenUsage.of(50, 100);
        
        // WHEN creating a successful response
        LLMResponse response = LLMResponse.success("test content", usage, "stop");
        
        // THEN it should be properly configured
        assertThat(response.content()).isEqualTo("test content");
        assertThat(response.tokenUsage()).isEqualTo(usage);
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.error()).isNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isError()).isFalse();
    }
    
    @Test
    void error_shouldCreateErrorResponse() {
        // WHEN creating an error response
        LLMResponse response = LLMResponse.error("API error");
        
        // THEN it should be properly configured
        assertThat(response.content()).isNull();
        assertThat(response.tokenUsage()).isNull();
        assertThat(response.finishReason()).isNull();
        assertThat(response.error()).isEqualTo("API error");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isError()).isTrue();
    }
    
    @Test
    void isSuccess_shouldReturnFalseWhenContentIsNull() {
        // GIVEN a response with null content but no error
        LLMResponse response = new LLMResponse(null, null, "stop", null);
        
        // THEN it should not be considered successful
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.isError()).isFalse();
    }
}