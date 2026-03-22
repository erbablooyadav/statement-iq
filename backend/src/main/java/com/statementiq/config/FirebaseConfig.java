package com.statementiq.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.private-key:}")
    private String privateKey;

    @Value("${firebase.client-email:}")
    private String clientEmail;

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase already initialized.");
                return;
            }

            // Option 1: Use full service account JSON file path (recommended)
            if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                log.info("Initializing Firebase from service account file: {}", serviceAccountPath);
                try (InputStream is = new FileInputStream(serviceAccountPath)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(is))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK initialized from service account file.");
                    return;
                }
            }

            // Option 2: Use individual env vars to build service account JSON
            if (projectId.isBlank() || privateKey.isBlank() || clientEmail.isBlank()) {
                log.warn("Firebase credentials not configured. Auth will not work. " +
                         "Set FIREBASE_SERVICE_ACCOUNT_PATH to your JSON key file, " +
                         "or set FIREBASE_PROJECT_ID, FIREBASE_PRIVATE_KEY, and FIREBASE_CLIENT_EMAIL.");
                return;
            }

            // Fix private key: handle escaped newlines from env vars
            String formattedKey = privateKey
                    .replace("\\n", "\n")
                    .replace("\\\\n", "\n");

            // Build the full service account JSON
            String serviceAccountJson = """
                    {
                      "type": "service_account",
                      "project_id": "%s",
                      "private_key_id": "auto-generated",
                      "private_key": "%s",
                      "client_email": "%s",
                      "client_id": "000000000000000000000",
                      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                      "token_uri": "https://oauth2.googleapis.com/token",
                      "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                      "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/%s"
                    }
                    """.formatted(
                    projectId,
                    formattedKey.replace("\n", "\\n"),  // re-escape for JSON
                    clientEmail,
                    clientEmail.replace("@", "%40")
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))))
                    .setProjectId(projectId)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized for project: {}", projectId);

        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK", e);
        }
    }
}
