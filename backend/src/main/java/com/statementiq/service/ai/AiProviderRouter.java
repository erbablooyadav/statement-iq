package com.statementiq.service.ai;

import com.statementiq.dto.AiRequest;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.AiProvider;
import com.statementiq.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes AI requests to the correct provider based on user preferences.
 * 
 * Resolution order:
 *   1. Use the user's preferred provider + their BYOK key
 *   2. If no BYOK key, use the platform's key for that provider
 *   3. If no platform key, return an error guiding the user to add a key
 */
@Service
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);
    private final Map<AiProvider, AiProviderService> providers;

    public AiProviderRouter(List<AiProviderService> providerServices) {
        this.providers = providerServices.stream()
                .collect(Collectors.toMap(AiProviderService::getProvider, Function.identity()));
        log.info("AI Provider Router initialized with {} providers: {}",
                providers.size(), providers.keySet());
    }

    /**
     * Route an AI request for a specific user with default max tokens (8192).
     */
    public AiResponse route(String systemPrompt, String userMessage, User user) {
        return route(systemPrompt, userMessage, user, 8192);
    }

    /**
     * Route an AI request for a specific user with custom max tokens.
     * Use higher values (e.g. 16384) for document parsing to avoid output truncation.
     */
    public AiResponse route(String systemPrompt, String userMessage, User user, int maxTokens) {
        AiProvider preferred = user.getPreferredAiProvider();
        if (preferred == null) preferred = AiProvider.CLAUDE; // Default

        AiProviderService service = providers.get(preferred);
        if (service == null) {
            return AiResponse.error("AI provider " + preferred + " is not available.", preferred);
        }

        // Resolve API key: user's BYOK key → platform key (handled by service)
        String apiKey = null;
        String localUrl = null;
        String model = null;

        if (user.getAiApiKeys() != null) {
            apiKey = user.getAiApiKeys().get(preferred.name());
        }
        if (preferred == AiProvider.LOCAL) {
            localUrl = user.getLocalModelUrl();
            model = user.getLocalModelName();
        } else if (user.getAiModelOverrides() != null) {
            model = user.getAiModelOverrides().get(preferred.name());
        }

        AiRequest request = new AiRequest(
                systemPrompt, userMessage, preferred, apiKey, model, localUrl, maxTokens
        );

        log.debug("Routing AI request to {} for user {} (maxTokens={})", preferred, user.getId(), maxTokens);
        return service.complete(request);
    }

    /**
     * Test a specific provider connection.
     */
    public AiResponse testProvider(AiProvider provider, String apiKey, String localUrl, String model) {
        AiProviderService service = providers.get(provider);
        if (service == null) {
            return AiResponse.error("Provider " + provider + " is not available.", provider);
        }
        return service.testConnection(apiKey, localUrl, model);
    }

    /**
     * List all available providers.
     */
    public List<AiProvider> getAvailableProviders() {
        return List.copyOf(providers.keySet());
    }
}
