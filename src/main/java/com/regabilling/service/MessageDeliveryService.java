package com.regabilling.service;

import com.regabilling.entity.RetargetingCampaign;
import com.regabilling.entity.RetargetingMetrics;
import com.regabilling.repository.RetargetingCampaignRepository;
import com.regabilling.repository.RetargetingMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryService {
    
    private final EmailService emailService;
    private final SmsService smsService;
    private final RetargetingCampaignRepository campaignRepository;
    private final RetargetingMetricsRepository metricsRepository;
    
    @Async
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> sendEmailWithRetry(RetargetingCampaign campaign) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("📧 Sending email for campaign {}", campaign.getId());
                
                String messageId = emailService.sendRetargetingEmail(
                    campaign.getPreCustomer().getEmail(),
                    campaign.getEmailSubject(),
                    enrichEmailContent(campaign),
                    campaign.getPreCustomer().getName()
                );
                
                updateCampaignSuccess(campaign, messageId, RetargetingCampaign.CampaignStatus.SENT);
                recordMetric(RetargetingMetrics.EventType.EMAIL_SENT, campaign, messageId);
                
                log.info("✅ Email sent successfully for campaign {}", campaign.getId());
                
            } catch (Exception e) {
                log.error("❌ Email delivery failed for campaign {}: {}", campaign.getId(), e.getMessage());
                updateCampaignFailure(campaign, e.getMessage());
                recordMetric(RetargetingMetrics.EventType.EMAIL_FAILED, campaign, e.getMessage());
                throw new RuntimeException("Email delivery failed", e);
            }
        });
    }
    
    @Async
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public CompletableFuture<Void> sendSmsWithRetry(RetargetingCampaign campaign) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("📱 Sending SMS for campaign {}", campaign.getId());
                
                String messageId = smsService.sendRetargetingSms(
                    campaign.getPreCustomer().getPhone(),
                    campaign.getMessageContent()
                );
                
                updateCampaignSuccess(campaign, messageId, RetargetingCampaign.CampaignStatus.SENT);
                recordMetric(RetargetingMetrics.EventType.SMS_SENT, campaign, messageId);
                
                log.info("✅ SMS sent successfully for campaign {}", campaign.getId());
                
            } catch (Exception e) {
                log.error("❌ SMS delivery failed for campaign {}: {}", campaign.getId(), e.getMessage());
                updateCampaignFailure(campaign, e.getMessage());
                recordMetric(RetargetingMetrics.EventType.SMS_FAILED, campaign, e.getMessage());
                throw new RuntimeException("SMS delivery failed", e);
            }
        });
    }
    
    private String enrichEmailContent(RetargetingCampaign campaign) {
        String content = campaign.getMessageContent();
        String trackingPixel = generateTrackingPixel(campaign.getId());
        String unsubscribeLink = generateUnsubscribeLink(campaign.getPreCustomer().getId());
        
        content += trackingPixel;
        content += unsubscribeLink;
        
        return content;
    }
    
    private String generateTrackingPixel(Long campaignId) {
        return String.format(
            "<img src=\"https://regabilling.com/api/track/open/%d\" width=\"1\" height=\"1\" style=\"display:none;\" alt=\"\" />",
            campaignId
        );
    }
    
    private String generateUnsubscribeLink(Long preCustomerId) {
        return String.format(
            "<br><br><center><p style=\"font-size:12px;color:#666;\"><a href=\"https://regabilling.com/api/unsubscribe/%d\" style=\"color:#666;\">Unsubscribe</a></p></center>",
            preCustomerId
        );
    }
    
    private void updateCampaignSuccess(RetargetingCampaign campaign, String messageId, RetargetingCampaign.CampaignStatus status) {
        campaign.setExternalMessageId(messageId);
        campaign.setStatus(status);
        campaign.setSentAt(LocalDateTime.now());
        campaign.setDeliveryStatus("SENT");
        campaignRepository.save(campaign);
    }
    
    private void updateCampaignFailure(RetargetingCampaign campaign, String error) {
        campaign.setStatus(RetargetingCampaign.CampaignStatus.FAILED);
        campaign.setErrorMessage(error);
        campaignRepository.save(campaign);
    }
    
    private void recordMetric(RetargetingMetrics.EventType eventType, RetargetingCampaign campaign, String metadata) {
        try {
            RetargetingMetrics metric = RetargetingMetrics.builder()
                .campaignId(campaign.getId())
                .preCustomerId(campaign.getPreCustomer().getId())
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .metadata(metadata)
                .costIncurred(campaign.getCostIncurred())
                .build();
                
            metricsRepository.save(metric);
        } catch (Exception e) {
            log.error("❌ Error recording metric: {}", e.getMessage(), e);
        }
    }
}
