package com.statementiq.controller;

import com.google.gson.Gson;
import com.statementiq.dto.ApiResponse;
import com.statementiq.model.User;
import com.statementiq.service.domain.RazorpayService;
import com.statementiq.service.domain.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final RazorpayService razorpayService;
    private final UserService userService;
    private final Gson gson = new Gson();

    public PaymentController(RazorpayService razorpayService, UserService userService) {
        this.razorpayService = razorpayService;
        this.userService = userService;
    }

    /** POST /payments/create-order */
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @RequestBody Map<String, String> body, Authentication auth) {
        String planId = body.get("planId");
        if (planId == null || planId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "planId is required"));
        }
        try {
            User user = userService.getUserByFirebaseUid((String) auth.getPrincipal());
            Map<String, Object> order = razorpayService.createOrder(planId, user.getId());
            return ResponseEntity.ok(ApiResponse.ok(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create Razorpay order", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("PAYMENT_ERROR", "Payment initiation failed"));
        }
    }

    /** POST /payments/verify */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyPayment(
            @RequestBody Map<String, String> body, Authentication auth) {
        boolean valid = razorpayService.verifyPaymentSignature(
                body.get("razorpay_order_id"),
                body.get("razorpay_payment_id"),
                body.get("razorpay_signature"));
        if (valid) {
            log.info("Payment verified for order {}", body.get("razorpay_order_id"));
            return ResponseEntity.ok(ApiResponse.ok("Payment verified"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_SIGNATURE", "Invalid payment signature"));
    }

    /** POST /payments/webhook — unauthenticated, secured by HMAC */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || !razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Webhook: invalid/missing signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = gson.fromJson(rawBody, Map.class);
            log.info("Razorpay webhook: {}", event.get("event"));
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
        }
        return ResponseEntity.ok("OK");
    }
}
