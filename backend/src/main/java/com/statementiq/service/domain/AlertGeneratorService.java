package com.statementiq.service.domain;

import com.statementiq.model.Alert;
import com.statementiq.model.Insight;
import com.statementiq.model.Statement;
import com.statementiq.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AlertGeneratorService.class);
    private final AlertRepository alertRepository;

    public AlertGeneratorService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void generateAlerts(Statement statement, Insight insight) {
        if (statement == null) return;
        
        String userId = statement.getUserId();
        String statementId = statement.getId();
        List<Alert> newAlerts = new ArrayList<>();

        // 1. Hidden Charges Alerts
        if (insight != null && insight.getHiddenCharges() != null) {
            for (Insight.HiddenCharge hc : insight.getHiddenCharges()) {
                Alert severityAlert = Alert.builder()
                        .userId(userId)
                        .statementId(statementId)
                        .type(Alert.AlertType.HIDDEN_CHARGE)
                        .title("Hidden Charge Detected: " + hc.getType())
                        .description(hc.getDescription() + ". " + hc.getAdvice())
                        .amount(hc.getAmount())
                        .severity(Alert.AlertSeverity.HIGH)
                        .createdAt(Instant.now())
                        .build();
                newAlerts.add(severityAlert);
            }
        }

        // 2. Bill Due Alert
        if (statement.getBillDueDate() != null && !statement.getBillDueDate().isBlank()) {
            try {
                LocalDate dueDate = LocalDate.parse(statement.getBillDueDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
                
                if (daysUntilDue >= 0 && daysUntilDue <= 7) {
                    Alert billAlert = Alert.builder()
                            .userId(userId)
                            .statementId(statementId)
                            .type(Alert.AlertType.BILL_DUE)
                            .title("Credit Card Bill Due Soon")
                            .description("Your " + statement.getBankName() + " bill is due in " + daysUntilDue + " days.")
                            .amount(statement.getTotalAmountDue())
                            .severity(daysUntilDue <= 3 ? Alert.AlertSeverity.HIGH : Alert.AlertSeverity.MEDIUM)
                            .createdAt(Instant.now())
                            .build();
                    newAlerts.add(billAlert);
                }
            } catch (DateTimeParseException e) {
                log.warn("Could not parse bill due date: {}", statement.getBillDueDate());
            }
        }

        // 3. Saving Opportunities
        if (insight != null && insight.getSavingSuggestions() != null && !insight.getSavingSuggestions().isEmpty()) {
            for (int i = 0; i < Math.min(2, insight.getSavingSuggestions().size()); i++) {
                Alert savingAlert = Alert.builder()
                        .userId(userId)
                        .statementId(statementId)
                        .type(Alert.AlertType.SAVING_OPPORTUNITY)
                        .title("Saving Opportunity")
                        .description(insight.getSavingSuggestions().get(i))
                        .severity(Alert.AlertSeverity.LOW)
                        .createdAt(Instant.now())
                        .build();
                newAlerts.add(savingAlert);
            }
        }

        if (!newAlerts.isEmpty()) {
            alertRepository.saveAll(newAlerts);
            log.info("Generated {} alerts for statement {}", newAlerts.size(), statementId);
        }
    }
}
