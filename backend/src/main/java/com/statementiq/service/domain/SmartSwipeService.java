package com.statementiq.service.domain;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.statementiq.dto.AiResponse;
import com.statementiq.model.SmartSwipe;
import com.statementiq.model.Transaction;
import com.statementiq.model.User;
import com.statementiq.repository.SmartSwipeRepository;
import com.statementiq.repository.TransactionRepository;
import com.statementiq.service.ai.AiProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SmartSwipeService {

    private static final Logger log = LoggerFactory.getLogger(SmartSwipeService.class);
    private final SmartSwipeRepository smartSwipeRepository;
    private final TransactionRepository transactionRepository;
    private final AiProviderRouter aiProviderRouter;
    private final UserService userService;
    private final Gson gson;

    private static final String SYSTEM_PROMPT = 
        "You are an expert Indian credit card advisor. Output ONLY raw JSON array. Do not include markdown or conversational text.";
        
    private static final String USER_PROMPT_TEMPLATE = 
        "The user's top spending categories are: %s. Recommend exactly ONE best credit card in the Indian market for EACH category to maximize their rewards. Return a JSON array with exactly this format: [{\"category\": \"Food Delivery\", \"cardName\": \"SBI Cashback Visa\", \"benefitDescription\": \"5%% cashback on all online food delivery\", \"potentialSaving\": \"₹150\"}]. Use accurate market data.";

    public SmartSwipeService(SmartSwipeRepository smartSwipeRepository,
                             TransactionRepository transactionRepository,
                             AiProviderRouter aiProviderRouter,
                             UserService userService) {
        this.smartSwipeRepository = smartSwipeRepository;
        this.transactionRepository = transactionRepository;
        this.aiProviderRouter = aiProviderRouter;
        this.userService = userService;
        this.gson = new Gson();
    }

    public SmartSwipe getRecommendations(String userId) {
        // 1. Check if we have recent recommendations (within 7 days)
        Optional<SmartSwipe> recentOpt = smartSwipeRepository.findFirstByUserIdOrderByGeneratedAtDesc(userId);
        if (recentOpt.isPresent()) {
            SmartSwipe recent = recentOpt.get();
            if (recent.getGeneratedAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS))) {
                return recent;
            }
        }

        // 2. Fetch recent transactions to find top categories
        Page<Transaction> txnsPage = transactionRepository.findByUserIdAndTransactionType(
                userId, Transaction.TransactionType.DEBIT, 
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "transactionDate"))
        );

        List<Transaction> recentDebits = txnsPage.getContent();
        if (recentDebits.isEmpty()) {
            // Provide a generic default if no transactions exist yet
            return generateGenericRecommendations(userId);
        }

        // Aggregate by category
        Map<String, BigDecimal> categorySums = recentDebits.stream()
                .filter(t -> t.getCategory() != null && !t.getCategory().isBlank())
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<String> topCategories = categorySums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCategories.isEmpty()) {
            return generateGenericRecommendations(userId);
        }

        // 3. Prompt AI for recommendations
        String catString = String.join(", ", topCategories);
        String prompt = String.format(USER_PROMPT_TEMPLATE, catString);

        try {
            User user = userService.getUserById(userId);
            AiResponse response = aiProviderRouter.route(SYSTEM_PROMPT, prompt, user, 1024);
            
            String jsonOutput = response.content().replaceAll("```json", "").replaceAll("```", "").trim();
            Type listType = new TypeToken<List<SmartSwipe.CardRecommendation>>(){}.getType();
            List<SmartSwipe.CardRecommendation> recs = gson.fromJson(jsonOutput, listType);

            SmartSwipe swipe = SmartSwipe.builder()
                    .userId(userId)
                    .recommendations(recs)
                    .generatedAt(Instant.now())
                    .build();

            return smartSwipeRepository.save(swipe);

        } catch (Exception e) {
            log.error("Failed to generate smart swipe for user {}", userId, e);
            return recentOpt.orElseGet(() -> generateGenericRecommendations(userId));
        }
    }

    private SmartSwipe generateGenericRecommendations(String userId) {
        SmartSwipe generic = SmartSwipe.builder()
                .userId(userId)
                .generatedAt(Instant.now())
                .recommendations(List.of(
                        new SmartSwipe.CardRecommendation("Groceries", "ICICI Amazon Pay", "5% cashback for Prime members", "₹100"),
                        new SmartSwipe.CardRecommendation("Food Delivery", "Axis Ace", "4% cashback on Swiggy & Zomato", "₹25"),
                        new SmartSwipe.CardRecommendation("Travel", "HDFC Regalia Gold", "Premium lounge access and 4X points", "₹200")
                ))
                .build();
        return generic; // Don't save generic to DB to allow retry next time
    }
}
