package com.statementiq.controller;

import com.statementiq.dto.AiResponse;
import com.statementiq.dto.ApiResponse;
import com.statementiq.model.AiProvider;
import com.statementiq.model.User;
import com.statementiq.service.ai.AiProviderRouter;
import com.statementiq.service.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Settings endpoints — manage BYOK keys, provider preferences, and test connections.
 */
@RestController
@RequestMapping("/api/v1/settings/ai")
public class AiSettingsController {

    private final UserService userService;
    private final AiProviderRouter aiRouter;

    public AiSettingsController(UserService userService, AiProviderRouter aiRouter) {
        this.userService = userService;
        this.aiRouter = aiRouter;
    }

    /**
     * GET /settings/ai — Get current AI configuration.
     * Returns preferred provider, which providers have keys configured (masked), and provider info.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiSettings(Authentication auth) {
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());

        Map<String, Object> settings = new HashMap<>();
        settings.put("preferredProvider", user.getPreferredAiProvider());
        settings.put("localModelUrl", user.getLocalModelUrl());
        settings.put("localModelName", user.getLocalModelName());

        // Show which providers have keys (masked)
        Map<String, Object> configuredKeys = new HashMap<>();
        if (user.getAiApiKeys() != null) {
            for (Map.Entry<String, String> entry : user.getAiApiKeys().entrySet()) {
                String key = entry.getValue();
                String masked = key.length() > 8
                        ? key.substring(0, 4) + "****" + key.substring(key.length() - 4)
                        : "****";
                configuredKeys.put(entry.getKey(), masked);
            }
        }
        settings.put("configuredKeys", configuredKeys);

        Map<String, Object> configuredModels = new HashMap<>();
        if (user.getAiModelOverrides() != null) {
            configuredModels.putAll(user.getAiModelOverrides());
        }
        settings.put("configuredModels", configuredModels);

        // Provider info with setup guides
        settings.put("providers", getProviderGuides());

        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    /**
     * PUT /settings/ai/provider — Set preferred AI provider.
     */
    @PutMapping("/provider")
    public ResponseEntity<ApiResponse<String>> setPreferredProvider(
            @RequestBody Map<String, String> body, Authentication auth) {

        String providerStr = body.get("provider");
        AiProvider provider;
        try {
            provider = AiProvider.valueOf(providerStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER", "Invalid provider. Choose: CLAUDE, GEMINI, OPENAI, or LOCAL"));
        }

        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        user.setPreferredAiProvider(provider);
        userService.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Provider set to " + provider.getDisplayName()));
    }

    /**
     * PUT /settings/ai/key — Save API key for a provider.
     */
    @PutMapping("/key")
    public ResponseEntity<ApiResponse<String>> saveApiKey(
            @RequestBody Map<String, String> body, Authentication auth) {

        String providerStr = body.get("provider");
        String apiKey = body.get("apiKey");
        String modelOverride = body.get("modelOverride");

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("MISSING_KEY", "API key is required"));
        }

        AiProvider provider;
        try {
            provider = AiProvider.valueOf(providerStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER", "Invalid provider"));
        }

        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        if (user.getAiApiKeys() == null) user.setAiApiKeys(new HashMap<>());
        user.getAiApiKeys().put(provider.name(), apiKey);

        if (modelOverride != null && !modelOverride.trim().isEmpty()) {
            if (user.getAiModelOverrides() == null) user.setAiModelOverrides(new HashMap<>());
            user.getAiModelOverrides().put(provider.name(), modelOverride.trim());
        } else if (user.getAiModelOverrides() != null) {
            user.getAiModelOverrides().remove(provider.name());
        }

        userService.save(user);

        return ResponseEntity.ok(ApiResponse.ok("API key saved for " + provider.getDisplayName()));
    }

    /**
     * DELETE /settings/ai/key/{provider} — Remove API key for a provider.
     */
    @DeleteMapping("/key/{provider}")
    public ResponseEntity<ApiResponse<String>> deleteApiKey(
            @PathVariable String provider, Authentication auth) {

        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        if (user.getAiApiKeys() != null) {
            user.getAiApiKeys().remove(provider.toUpperCase());
            userService.save(user);
        }

        return ResponseEntity.ok(ApiResponse.ok("API key removed"));
    }

    /**
     * PUT /settings/ai/local-url — Set local model endpoint URL.
     */
    @PutMapping("/local-url")
    public ResponseEntity<ApiResponse<String>> setLocalUrl(
            @RequestBody Map<String, String> body, Authentication auth) {

        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
        user.setLocalModelUrl(body.get("url"));
        user.setLocalModelName(body.getOrDefault("model", "llama3"));
        
        if (body.containsKey("apiKey")) {
            String apiKey = body.get("apiKey");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                if (user.getAiApiKeys() == null) user.setAiApiKeys(new HashMap<>());
                user.getAiApiKeys().put("LOCAL", apiKey.trim());
            }
        }
        
        userService.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Local model URL saved"));
    }

    /**
     * POST /settings/ai/test — Test a provider connection.
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<AiResponse>> testProvider(
            @RequestBody Map<String, String> body, Authentication auth) {

        String providerStr = body.get("provider");
        String apiKey = body.get("apiKey");
        String localUrl = body.get("localUrl");
        String model = body.get("model");
        User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());

        AiProvider provider;
        try {
            provider = AiProvider.valueOf(providerStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PROVIDER", "Invalid provider"));
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            if (user.getAiApiKeys() != null) {
                apiKey = user.getAiApiKeys().get(provider.name());
            }
        }

        // Pass modelOverride if provider is not local. Alternatively we can rely on AiProviderRouter.
        AiResponse result = aiRouter.testProvider(provider, apiKey, localUrl, model);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Setup guides for each provider.
     */
    private List<Map<String, String>> getProviderGuides() {
        return List.of(
                Map.of(
                        "provider", "CLAUDE",
                        "name", "Claude (Anthropic)",
                        "icon", "🟣",
                        "description", "Most advanced reasoning. Best for financial analysis.",
                        "guide", "1. Go to console.anthropic.com\n2. Click API Keys → Create Key\n3. Copy the key (starts with sk-ant-)\n4. Paste it below",
                        "link", "https://console.anthropic.com/settings/keys",
                        "keyPrefix", "sk-ant-"
                ),
                Map.of(
                        "provider", "GEMINI",
                        "name", "Gemini (Google)",
                        "icon", "🔵",
                        "description", "Free tier available (15 RPM). Great for getting started.",
                        "guide", "1. Go to aistudio.google.com/apikey\n2. Click Create API Key\n3. Select your Google Cloud project\n4. Copy the key (starts with AIza)\n5. Paste it below",
                        "link", "https://aistudio.google.com/apikey",
                        "keyPrefix", "AIza"
                ),
                Map.of(
                        "provider", "OPENAI",
                        "name", "OpenAI",
                        "icon", "🟢",
                        "description", "GPT-4o Mini is very affordable. Good all-round choice.",
                        "guide", "1. Go to platform.openai.com/api-keys\n2. Click Create new secret key\n3. Copy the key (starts with sk-)\n4. Paste it below",
                        "link", "https://platform.openai.com/api-keys",
                        "keyPrefix", "sk-"
                ),
                Map.of(
                        "provider", "LOCAL",
                        "name", "Local Model",
                        "icon", "🖥️",
                        "description", "100% private. Runs on your computer. No data leaves your machine.",
                        "guide", "For Ollama:\n1. Install: ollama.com/download\n2. Run: ollama serve\n3. Pull a model: ollama pull llama3\n4. Enter URL: http://localhost:11434\n\nFor LM Studio:\n1. Download from lmstudio.ai\n2. Load a model\n3. Start server\n4. Enter URL: http://localhost:1234",
                        "link", "https://ollama.com/download",
                        "keyPrefix", ""
                )
        );
    }
}
