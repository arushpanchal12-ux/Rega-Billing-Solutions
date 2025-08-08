# ================================================================
# Rega Billing Solutions - Complete Retargeting System Generator (FIXED)
# This script creates all files and folders for the retargeting system
# ================================================================

Write-Host "🚀 Generating Complete Retargeting System for Rega Billing Solutions..." -ForegroundColor Green

$projectRoot = "C:\Users\arush\Desktop\Rega Billing Solutions\rega-billing-solutions"
$javaPath = "$projectRoot\src\main\java\com\regabilling"
$resourcesPath = "$projectRoot\src\main\resources"

# Ensure we're in the correct directory
if (!(Test-Path $projectRoot)) {
    Write-Host "❌ Project directory not found: $projectRoot" -ForegroundColor Red
    exit 1
}

Set-Location $projectRoot

Write-Host "📁 Creating retargeting directories..." -ForegroundColor Cyan

# Create retargeting package structure
$retargetingDirs = @(
    "$javaPath\entity",
    "$javaPath\repository", 
    "$javaPath\service",
    "$javaPath\controller",
    "$javaPath\scheduler",
    "$javaPath\config",
    "$resourcesPath\db\migration"
)

foreach ($dir in $retargetingDirs) {
    if (!(Test-Path $dir)) {
        New-Item -Path $dir -ItemType Directory -Force | Out-Null
    }
}

Write-Host "📝 Generating entity classes..." -ForegroundColor Cyan

# =====================================
# ENTITY CLASSES
# =====================================

# Update PreCustomer.java
$preCustomerContent = @'
package com.regabilling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pre_customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String phone;
    
    @Column(nullable = false)
    private String password;
    
    @Builder.Default
    @Column(name = "marketing_consent")
    private Boolean marketingConsent = true;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreCustomerStatus status = PreCustomerStatus.PENDING_PAYMENT;
    
    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
    
    @Column(name = "last_retargeting_sent")
    private LocalDateTime lastRetargetingSent;
    
    @Builder.Default
    @Column(name = "retargeting_week")
    private Integer retargetingWeek = 0;
    
    @Column(name = "conversion_source")
    private String conversionSource;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "preCustomer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RetargetingCampaign> retargetingCampaigns;
    
    public enum PreCustomerStatus {
        PENDING_PAYMENT,
        PAYMENT_FAILED,
        CONVERTED,
        UNSUBSCRIBED,
        ABANDONED
    }
    
    public boolean isEligibleForRetargeting() {
        return status == PreCustomerStatus.PENDING_PAYMENT 
            && unsubscribedAt == null 
            && marketingConsent == true
            && retargetingWeek < 4;
    }
}
'@

# RetargetingCampaign.java
$retargetingCampaignContent = @'
package com.regabilling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "retargeting_campaigns", indexes = {
    @Index(name = "idx_retargeting_status_scheduled", columnList = "status, scheduled_at"),
    @Index(name = "idx_retargeting_pre_customer", columnList = "pre_customer_id"),
    @Index(name = "idx_retargeting_external_id", columnList = "external_message_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetargetingCampaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_customer_id", nullable = false)
    private PreCustomer preCustomer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignType campaignType;
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.SCHEDULED;
    
    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
    
    @Column(name = "email_subject")
    private String emailSubject;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "opened_at")
    private LocalDateTime openedAt;
    
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
    
    @Column(name = "delivery_status")
    private String deliveryStatus;
    
    @Column(name = "external_message_id")
    private String externalMessageId;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "campaign_week")
    private Integer campaignWeek;
    
    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Builder.Default
    @Column(name = "cost_incurred")
    private Double costIncurred = 0.0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CampaignType {
        EMAIL, SMS
    }
    
    public enum CampaignStatus {
        SCHEDULED, SENT, DELIVERED, OPENED, CLICKED, FAILED, CONVERTED
    }
    
    public boolean canRetry() {
        return status == CampaignStatus.FAILED && retryCount < 3;
    }
    
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
'@

# RetargetingTemplate.java
$retargetingTemplateContent = @'
package com.regabilling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "retargeting_templates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"template_type", "campaign_week"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetargetingTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String templateName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateType templateType;
    
    @Column(name = "subject_line")
    private String subjectLine;
    
    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String messageContent;
    
    @Column(name = "campaign_week", nullable = false)
    private Integer campaignWeek;
    
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "expected_conversion_rate")
    private Double expectedConversionRate;
    
    @Column(name = "cost_per_message")
    private Double costPerMessage;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TemplateType {
        EMAIL, SMS
    }
}
'@

# RetargetingMetrics.java
$retargetingMetricsContent = @'
package com.regabilling.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "retargeting_metrics", indexes = {
    @Index(name = "idx_metrics_event_date", columnList = "event_type, event_timestamp"),
    @Index(name = "idx_metrics_campaign", columnList = "campaign_id"),
    @Index(name = "idx_metrics_pre_customer", columnList = "pre_customer_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetargetingMetrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "campaign_id")
    private Long campaignId;
    
    @Column(name = "pre_customer_id")
    private Long preCustomerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;
    
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @Builder.Default
    @Column(name = "cost_incurred")
    private Double costIncurred = 0.0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum EventType {
        EMAIL_SCHEDULED, EMAIL_SENT, EMAIL_DELIVERED, EMAIL_OPENED, EMAIL_CLICKED, EMAIL_FAILED,
        SMS_SCHEDULED, SMS_SENT, SMS_DELIVERED, SMS_FAILED,
        CAMPAIGN_CONVERTED, CAMPAIGN_UNSUBSCRIBED
    }
}
'@

Write-Host "📝 Generating repository classes..." -ForegroundColor Cyan

# RetargetingCampaignRepository.java
$campaignRepositoryContent = @'
package com.regabilling.repository;

import com.regabilling.entity.RetargetingCampaign;
import com.regabilling.entity.PreCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetargetingCampaignRepository extends JpaRepository<RetargetingCampaign, Long> {
    
    @Query("SELECT rc FROM RetargetingCampaign rc WHERE rc.status = :status AND rc.scheduledAt <= :currentTime ORDER BY rc.scheduledAt ASC")
    List<RetargetingCampaign> findScheduledCampaigns(
        @Param("status") RetargetingCampaign.CampaignStatus status, 
        @Param("currentTime") LocalDateTime currentTime
    );
    
    @Query("SELECT rc FROM RetargetingCampaign rc WHERE rc.preCustomer.id = :preCustomerId ORDER BY rc.createdAt DESC")
    List<RetargetingCampaign> findByPreCustomerIdOrderByCreatedAtDesc(@Param("preCustomerId") Long preCustomerId);
    
    @Query("SELECT COUNT(rc) FROM RetargetingCampaign rc WHERE rc.preCustomer = :preCustomer AND rc.campaignWeek = :week")
    Long countByPreCustomerAndCampaignWeek(@Param("preCustomer") PreCustomer preCustomer, @Param("week") Integer week);
    
    @Query("SELECT rc FROM RetargetingCampaign rc WHERE rc.status = :status AND rc.retryCount < 3")
    List<RetargetingCampaign> findFailedCampaignsForRetry(@Param("status") RetargetingCampaign.CampaignStatus status);
    
    Optional<RetargetingCampaign> findByExternalMessageId(String externalMessageId);
    
    @Query("SELECT SUM(rc.costIncurred) FROM RetargetingCampaign rc WHERE rc.createdAt >= :startDate")
    Double getTotalCostSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(rc) FROM RetargetingCampaign rc WHERE rc.status = :status AND rc.createdAt >= :startDate")
    Long getConversionCountSince(@Param("status") RetargetingCampaign.CampaignStatus status, @Param("startDate") LocalDateTime startDate);
}
'@

# RetargetingTemplateRepository.java
$templateRepositoryContent = @'
package com.regabilling.repository;

import com.regabilling.entity.RetargetingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetargetingTemplateRepository extends JpaRepository<RetargetingTemplate, Long> {
    
    Optional<RetargetingTemplate> findByTemplateTypeAndCampaignWeekAndIsActiveTrue(
        RetargetingTemplate.TemplateType templateType, 
        Integer campaignWeek
    );
    
    List<RetargetingTemplate> findByTemplateTypeAndIsActiveTrueOrderByCampaignWeek(RetargetingTemplate.TemplateType templateType);
    
    List<RetargetingTemplate> findByIsActiveTrueOrderByCampaignWeekAscTemplateTypeAsc();
}
'@

# RetargetingMetricsRepository.java
$metricsRepositoryContent = @'
package com.regabilling.repository;

import com.regabilling.entity.RetargetingMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RetargetingMetricsRepository extends JpaRepository<RetargetingMetrics, Long> {
    
    List<RetargetingMetrics> findByCampaignIdOrderByEventTimestampAsc(Long campaignId);
    
    List<RetargetingMetrics> findByPreCustomerIdOrderByEventTimestampAsc(Long preCustomerId);
    
    @Query("SELECT COUNT(rm) FROM RetargetingMetrics rm WHERE rm.eventType = :eventType AND rm.eventTimestamp >= :startDate")
    Long countByEventTypeAndEventTimestampAfter(@Param("eventType") RetargetingMetrics.EventType eventType, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT SUM(rm.costIncurred) FROM RetargetingMetrics rm WHERE rm.eventTimestamp >= :startDate")
    Double getTotalCostSince(@Param("startDate") LocalDateTime startDate);
}
'@

Write-Host "📝 Generating database migration..." -ForegroundColor Cyan

# Database Migration
$migrationContent = @'
-- Retargeting System Database Migration
-- V3__create_retargeting_tables.sql

-- Update pre_customers table for retargeting
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS unsubscribed_at TIMESTAMP NULL;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS last_retargeting_sent TIMESTAMP NULL;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS retargeting_week INTEGER DEFAULT 0;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS conversion_source VARCHAR(50) NULL;

-- Retargeting Templates Table
CREATE TABLE IF NOT EXISTS retargeting_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    subject_line VARCHAR(500),
    message_content TEXT NOT NULL,
    campaign_week INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    expected_conversion_rate DOUBLE,
    cost_per_message DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_type_week (template_type, campaign_week)
);

-- Retargeting Campaigns Table
CREATE TABLE IF NOT EXISTS retargeting_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pre_customer_id BIGINT NOT NULL,
    campaign_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    message_content TEXT,
    email_subject VARCHAR(500),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    delivery_status VARCHAR(100),
    external_message_id VARCHAR(255),
    error_message TEXT,
    campaign_week INTEGER,
    retry_count INTEGER DEFAULT 0,
    cost_incurred DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (pre_customer_id) REFERENCES pre_customers(id),
    INDEX idx_retargeting_status_scheduled (status, scheduled_at),
    INDEX idx_retargeting_pre_customer (pre_customer_id),
    INDEX idx_retargeting_external_id (external_message_id)
);

-- Retargeting Metrics Table
CREATE TABLE IF NOT EXISTS retargeting_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT,
    pre_customer_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    metadata TEXT,
    cost_incurred DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metrics_event_date (event_type, event_timestamp),
    INDEX idx_metrics_campaign (campaign_id),
    INDEX idx_metrics_pre_customer (pre_customer_id)
);

-- Insert Default Email Templates
INSERT IGNORE INTO retargeting_templates (template_name, template_type, subject_line, message_content, campaign_week, expected_conversion_rate, cost_per_message) VALUES
('week1_email', 'EMAIL', 
'Complete Your Rega Billing Setup - Only ₹499!', 
'<html><body><h2>Hi {{firstName}},</h2><p>Complete your Rega Billing setup for just ₹499!</p><p>You signed up {{daysSinceSignup}} days ago.</p><a href="https://regabilling.com/complete-payment?email={{email}}">Complete Payment Now</a></body></html>', 
1, 0.12, 0.50),

('week2_email', 'EMAIL', 
'Join 2,500+ Businesses Using Rega Billing!', 
'<html><body><h2>Hi {{firstName}},</h2><p>Don''t let manual billing slow your growth. Join 2,500+ businesses!</p><a href="https://regabilling.com/complete-payment?email={{email}}">Start Now - ₹499</a></body></html>', 
2, 0.08, 0.50);

-- Insert Default SMS Templates  
INSERT IGNORE INTO retargeting_templates (template_name, template_type, message_content, campaign_week, expected_conversion_rate, cost_per_message) VALUES
('week2_sms', 'SMS', 
'Hi {{firstName}}! Complete your Rega Billing setup for ₹499. Join 2,500+ businesses: https://regabilling.com/pay?email={{email}}', 
2, 0.06, 3.00);
'@

Write-Host "💾 Writing all files to disk..." -ForegroundColor Yellow

# Write all files
try {
    # Entity files
    $preCustomerContent | Out-File -FilePath "$javaPath\entity\PreCustomer.java" -Encoding UTF8 -Force
    $retargetingCampaignContent | Out-File -FilePath "$javaPath\entity\RetargetingCampaign.java" -Encoding UTF8 -Force
    $retargetingTemplateContent | Out-File -FilePath "$javaPath\entity\RetargetingTemplate.java" -Encoding UTF8 -Force  
    $retargetingMetricsContent | Out-File -FilePath "$javaPath\entity\RetargetingMetrics.java" -Encoding UTF8 -Force

    # Repository files
    $campaignRepositoryContent | Out-File -FilePath "$javaPath\repository\RetargetingCampaignRepository.java" -Encoding UTF8 -Force
    $templateRepositoryContent | Out-File -FilePath "$javaPath\repository\RetargetingTemplateRepository.java" -Encoding UTF8 -Force
    $metricsRepositoryContent | Out-File -FilePath "$javaPath\repository\RetargetingMetricsRepository.java" -Encoding UTF8 -Force

    # Database migration
    $migrationContent | Out-File -FilePath "$resourcesPath\db\migration\V3__create_retargeting_tables.sql" -Encoding UTF8 -Force

    Write-Host ""
    Write-Host "🎉 SUCCESS! Retargeting System Core Files Generated!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📁 Files Created:" -ForegroundColor Cyan
    Write-Host "   📦 4 Entity classes (PreCustomer, RetargetingCampaign, RetargetingTemplate, RetargetingMetrics)" -ForegroundColor White
    Write-Host "   🗄️  3 Repository interfaces" -ForegroundColor White  
    Write-Host "   📊 1 Database migration with templates" -ForegroundColor White
    Write-Host ""
    Write-Host "🚀 READY TO TEST!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Next Steps:" -ForegroundColor Yellow
    Write-Host "   1. mvn clean install" -ForegroundColor White
    Write-Host "   2. mvn spring-boot:run" -ForegroundColor White
    Write-Host "   3. Check H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
    Write-Host ""
    Write-Host "🎯 Core Features Added:" -ForegroundColor Cyan
    Write-Host "   ✅ Retargeting database schema" -ForegroundColor Green
    Write-Host "   ✅ Campaign and template entities" -ForegroundColor Green
    Write-Host "   ✅ Metrics tracking foundation" -ForegroundColor Green
    Write-Host "   ✅ Sample email templates" -ForegroundColor Green

} catch {
    Write-Host "❌ Error writing files: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ Core retargeting files generated successfully!" -ForegroundColor Green
Write-Host "🎯 Your Rega Billing Solutions now has the retargeting foundation!" -ForegroundColor Green

