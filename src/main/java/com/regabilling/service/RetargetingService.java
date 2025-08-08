package com.regabilling.service;

import com.regabilling.entity.RetargetingCampaign;
import com.regabilling.entity.RetargetingTemplate;
import com.regabilling.entity.PreCustomer;
import com.regabilling.entity.RetargetingMetrics;
import com.regabilling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetargetingService {
    
    private final RetargetingCampaignRepository campaignRepository;
    private final RetargetingTemplateRepository templateRepository;
    private final PreCustomerRepository preCustomerRepository;
    private final RetargetingMetricsRepository metricsRepository;
    private final MessageDeliveryService messageDeliveryService;
    
    @Value("${app.retargeting.max-weekly-spend:5000.0}")
    private Double maxWeeklySpend;
    
    @Value("${app.retargeting.email-cost:0.50}")
    private Double emailCost;
    
    @Value("${app.retargeting.sms-cost:3.00}")
    private Double smsCost;
    
    @Transactional
    public void scheduleRetargetingCampaigns() {
        try {
            log.info("🎯 Starting retargeting campaign scheduling...");
            
            if (!isBudgetAvailable()) {
                log.warn("⚠️ Weekly budget limit exceeded, skipping campaign scheduling");
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
            List<PreCustomer> eligibleUsers = preCustomerRepository.findEligibleForRetargeting(
                PreCustomer.PreCustomerStatus.PENDING_PAYMENT, 
                cutoffDate
            );
            
            log.info("📊 Found {} users eligible for retargeting", eligibleUsers.size());
            
            int campaignsScheduled = 0;
            for (PreCustomer preCustomer : eligibleUsers) {
                if (scheduleUserCampaigns(preCustomer)) {
                    campaignsScheduled++;
                }
                
                if (!isBudgetAvailable()) {
                    log.warn("⚠️ Budget limit reached after {} users", campaignsScheduled);
                    break;
                }
            }
            
            log.info("✅ Scheduled campaigns for {} users", campaignsScheduled);
            
        } catch (Exception e) {
            log.error("❌ Error in retargeting campaign scheduling: {}", e.getMessage(), e);
        }
    }
    
    private boolean scheduleUserCampaigns(PreCustomer preCustomer) {
        try {
            int nextWeek = preCustomer.getRetargetingWeek() + 1;
            
            if (nextWeek > 4) {
                preCustomer.setStatus(PreCustomer.PreCustomerStatus.ABANDONED);
                preCustomerRepository.save(preCustomer);
                return false;
            }
            
            Long existingCampaigns = campaignRepository.countByPreCustomerAndCampaignWeek(preCustomer, nextWeek);
            if (existingCampaigns > 0) {
                return false;
            }
            
            LocalDateTime scheduledTime = calculateOptimalScheduleTime(nextWeek);
            
            boolean emailScheduled = scheduleEmailCampaign(preCustomer, nextWeek, scheduledTime);
            boolean smsScheduled = false;
            
            if (nextWeek == 2 || nextWeek == 4) {
                smsScheduled = scheduleSmsCampaign(preCustomer, nextWeek, scheduledTime.plusMinutes(5));
            }
            
            if (emailScheduled || smsScheduled) {
                preCustomer.setRetargetingWeek(nextWeek);
                preCustomer.setLastRetargetingSent(LocalDateTime.now());
                preCustomerRepository.save(preCustomer);
                
                log.info("✅ Scheduled campaigns for user {} (week {})", preCustomer.getEmail(), nextWeek);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error scheduling campaigns for user {}: {}", preCustomer.getEmail(), e.getMessage(), e);
            return false;
        }
    }
    
    private boolean scheduleEmailCampaign(PreCustomer preCustomer, int week, LocalDateTime scheduledAt) {
        Optional<RetargetingTemplate> templateOpt = templateRepository
            .findByTemplateTypeAndCampaignWeekAndIsActiveTrue(RetargetingTemplate.TemplateType.EMAIL, week);
            
        if (templateOpt.isEmpty()) {
            log.warn("⚠️ No email template found for week {}", week);
            return false;
        }
        
        RetargetingTemplate template = templateOpt.get();
        
        RetargetingCampaign campaign = RetargetingCampaign.builder()
            .preCustomer(preCustomer)
            .campaignType(RetargetingCampaign.CampaignType.EMAIL)
            .status(RetargetingCampaign.CampaignStatus.SCHEDULED)
            .messageContent(personalizeMessage(template.getMessageContent(), preCustomer))
            .emailSubject(personalizeMessage(template.getSubjectLine(), preCustomer))
            .scheduledAt(scheduledAt)
            .campaignWeek(week)
            .costIncurred(emailCost)
            .build();
            
        campaignRepository.save(campaign);
        recordMetric(RetargetingMetrics.EventType.EMAIL_SCHEDULED, campaign, null);
        
        return true;
    }
    
    private boolean scheduleSmsCampaign(PreCustomer preCustomer, int week, LocalDateTime scheduledAt) {
        Optional<RetargetingTemplate> templateOpt = templateRepository
            .findByTemplateTypeAndCampaignWeekAndIsActiveTrue(RetargetingTemplate.TemplateType.SMS, week);
            
        if (templateOpt.isEmpty()) {
            log.warn("⚠️ No SMS template found for week {}", week);
            return false;
        }
        
        RetargetingTemplate template = templateOpt.get();
        
        RetargetingCampaign campaign = RetargetingCampaign.builder()
            .preCustomer(preCustomer)
            .campaignType(RetargetingCampaign.CampaignType.SMS)
            .status(RetargetingCampaign.CampaignStatus.SCHEDULED)
            .messageContent(personalizeMessage(template.getMessageContent(), preCustomer))
            .scheduledAt(scheduledAt)
            .campaignWeek(week)
            .costIncurred(smsCost)
            .build();
            
        campaignRepository.save(campaign);
        recordMetric(RetargetingMetrics.EventType.SMS_SCHEDULED, campaign, null);
        
        return true;
    }
    
    @Transactional
    public void executePendingCampaigns() {
        try {
            List<RetargetingCampaign> pendingCampaigns = campaignRepository.findScheduledCampaigns(
                RetargetingCampaign.CampaignStatus.SCHEDULED,
                LocalDateTime.now()
            );
            
            if (pendingCampaigns.isEmpty()) {
                log.debug("No pending campaigns to execute");
                return;
            }
            
            log.info("🚀 Executing {} pending campaigns", pendingCampaigns.size());
            
            for (RetargetingCampaign campaign : pendingCampaigns) {
                executeCampaignAsync(campaign);
            }
            
        } catch (Exception e) {
            log.error("❌ Error executing pending campaigns: {}", e.getMessage(), e);
        }
    }
    
    private void executeCampaignAsync(RetargetingCampaign campaign) {
        if (campaign.getCampaignType() == RetargetingCampaign.CampaignType.EMAIL) {
            messageDeliveryService.sendEmailWithRetry(campaign);
        } else {
            messageDeliveryService.sendSmsWithRetry(campaign);
        }
    }
    
    @Transactional
    public void processFailedCampaigns() {
        try {
            List<RetargetingCampaign> failedCampaigns = campaignRepository
                .findFailedCampaignsForRetry(RetargetingCampaign.CampaignStatus.FAILED);
                
            log.info("🔄 Processing {} failed campaigns for retry", failedCampaigns.size());
            
            for (RetargetingCampaign campaign : failedCampaigns) {
                if (campaign.canRetry()) {
                    campaign.incrementRetry();
                    campaign.setStatus(RetargetingCampaign.CampaignStatus.SCHEDULED);
                    campaign.setScheduledAt(LocalDateTime.now().plusMinutes(30));
                    campaignRepository.save(campaign);
                    
                    log.info("🔄 Scheduled retry {} for campaign {}", campaign.getRetryCount(), campaign.getId());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error processing failed campaigns: {}", e.getMessage(), e);
        }
    }
    
    public void trackEmailOpen(Long campaignId, String userAgent, String ipAddress) {
        try {
            Optional<RetargetingCampaign> campaignOpt = campaignRepository.findById(campaignId);
            if (campaignOpt.isPresent()) {
                RetargetingCampaign campaign = campaignOpt.get();
                campaign.setStatus(RetargetingCampaign.CampaignStatus.OPENED);
                campaign.setOpenedAt(LocalDateTime.now());
                campaignRepository.save(campaign);
                
                recordMetric(RetargetingMetrics.EventType.EMAIL_OPENED, campaign, 
                    String.format("user_agent=%s,ip=%s", userAgent, ipAddress));
                    
                log.info("👀 Email opened for campaign {}", campaignId);
            }
        } catch (Exception e) {
            log.error("❌ Error tracking email open: {}", e.getMessage(), e);
        }
    }
    
    public void trackEmailClick(Long campaignId, String clickedUrl) {
        try {
            Optional<RetargetingCampaign> campaignOpt = campaignRepository.findById(campaignId);
            if (campaignOpt.isPresent()) {
                RetargetingCampaign campaign = campaignOpt.get();
                campaign.setStatus(RetargetingCampaign.CampaignStatus.CLICKED);
                campaign.setClickedAt(LocalDateTime.now());
                campaignRepository.save(campaign);
                
                recordMetric(RetargetingMetrics.EventType.EMAIL_CLICKED, campaign, 
                    String.format("clicked_url=%s", clickedUrl));
                    
                log.info("🔗 Email clicked for campaign {}: {}", campaignId, clickedUrl);
            }
        } catch (Exception e) {
            log.error("❌ Error tracking email click: {}", e.getMessage(), e);
        }
    }
    
    private String personalizeMessage(String template, PreCustomer preCustomer) {
        if (template == null) return "";
        
        long daysSinceSignup = ChronoUnit.DAYS.between(preCustomer.getCreatedAt(), LocalDateTime.now());
        String firstName = preCustomer.getName().split(" ")[0];
        
        return template
            .replace("{{name}}", preCustomer.getName())
            .replace("{{firstName}}", firstName)
            .replace("{{email}}", preCustomer.getEmail())
            .replace("{{daysSinceSignup}}", String.valueOf(daysSinceSignup))
            .replace("{{signupDate}}", preCustomer.getCreatedAt().toLocalDate().toString());
    }
    
    private LocalDateTime calculateOptimalScheduleTime(int week) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek targetDay = (week == 1) ? DayOfWeek.MONDAY : DayOfWeek.TUESDAY;
        int targetHour = (week <= 2) ? 11 : 10;
        
        LocalDateTime nextTarget = now.with(targetDay).withHour(targetHour).withMinute(0).withSecond(0);
        
        if (nextTarget.isBefore(now) || nextTarget.isEqual(now)) {
            nextTarget = nextTarget.plusWeeks(1);
        }
        
        return nextTarget;
    }
    
    private boolean isBudgetAvailable() {
        LocalDateTime weekStart = LocalDateTime.now().with(DayOfWeek.MONDAY).withHour(0).withMinute(0);
        Double weeklySpend = campaignRepository.getTotalCostSince(weekStart);
        
        return weeklySpend == null || weeklySpend < maxWeeklySpend;
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
/ /   C o d e R a b b i t :   A n a l y z e   r e t a r g e t i n g   l o g i c   f o r   p e r f o r m a n c e   a n d   c o r r e c t n e s s  
 