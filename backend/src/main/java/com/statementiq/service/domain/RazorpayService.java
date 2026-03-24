package com.statementiq.service.domain;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Razorpay payment integration service.
 * PRIVACY: No user financial statement data is passed to Razorpay.
 * Only plan metadata and user identifiers are shared.
 */
@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    private static final Map<String, Integer> PLAN_PRICES = Map.of(
            "starter", 9900,  // ₹99/mo
            "pro", 19900,     // ₹199/mo
            "elite", 49900    // ₹499/mo
    );

    private final Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(String planId, String userId) throws Exception {
        Integer amount = PLAN_PRICES.get(planId.toLowerCase());
        if (amount == null) throw new IllegalArgumentException("Unknown plan: " + planId);

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("planId", planId);
        notes.put("userId", userId);

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("amount", amount);
        orderBody.put("currency", "INR");
        orderBody.put("receipt", "statiq_" + userId + "_" + System.currentTimeMillis());
        orderBody.put("notes", notes);

        String authHeader = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(orderBody)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);
        responseMap.put("keyId", keyId);

        log.info("Created Razorpay order {} for plan {}", responseMap.get("id"), planId);
        return responseMap;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    public boolean verifyWebhookSignature(String rawBody, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return computed.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }
}
