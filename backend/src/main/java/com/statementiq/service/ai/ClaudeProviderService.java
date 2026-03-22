package com.statementiq.service.ai;

import com.statementiq.dto.AiRequest;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Claude (Anthropic) AI provider.
 * Uses Anthropic Messages API: https://docs.anthropic.com/en/api/messages
 */
@Service
public class ClaudeProviderService implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProviderService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    @Value("${claude.api-key:}")
    private String platformApiKey;

    @Value("${claude.model:claude-sonnet-4-20250514}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiProvider getProvider() {
        return AiProvider.CLAUDE;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String apiKey = (request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey() : platformApiKey;
        String model = (request.model() != null && !request.model().isBlank())
                ? request.model() : defaultModel;

        if (apiKey == null || apiKey.isBlank()) {
            return AiResponse.error("No Claude API key configured. Add your key in Settings → AI Provider.", AiProvider.CLAUDE);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", request.maxTokens(),
                    "system", request.systemPrompt() != null ? request.systemPrompt() : "",
                    "messages", List.of(Map.of("role", "user", "content", request.userMessage()))
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) return AiResponse.error("Empty response from Claude", AiProvider.CLAUDE);

            List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
            String text = content != null && !content.isEmpty() ? (String) content.get(0).get("text") : "";

            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            int inputTokens = usage != null ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int outputTokens = usage != null ? ((Number) usage.get("output_tokens")).intValue() : 0;

            return AiResponse.ok(text, AiProvider.CLAUDE, model, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("Claude API call failed", e);
            return AiResponse.error("Claude API error: " + e.getMessage(), AiProvider.CLAUDE);
        }
    }

    @Override
    public AiResponse testConnection(String apiKey, String localUrl, String model) {
        AiRequest testReq = new AiRequest(
                "You are a test assistant.", "Reply with exactly: Connection successful!",
                AiProvider.CLAUDE, apiKey, model != null ? model : defaultModel, null, 50
        );
        return complete(testReq);
    }
}
