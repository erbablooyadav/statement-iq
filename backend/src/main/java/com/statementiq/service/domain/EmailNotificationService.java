package com.statementiq.service.domain;

import com.statementiq.model.Goal;
import com.statementiq.model.ReportCard;
import com.statementiq.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Handles outbound email notifications.
 *
 * PRIVACY: No raw statement data, PDF content or transaction lists
 * are ever included in emails. Only high-level aggregated summaries.
 *
 * Currently uses Spring's SimpleMailMessage (plain text) for maximum
 * compatibility. Can be upgraded to HTML templates (Thymeleaf / MJML)
 * when a dedicated transactional email provider (e.g., Resend, SES) is configured.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final MailSender mailSender;

    public EmailNotificationService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Sends the monthly ReportCard summary email. */
    public void sendMonthlyReport(User user, ReportCard card) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("📊 Your " + card.getMonth() + " Financial Report | StatementIQ");
            msg.setText(buildMonthlyReportBody(user, card));
            mailSender.send(msg);
            log.info("Monthly report email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send monthly report to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /** Sends a bill-due reminder (7 days and 3 days). */
    public void sendBillDueReminder(User user, String bankName, String dueDate, String amountDue, int daysRemaining) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("⚠️ " + bankName + " Bill Due in " + daysRemaining + " Days | StatementIQ");
            msg.setText(
                    "Hi " + (user.getName() != null ? user.getName().split(" ")[0] : "there") + ",\n\n" +
                    "Your " + bankName + " credit card bill of " + amountDue + " is due on " + dueDate + ".\n\n" +
                    "Please make sure to pay on time to avoid late fees.\n\n" +
                    "— StatementIQ\n\n" +
                    "Your data stays private. We never share or sell your financial information.\n" +
                    "Unsubscribe: Visit your StatementIQ account settings."
            );
            mailSender.send(msg);
            log.info("Bill due reminder sent to {} for {}", user.getEmail(), bankName);
        } catch (Exception e) {
            log.warn("Failed to send bill reminder to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /** Sends a goal check-in nudge. */
    public void sendGoalNudge(User user, Goal goal, String missedDays) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject("🎯 Don't Break Your Streak! | StatementIQ");
            msg.setText(
                    "Hi " + (user.getName() != null ? user.getName().split(" ")[0] : "there") + ",\n\n" +
                    "You haven't logged a check-in for your goal \"" + goal.getName() + "\" in " + missedDays + " days.\n\n" +
                    "Even a small contribution keeps your streak alive! Log in to StatementIQ to check in.\n\n" +
                    "— StatementIQ"
            );
            mailSender.send(msg);
            log.info("Goal nudge email sent to {} for goal {}", user.getEmail(), goal.getName());
        } catch (Exception e) {
            log.warn("Failed to send goal nudge to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildMonthlyReportBody(User user, ReportCard card) {
        String name = user.getName() != null ? user.getName().split(" ")[0] : "there";
        return "Hi " + name + ",\n\n" +
               "Here's your " + card.getMonth() + " Financial Report Card:\n\n" +
               "🏆 Badge: " + card.getBadge() + "\n" +
               "📊 Overall Score: " + card.getOverallScore() + "/100\n\n" +
               "   Savings Score:      " + card.getSavingsScore() + "/100\n" +
               "   Spend Control:      " + card.getSpendControlScore() + "/100\n" +
               "   Hidden Charge Free: " + card.getHiddenChargeScore() + "/100\n\n" +
               "💰 Summary:\n" +
               "   Income:  ₹" + card.getTotalIncome() + "\n" +
               "   Spent:   ₹" + card.getTotalSpent() + "\n" +
               "   Saved:   ₹" + card.getTotalSaved() + "\n" +
               "   Transactions: " + card.getTransactionCount() + "\n\n" +
               card.getAiNarrative() + "\n\n" +
               "Login to StatementIQ to see full breakdown and smart recommendations.\n\n" +
               "— StatementIQ Team\n\n" +
               "Privacy Notice: This summary contains aggregate data only. " +
               "We never include raw transaction details in emails.\n" +
               "Manage email preferences in your account settings.";
    }
}
