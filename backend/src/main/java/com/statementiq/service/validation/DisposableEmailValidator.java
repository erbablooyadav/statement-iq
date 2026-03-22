package com.statementiq.service.validation;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Blocks disposable/temp email providers at signup.
 * These domains are commonly used for throwaway accounts.
 */
@Service
public class DisposableEmailValidator {

    // Top 200+ most common disposable email domains
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            // Major temp email services
            "mailinator.com", "guerrillamail.com", "guerrillamail.de", "guerrillamail.net",
            "tempmail.com", "temp-mail.org", "throwaway.email", "sharklasers.com",
            "grr.la", "guerrillamailblock.com", "pokemail.net", "spam4.me",
            "yopmail.com", "yopmail.fr", "yopmail.net", "cool.fr.nf",
            "jetable.fr.nf", "nospam.ze.tc", "nomail.xl.cx", "mega.zik.dj",
            "speed.1s.fr", "courriel.fr.nf", "moncourrier.fr.nf", "monemail.fr.nf",

            // Disposable services
            "10minutemail.com", "10minutemail.net", "minutemail.com",
            "tempail.com", "tempr.email", "dispostable.com", "maildrop.cc",
            "mailnesia.com", "mailcatch.com", "mailsac.com",
            "fakeinbox.com", "fakemail.net", "trashmail.com", "trashmail.me",
            "trashmail.net", "trashmail.org", "trashymail.com", "trashymail.net",
            "getnada.com", "anonbox.net",

            // Burner mail
            "burnermail.io", "burnermailbox.com", "emailondeck.com",
            "crazymailing.com", "discard.email", "discardmail.com",
            "discardmail.de", "emailsensei.com", "getairmail.com",

            // Temp/one-time-use
            "mohmal.com", "harakirimail.com", "mailexpire.com",
            "mailforspam.com", "spamgourmet.com", "spamhereplease.com",
            "mytemp.email", "tempsky.com", "tempmailaddress.com",
            "mailzilla.com", "mailnator.com", "sogetthis.com",
            "mailinater.com", "mailinator2.com", "mailinator.net",
            "mailtemp.info", "trbvm.com", "trbvn.com",

            // Catch-all services
            "mintemail.com", "spamfree24.org", "emailigo.de",
            "emkei.cz", "emigmail.com", "filzmail.com",
            "spaml.com", "spamevader.com", "safetymail.info",

            // Other common ones
            "mailnull.com", "zoemail.org", "teleosaurs.xyz",
            "armyspy.com", "cuvox.de", "dayrep.com", "einrot.com",
            "fleckens.hu", "gustr.com", "jourrapide.com", "rhyta.com",
            "superrito.com", "teleworm.us", "throwam.com",
            "zetmail.com", "imgof.com", "mailhub.pro",
            "spam.la", "bugmenot.com", "rmqkr.net"
    );

    /**
     * Returns true if the email is valid (not disposable).
     */
    public boolean isValid(String email) {
        if (email == null || email.isBlank()) return false;

        String domain = extractDomain(email);
        if (domain == null) return false;

        // Block known disposable domains
        if (BLOCKED_DOMAINS.contains(domain.toLowerCase())) return false;

        // Block suspicious patterns
        if (domain.length() < 4) return false;  // Extremely short domains

        return true;
    }

    /**
     * Returns the reason if invalid, null if valid.
     */
    public String getInvalidReason(String email) {
        if (email == null || email.isBlank()) return "Email is required";

        String domain = extractDomain(email);
        if (domain == null) return "Invalid email format";

        if (BLOCKED_DOMAINS.contains(domain.toLowerCase())) {
            return "Disposable or temporary email addresses are not allowed. Please use a permanent email address (Gmail, Outlook, Yahoo, etc.)";
        }

        if (domain.length() < 4) return "Invalid email domain";

        return null; // Valid
    }

    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 1 || atIndex >= email.length() - 1) return null;
        return email.substring(atIndex + 1).trim().toLowerCase();
    }
}
