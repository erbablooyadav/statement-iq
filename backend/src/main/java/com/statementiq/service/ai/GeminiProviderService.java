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
 * Google Gemini AI provider.
 * Uses Generative Language API: https://ai.google.dev/gemini-api/docs
 */
@Service
public class GeminiProviderService implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(GeminiProviderService.class);
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    @Value("${gemini.api-key:}")
    private String platformApiKey;

    @Value("${gemini.model:gemini-3.1-pro-preview}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String apiKey = (request.apiKey() != null && !request.apiKey().isBlank())
                ? request.apiKey() : platformApiKey;
        String model = (request.model() != null && !request.model().isBlank())
                ? request.model() : defaultModel;

        if (apiKey == null || apiKey.isBlank()) {
            return AiResponse.error("No Gemini API key configured. Add your key in Settings → AI Provider.", AiProvider.GEMINI);
        }

        try {
            String url = String.format(API_URL, model);

            // Build prompt — Gemini uses a single text content with system instruction
            String fullPrompt = request.userMessage();
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                fullPrompt = request.systemPrompt() + "\n\n" + request.userMessage();
            }

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", fullPrompt)))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", request.maxTokens(),
                            "temperature", 0.3
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey.trim());

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) return AiResponse.error("Empty response from Gemini", AiProvider.GEMINI);

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return AiResponse.error("No response candidates from Gemini", AiProvider.GEMINI);
            }

            String finishReason = (String) candidates.get(0).get("finishReason");
            
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            StringBuilder textBuilder = new StringBuilder();
            if (content != null && content.get("parts") != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                for (Map<String, Object> part : parts) {
                    if (part.get("text") != null) {
                        textBuilder.append((String) part.get("text"));
                    }
                }
            }
            String text = textBuilder.toString();
            
            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("Gemini AI reached max output tokens. Output may be truncated.");
            } else if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                log.warn("Gemini AI stopped due to {}", finishReason);
                return AiResponse.error("Blocked by AI safety filters. Please try another provider.", AiProvider.GEMINI);
            }

            // Gemini returns usage metadata
            Map<String, Object> usageMeta = (Map<String, Object>) responseBody.get("usageMetadata");
            int inputTokens = usageMeta != null && usageMeta.get("promptTokenCount") != null
                    ? ((Number) usageMeta.get("promptTokenCount")).intValue() : 0;
            int outputTokens = usageMeta != null && usageMeta.get("candidatesTokenCount") != null
                    ? ((Number) usageMeta.get("candidatesTokenCount")).intValue() : 0;

            return AiResponse.ok(text, AiProvider.GEMINI, model, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return AiResponse.error("Gemini API error: " + e.getMessage(), AiProvider.GEMINI);
        }
    }

    @Override
    public AiResponse testConnection(String apiKey, String localUrl, String model) {
        AiRequest testReq = new AiRequest(
                "You are a test assistant.", "Reply with exactly: Connection successful!",
                AiProvider.GEMINI, apiKey, model != null ? model : defaultModel, null, 50
        );
        return complete(testReq);
    }
}
