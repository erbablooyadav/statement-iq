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
 * OpenAI provider.
 * Uses Chat Completions API: https://platform.openai.com/docs/api-reference/chat
 */
@Service
public class OpenAiProviderService implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProviderService.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api-key:}")
    private String platformApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String apiKey = (request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey() : platformApiKey;
        String model = (request.model() != null && !request.model().isBlank())
                ? request.model() : defaultModel;

        if (apiKey == null || apiKey.isBlank()) {
            return AiResponse.error("No OpenAI API key configured. Add your key in Settings → AI Provider.", AiProvider.OPENAI);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", request.maxTokens(),
                    "temperature", 0.3,
                    "messages", List.of(
                            Map.of("role", "system", "content", request.systemPrompt() != null ? request.systemPrompt() : ""),
                            Map.of("role", "user", "content", request.userMessage())
                    )
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) return AiResponse.error("Empty response from OpenAI", AiProvider.OPENAI);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            String text = "";
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                text = message != null ? (String) message.get("content") : "";
            }

            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            int inputTokens = usage != null ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
            int outputTokens = usage != null ? ((Number) usage.get("completion_tokens")).intValue() : 0;

            return AiResponse.ok(text, AiProvider.OPENAI, model, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            return AiResponse.error("OpenAI API error: " + e.getMessage(), AiProvider.OPENAI);
        }
    }

    @Override
    public AiResponse testConnection(String apiKey, String localUrl, String model) {
        AiRequest testReq = new AiRequest(
                "You are a test assistant.", "Reply with exactly: Connection successful!",
                AiProvider.OPENAI, apiKey, model != null ? model : defaultModel, null, 50
        );
        return complete(testReq);
    }
}
