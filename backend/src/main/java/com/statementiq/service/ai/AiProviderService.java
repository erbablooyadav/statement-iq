package com.statementiq.service.ai;

import com.statementiq.dto.AiRequest;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.AiProvider;

/**
 * Provider-agnostic AI service interface.
 * Each AI provider (Claude, Gemini, OpenAI, Local) implements this.
 */
public interface AiProviderService {

    /**
     * Send a completion request to the AI provider.
     */
    AiResponse complete(AiRequest request);

    /**
     * Which provider this service handles.
     */
    AiProvider getProvider();

    /**
     * Test connectivity with the given API key/URL.
     * Returns a simple greeting response to verify the key works.
     */
    AiResponse testConnection(String apiKey, String localUrl, String model);
}
