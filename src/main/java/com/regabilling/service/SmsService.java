package com.regabilling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {
    
    private final RestTemplate restTemplate;
    
    @Value("${app.sms.twilio.account-sid:mock-account-sid}")
    private String twilioAccountSid;
    
    @Value("${app.sms.twilio.auth-token:mock-auth-token}")
    private String twilioAuthToken;
    
    @Value("${app.sms.twilio.from-number:+1234567890}")
    private String fromNumber;
    
    @Value("${app.sms.mock-mode:true}")
    private boolean mockMode;
    
    public String sendRetargetingSms(String toNumber, String message) {
        if (mockMode) {
            return sendMockSms(toNumber, message);
        }
        
        return sendViaTwilio(toNumber, message);
    }
    
    private String sendMockSms(String toNumber, String message) {
        String mockMessageId = "mock_sms_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("📱 MOCK SMS - Retargeting Campaign");
        log.info("   📞 To: {}", toNumber);
        log.info("   💬 Message: {}", message);
        log.info("   📏 Length: {} chars", message.length());
        log.info("   🆔 Message ID: {}", mockMessageId);
        log.info("   ⏰ Sent at: {}", java.time.LocalDateTime.now());
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return mockMessageId;
    }
    
    private String sendViaTwilio(String toNumber, String message) {
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String credentials = twilioAccountSid + ":" + twilioAuthToken;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", fromNumber);
            body.add("To", toNumber);
            body.add("Body", message);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("📱 SMS sent via Twilio to {}", toNumber);
                return "twilio_" + UUID.randomUUID().toString().substring(0, 8);
            } else {
                throw new RuntimeException("Twilio API returned status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("❌ Twilio SMS failed for {}: {}", toNumber, e.getMessage());
            throw new RuntimeException("SMS sending failed: " + e.getMessage(), e);
        }
    }
}
