package com.statementiq.service.ai;

import com.statementiq.dto.AiRequest;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Local model provider — supports Ollama, LM Studio, and any OpenAI-compatible endpoint.
 * The user provides their own URL (e.g., http://localhost:11434 for Ollama).
 * 
 * Ollama uses /api/chat (native) or /v1/chat/completions (OpenAI-compatible).
 * LM Studio uses /v1/chat/completions (OpenAI-compatible).
 * We use the OpenAI-compatible format for maximum compatibility.
 */
@Service
public class LocalProviderService implements AiProviderService {

    private static final Logger log = LoggerFactory.getLogger(LocalProviderService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiProvider getProvider() {
        return AiProvider.LOCAL;
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String localUrl = request.localUrl();
        if (localUrl == null || localUrl.isBlank()) {
            return AiResponse.error("No local model URL configured. Set your Ollama/LM Studio URL in Settings → AI Provider.", AiProvider.LOCAL);
        }

        String model = (request.model() != null && !request.model().isBlank())
                ? request.model() : "llama3";

        try {
            // Normalize URL and determine which API format to use
            localUrl = localUrl.replaceAll("/+$", ""); // Strip trailing slashes

            String apiUrl;
            Map<String, Object> body;

            if (localUrl.contains(":11434") || localUrl.contains("ollama")) {
                // Ollama native API
                apiUrl = localUrl + "/api/chat";
                body = Map.of(
                        "model", model,
                        "stream", false,
                        "messages", List.of(
                                Map.of("role", "system", "content", request.systemPrompt() != null ? request.systemPrompt() : ""),
                                Map.of("role", "user", "content", request.userMessage())
                        )
                );
            } else {
                // OpenAI-compatible (LM Studio, etc.)
                apiUrl = localUrl + "/v1/chat/completions";
                body = Map.of(
                        "model", model,
                        "max_tokens", request.maxTokens(),
                        "stream", false,
                        "messages", List.of(
                                Map.of("role", "system", "content", request.systemPrompt() != null ? request.systemPrompt() : ""),
                                Map.of("role", "user", "content", request.userMessage())
                        )
                );
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (request.apiKey() != null && !request.apiKey().isBlank()) {
                String key = request.apiKey().trim();
                headers.setBearerAuth(key);
                log.info("Sending LOCAL provider request to {} with Authorization Bearer token length={}, starts with={}", 
                         apiUrl, key.length(), key.length() > 4 ? key.substring(0, 4) : "****");
            } else {
                log.info("Sending LOCAL provider request to {} WITHOUT Authorization header", apiUrl);
            }

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) return AiResponse.error("Empty response from local model", AiProvider.LOCAL);

            String text;
            if (responseBody.containsKey("message")) {
                // Ollama native format
                Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                text = message != null ? (String) message.get("content") : "";
            } else if (responseBody.containsKey("choices")) {
                // OpenAI-compatible format
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                Map<String, Object> message = choices != null && !choices.isEmpty()
                        ? (Map<String, Object>) choices.get(0).get("message") : null;
                text = message != null ? (String) message.get("content") : "";
            } else {
                text = responseBody.toString();
            }

            return AiResponse.ok(text, AiProvider.LOCAL, model, 0, 0);
        } catch (Exception e) {
            log.error("Local model call failed to {}", localUrl, e);
            return AiResponse.error("Local model error: " + e.getMessage() +
                    ". Make sure your model server is running at: " + localUrl, AiProvider.LOCAL);
        }
    }

    @Override
    public AiResponse testConnection(String apiKey, String localUrl, String model) {
        AiRequest testReq = new AiRequest(
                "You are a test assistant.", "Reply with exactly: Connection successful!",
                AiProvider.LOCAL, apiKey, model != null && !model.isBlank() ? model : "llama3", localUrl, 50
        );
        return complete(testReq);
    }
}
