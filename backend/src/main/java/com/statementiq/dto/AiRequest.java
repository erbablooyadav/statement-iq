package com.statementiq.dto;

import com.statementiq.model.AiProvider;

/**
 * Provider-agnostic AI request. All providers map to this format.
 */
public record AiRequest(
        /** System prompt — sets the AI's behavior/role */
        String systemPrompt,
        /** User message — the actual query/input */
        String userMessage,
        /** Which provider to route to */
        AiProvider provider,
        /** API key to use (BYOK or platform key). Null = use platform default */
        String apiKey,
        /** Model override (null = use provider default) */
        String model,
        /** For LOCAL provider — the endpoint URL */
        String localUrl,
        /** Max tokens for response */
        int maxTokens
) {
    public AiRequest(String systemPrompt, String userMessage, AiProvider provider, String apiKey) {
        this(systemPrompt, userMessage, provider, apiKey, null, null, 4096);
    }
}
