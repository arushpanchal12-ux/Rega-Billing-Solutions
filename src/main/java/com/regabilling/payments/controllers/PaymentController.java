package com.regabilling.payments.controllers;

import com.regabilling.payments.dto.CreateSessionRequest;
import com.regabilling.payments.dto.CreateSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    @PostMapping("/create-session")
    public ResponseEntity<CreateSessionResponse> createPaymentSession(
            @Valid @RequestBody CreateSessionRequest request) {
        
        log.info("Creating payment session for pre-customer: {}", request.getPreCustomerId());
        
        // Mock payment session creation
        CreateSessionResponse response = new CreateSessionResponse(
            "RAZORPAY",
            "session_rzp_" + request.getPreCustomerId(),
            "rzp_test_mock_key",
            "order_mock_" + request.getPreCustomerId(),
            "INR",
            "49900",
            "https://checkout.razorpay.com/mock",
            "Payment session created successfully"
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        log.info("Received Razorpay webhook");
        log.debug("Payload: {}", payload);
        
        // Mock webhook processing
        return ResponseEntity.ok("Webhook processed successfully");
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Received Stripe webhook");
        log.debug("Payload: {}", payload);
        
        // Mock webhook processing
        return ResponseEntity.ok("Webhook processed successfully");
    }
}
