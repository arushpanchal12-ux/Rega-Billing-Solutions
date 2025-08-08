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
