package com.statementiq.controller;

import com.statementiq.dto.AiResponse;
import com.statementiq.dto.ApiResponse;
import com.statementiq.model.User;
import com.statementiq.service.ai.AiProviderRouter;
import com.statementiq.service.domain.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for AI Insights and AI Chatbot.
 */
@RestController
@RequestMapping("/api/v1/insights")
public class AiInsightsController {

    private final AiProviderRouter aiProviderRouter;
    private final UserService userService;

    public AiInsightsController(AiProviderRouter aiProviderRouter, UserService userService) {
        this.aiProviderRouter = aiProviderRouter;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody ChatRequest request, Authentication auth) {
        String firebaseUid = (String) auth.getPrincipal();
        User user = userService.getUserByFirebaseUid(firebaseUid);

        // Must be Pro or have BYOK keys configured
        boolean isPro = user.getPlan() != null && "PRO".equalsIgnoreCase(user.getPlan().toString());
        boolean hasKeys = user.getAiApiKeys() != null && !user.getAiApiKeys().isEmpty();
        boolean hasLocal = user.getLocalModelUrl() != null && !user.getLocalModelUrl().isEmpty();

        if (!isPro && !hasKeys && !hasLocal) {
            return ResponseEntity.status(403).body(
                    ApiResponse.error("UPGRADE_REQUIRED", "AI Chat is available for Pro users or if you provide your own API key (BYOK). Head to Settings to add a key!")
            );
        }

        String systemPrompt = "You are 'StatementIQ AI', a helpful, expert, and incredibly smart financial co-pilot for the user. " +
                "You analyze their spending, give personalized advice, and help them save money. " +
                "Be incredibly concise, friendly, and use formatting (bullet points, bold text) to make your points clear. " +
                "Always respond in plain text or markdown. DO NOT output raw JSON.";

        // Build context from history
        StringBuilder context = new StringBuilder();
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            context.append("Conversation History:\n");
            for (ChatMessage msg : request.getHistory()) {
                context.append(msg.getRole().equalsIgnoreCase("user") ? "User: " : "StatementIQ AI: ")
                       .append(msg.getContent()).append("\n");
            }
            context.append("\n");
        }

        context.append("User's latest message:\n").append(request.getMessage());

        AiResponse response = aiProviderRouter.route(systemPrompt, context.toString(), user);

        if (!response.success()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("AI_ERROR", response.error()));
        }

        return ResponseEntity.ok(ApiResponse.ok(response.content()));
    }

    @Data
    public static class ChatRequest {
        private String message;
        private List<ChatMessage> history;
    }

    @Data
    public static class ChatMessage {
        private String role; // 'user' or 'ai'
        private String content;
    }
}
