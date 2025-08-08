package com.regabilling;

import com.regabilling.service.RetargetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final RetargetingService retargetingService;
    
    @PostMapping("/retargeting/trigger-all")
    public ResponseEntity<Map<String, String>> triggerCompleteRetargetingFlow() {
        try {
            log.info("🧪 Triggering complete retargeting flow test...");
            
            retargetingService.scheduleRetargetingCampaigns();
            retargetingService.executePendingCampaigns();
            retargetingService.processFailedCampaigns();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Complete retargeting flow executed successfully!");
            response.put("status", "success");
            response.put("details", "Check logs for campaign details and mock message outputs");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error in complete retargeting flow: {}", e.getMessage(), e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Retargeting flow failed: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/retargeting/health")
    public ResponseEntity<Map<String, String>> checkRetargetingHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("retargeting_system", "operational");
        health.put("mock_email_service", "active");
        health.put("mock_sms_service", "active");
        health.put("scheduler", "running");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }
}
