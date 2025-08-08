package com.regabilling.controller;

import com.regabilling.entity.PreCustomer;
import com.regabilling.repository.PreCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/unsubscribe")
@RequiredArgsConstructor
@Slf4j
public class UnsubscribeController {
    
    private final PreCustomerRepository preCustomerRepository;
    
    @GetMapping("/{preCustomerId}")
    public ResponseEntity<String> unsubscribePage(@PathVariable Long preCustomerId) {
        try {
            Optional<PreCustomer> preCustomerOpt = preCustomerRepository.findById(preCustomerId);
            
            if (preCustomerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid unsubscribe link");
            }
            
            PreCustomer preCustomer = preCustomerOpt.get();
            
            if (preCustomer.getUnsubscribedAt() != null) {
                return ResponseEntity.ok("Already unsubscribed");
            }
            
            return ResponseEntity.ok("Unsubscribe page for " + preCustomer.getEmail());
            
        } catch (Exception e) {
            log.error("Error generating unsubscribe page: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("An error occurred");
        }
    }
    
    @PostMapping("/{preCustomerId}/confirm")
    public ResponseEntity<String> confirmUnsubscribe(@PathVariable Long preCustomerId) {
        try {
            Optional<PreCustomer> preCustomerOpt = preCustomerRepository.findById(preCustomerId);
            
            if (preCustomerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid unsubscribe link");
            }
            
            PreCustomer preCustomer = preCustomerOpt.get();
            preCustomer.setUnsubscribedAt(LocalDateTime.now());
            preCustomer.setStatus(PreCustomer.PreCustomerStatus.UNSUBSCRIBED);
            preCustomerRepository.save(preCustomer);
            
            log.info("✅ User {} successfully unsubscribed from retargeting", preCustomer.getEmail());
            
            return ResponseEntity.ok("Successfully unsubscribed");
            
        } catch (Exception e) {
            log.error("Error processing unsubscribe: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to unsubscribe");
        }
    }
}
