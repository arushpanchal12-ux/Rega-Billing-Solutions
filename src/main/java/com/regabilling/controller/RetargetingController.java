package com.regabilling.controller;

import com.regabilling.entity.RetargetingCampaign;
import com.regabilling.entity.RetargetingTemplate;
import com.regabilling.repository.RetargetingCampaignRepository;
import com.regabilling.repository.RetargetingTemplateRepository;
import com.regabilling.service.RetargetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/retargeting")
@RequiredArgsConstructor
@Slf4j
public class RetargetingController {
    
    private final RetargetingService retargetingService;
    private final RetargetingCampaignRepository campaignRepository;
    private final RetargetingTemplateRepository templateRepository;
    
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> triggerScheduling() {
        try {
            retargetingService.scheduleRetargetingCampaigns();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Retargeting campaigns scheduled successfully");
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error triggering retargeting scheduling: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to schedule campaigns: " + e.getMessage());
            response.put("status", "error");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> triggerExecution() {
        try {
            retargetingService.executePendingCampaigns();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pending campaigns executed successfully");
            response.put("status", "success");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error triggering campaign execution: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Failed to execute campaigns: " + e.getMessage());
            response.put("status", "error");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/campaigns")
    public ResponseEntity<Page<RetargetingCampaign>> getCampaigns(Pageable pageable) {
        Page<RetargetingCampaign> campaigns = campaignRepository.findAll(pageable);
        return ResponseEntity.ok(campaigns);
    }
    
    @GetMapping("/campaigns/user/{preCustomerId}")
    public ResponseEntity<List<RetargetingCampaign>> getUserCampaigns(@PathVariable Long preCustomerId) {
        List<RetargetingCampaign> campaigns = campaignRepository.findByPreCustomerIdOrderByCreatedAtDesc(preCustomerId);
        return ResponseEntity.ok(campaigns);
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<RetargetingTemplate>> getTemplates() {
        List<RetargetingTemplate> templates = templateRepository.findByIsActiveTrueOrderByCampaignWeekAscTemplateTypeAsc();
        return ResponseEntity.ok(templates);
    }
}
