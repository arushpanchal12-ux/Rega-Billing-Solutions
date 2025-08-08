package com.regabilling.controller;

import com.regabilling.service.RetargetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {
    
    private final RetargetingService retargetingService;
    
    @GetMapping("/open/{campaignId}")
    public ResponseEntity<byte[]> trackEmailOpen(
        @PathVariable Long campaignId,
        @RequestHeader(value = "User-Agent", required = false) String userAgent,
        @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
        @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        
        try {
            String clientIp = ipAddress != null ? ipAddress : (realIp != null ? realIp : "unknown");
            retargetingService.trackEmailOpen(campaignId, userAgent, clientIp);
            
            byte[] pixelBytes = {
                (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
                (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x01, (byte) 0x00,
                (byte) 0x3B
            };
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "image/gif");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");
            
            return new ResponseEntity<>(pixelBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("Error tracking email open for campaign {}: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/click/{campaignId}")
    public ResponseEntity<Void> trackEmailClick(
        @PathVariable Long campaignId,
        @RequestParam(required = false) String redirect) {
        
        try {
            retargetingService.trackEmailClick(campaignId, redirect);
            
            if (redirect != null && !redirect.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", redirect);
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            } else {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", "https://regabilling.com");
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
            
        } catch (Exception e) {
            log.error("Error tracking email click for campaign {}: {}", campaignId, e.getMessage());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirect != null ? redirect : "https://regabilling.com");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }
}
