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
