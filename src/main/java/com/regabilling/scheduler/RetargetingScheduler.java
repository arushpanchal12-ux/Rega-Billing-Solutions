package com.regabilling.scheduler;

import com.regabilling.service.RetargetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.retargeting.enabled", havingValue = "true", matchIfMissing = true)
public class RetargetingScheduler {
    
    private final RetargetingService retargetingService;
    
    @Scheduled(cron = "0 30 10 * * MON", zone = "Asia/Kolkata")
    public void scheduleWeeklyRetargetingCampaigns() {
        log.info("🎯 Starting weekly retargeting campaign scheduling (Monday 10:30 AM)...");
        
        try {
            retargetingService.scheduleRetargetingCampaigns();
            log.info("✅ Weekly retargeting campaign scheduling completed successfully");
        } catch (Exception e) {
            log.error("❌ Error in weekly retargeting campaign scheduling: {}", e.getMessage(), e);
        }
    }
    
    @Scheduled(fixedRate = 600000)
    public void executePendingCampaigns() {
        try {
            retargetingService.executePendingCampaigns();
        } catch (Exception e) {
            log.error("❌ Error executing pending campaigns: {}", e.getMessage(), e);
        }
    }
    
    @Scheduled(fixedRate = 7200000)
    public void processFailedCampaigns() {
        try {
            retargetingService.processFailedCampaigns();
        } catch (Exception e) {
            log.error("❌ Error processing failed campaigns: {}", e.getMessage(), e);
        }
    }
    
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void dailyHealthCheck() {
        log.info("🔍 Running daily retargeting system health check (9:00 AM)...");
    }
}
