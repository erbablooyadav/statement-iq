package com.statementiq.dto;

import com.statementiq.model.AiProvider;

/**
 * Provider-agnostic AI response.
 */
public record AiResponse(
        /** Whether the AI call succeeded */
        boolean success,
        /** The AI's text response */
        String content,
        /** Which provider actually served this response */
        AiProvider provider,
        /** Model used */
        String model,
        /** Token usage — input */
        int inputTokens,
        /** Token usage — output */
        int outputTokens,
        /** Error message if failed */
        String error
) {
    public static AiResponse ok(String content, AiProvider provider, String model, int inputTokens, int outputTokens) {
        return new AiResponse(true, content, provider, model, inputTokens, outputTokens, null);
    }

    public static AiResponse error(String error, AiProvider provider) {
        return new AiResponse(false, null, provider, null, 0, 0, error);
    }
}
