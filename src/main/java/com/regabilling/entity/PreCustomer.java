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
