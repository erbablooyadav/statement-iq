package com.statementiq.model;

/**
 * Supported AI providers for transaction analysis and insights.
 */
public enum AiProvider {
    CLAUDE("Claude (Anthropic)", "claude-sonnet-4-20250514"),
    GEMINI("Gemini (Google)", "gemini-2.0-flash"),
    OPENAI("OpenAI", "gpt-4o-mini"),
    LOCAL("Local Model", "default");

    private final String displayName;
    private final String defaultModel;

    AiProvider(String displayName, String defaultModel) {
        this.displayName = displayName;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() { return displayName; }
    public String getDefaultModel() { return defaultModel; }
}
