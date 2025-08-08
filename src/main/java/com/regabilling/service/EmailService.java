package com.regabilling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final RestTemplate restTemplate;
    
    @Value("${app.email.sendgrid.api-key:mock-api-key}")
    private String sendGridApiKey;
    
    @Value("${app.email.sendgrid.from-email:noreply@regabilling.com}")
    private String fromEmail;
    
    @Value("${app.email.sendgrid.from-name:Rega Billing Solutions}")
    private String fromName;
    
    @Value("${app.email.mock-mode:true}")
    private boolean mockMode;
    
    public String sendRetargetingEmail(String toEmail, String subject, String content, String recipientName) {
        if (mockMode) {
            return sendMockEmail(toEmail, subject, content, recipientName);
        }
        
        return sendViaSendGrid(toEmail, subject, content, recipientName);
    }
    
    private String sendMockEmail(String toEmail, String subject, String content, String recipientName) {
        String mockMessageId = "mock_email_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📧 MOCK EMAIL - Retargeting Campaign");
        log.info("   ✉️  To: {}", toEmail);
        log.info("   📝 Subject: {}", subject);
        log.info("   👤 Recipient: {}", recipientName);
        log.info("   📄 Content Length: {} chars", content.length());
        log.info("   🆔 Message ID: {}", mockMessageId);
        log.info("   ⏰ Sent at: {}", java.time.LocalDateTime.now());
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return mockMessageId;
    }
    
    private String sendViaSendGrid(String toEmail, String subject, String content, String recipientName) {
        try {
            String url = "https://api.sendgrid.com/v3/mail/send";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendGridApiKey);
            
            Map<String, Object> emailPayload = createSendGridPayload(toEmail, subject, content, recipientName);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String messageId = response.getHeaders().getFirst("X-Message-Id");
                log.info("📧 Email sent via SendGrid to {}", toEmail);
                return messageId != null ? messageId : "sendgrid_" + UUID.randomUUID().toString().substring(0, 8);
            } else {
                throw new RuntimeException("SendGrid API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("❌ SendGrid email failed for {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> createSendGridPayload(String toEmail, String subject, String content, String recipientName) {
        Map<String, Object> payload = new HashMap<>();
        
        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        from.put("name", fromName);
        payload.put("from", from);
        
        Map<String, String> to = new HashMap<>();
        to.put("email", toEmail);
        to.put("name", recipientName);
        
        Map<String, Object> personalization = new HashMap<>();
        personalization.put("to", new Object[]{to});
        personalization.put("subject", subject);
        payload.put("personalizations", new Object[]{personalization});
        
        Map<String, String> contentMap = new HashMap<>();
        contentMap.put("type", "text/html");
        contentMap.put("value", content);
        payload.put("content", new Object[]{contentMap});
        
        Map<String, Object> tracking = new HashMap<>();
        Map<String, Boolean> openTracking = new HashMap<>();
        openTracking.put("enable", true);
        tracking.put("open_tracking", openTracking);
        
        Map<String, Boolean> clickTracking = new HashMap<>();
        clickTracking.put("enable", true);
        tracking.put("click_tracking", clickTracking);
        
        payload.put("tracking_settings", tracking);
        
        return payload;
    }
}
