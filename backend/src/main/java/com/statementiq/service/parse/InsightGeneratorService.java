package com.statementiq.service.parse;

import com.google.gson.Gson;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.Insight;
import com.statementiq.model.Transaction;
import com.statementiq.repository.InsightRepository;
import com.statementiq.service.ai.AiProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(InsightGeneratorService.class);
    private static final int MAX_OUTPUT_TOKENS = 4096;

    private final AiProviderRouter aiProviderRouter;
    private final InsightRepository insightRepository;
    private final com.statementiq.service.domain.UserService userService;
    private final Gson gson;

    private static final String SYSTEM_PROMPT = 
            "You are an expert financial advisor AI. Your job is to analyze a list of transactions " +
            "and output deep financial insights in raw JSON format. DO NOT INCLUDE ANY MARKDOWN OR TEXT outside the JSON.";

    private static final String INSIGHT_PROMPT_TEMPLATE = 
            "Analyze the following transactions and generate an insight summary.\n" +
            "Expected JSON format:\n" +
            "{\n" +
            "  \"totalSpend\": 0.0,\n" +
            "  \"totalCredit\": 0.0,\n" +
            "  \"topCategory\": \"Category Name\",\n" +
            "  \"spendVsLastMonth\": 0.0,\n" +
            "  \"hiddenCharges\": [ { \"type\": \"LATE_FEE|ANNUAL_FEE|FOREX|OTHER\", \"description\": \"\", \"amount\": 0.0, \"advice\": \"\" } ],\n" +
            "  \"subscriptions\": [ { \"merchantName\": \"\", \"amount\": 0.0, \"frequency\": \"MONTHLY|ANNUAL\", \"isNew\": false } ],\n" +
            "  \"savingSuggestions\": [ \"suggestion 1\", \"suggestion 2\" ],\n" +
            "  \"rewardLeakage\": 0.0,\n" +
            "  \"narrativeSummary\": \"A 3-4 sentence engaging summary of their spending habits.\"\n" +
            "}\n\n" +
            "Transactions:\n%s";

    private final com.statementiq.service.domain.AlertGeneratorService alertGeneratorService;
    private final com.statementiq.repository.StatementRepository statementRepository;

    public InsightGeneratorService(AiProviderRouter aiProviderRouter, 
                                   InsightRepository insightRepository, 
                                   com.statementiq.service.domain.UserService userService,
                                   com.statementiq.service.domain.AlertGeneratorService alertGeneratorService,
                                   com.statementiq.repository.StatementRepository statementRepository) {
        this.aiProviderRouter = aiProviderRouter;
        this.insightRepository = insightRepository;
        this.userService = userService;
        this.alertGeneratorService = alertGeneratorService;
        this.statementRepository = statementRepository;
        this.gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(Instant.class, new com.google.gson.TypeAdapter<Instant>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, Instant value) throws java.io.IOException {
                        out.value(value != null ? value.toString() : null);
                    }
                    @Override
                    public Instant read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
                        return Instant.parse(in.nextString());
                    }
                })
                .create();
    }

    public void generateAndSaveInsight(String statementId, String userId, List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            log.warn("No transactions to analyze for statementId {}", statementId);
            return;
        }

        log.info("Generating AI Insight for statement {} with {} transactions", statementId, transactions.size());

        // Format transactions compactly to save tokens
        List<Map<String, Object>> compactTxns = transactions.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", t.getTransactionDate() != null ? t.getTransactionDate().toString() : "");
            m.put("desc", t.getMerchantName() != null ? t.getMerchantName() : t.getRawDescription());
            m.put("amt", t.getAmount());
            m.put("type", t.getTransactionType().name());
            m.put("cat", t.getCategory() != null ? t.getCategory() : "");
            return m;
        }).collect(Collectors.toList());

        String txnsJson = gson.toJson(compactTxns);
        String prompt = String.format(INSIGHT_PROMPT_TEMPLATE, txnsJson);

        try {
            com.statementiq.model.User user = userService.getUserById(userId);
            AiResponse response = aiProviderRouter.route(SYSTEM_PROMPT, prompt, user, MAX_OUTPUT_TOKENS);
            String jsonOutput = cleanJsonResponse(response.content());
            
            Insight generatedInsight = gson.fromJson(jsonOutput, Insight.class);
            generatedInsight.setStatementId(statementId);
            generatedInsight.setUserId(userId);
            generatedInsight.setGeneratedAt(Instant.now());

            insightRepository.save(generatedInsight);
            log.info("Successfully generated and saved insight for statement {}", statementId);
            
            // Trigger Alert Generation based on new Insights
            statementRepository.findById(statementId).ifPresent(stmt -> {
                alertGeneratorService.generateAlerts(stmt, generatedInsight);
            });

        } catch (Exception e) {
            log.error("Failed to generate AI insight for statement {}: {}", statementId, e.getMessage());
        }
    }

    private String cleanJsonResponse(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}
