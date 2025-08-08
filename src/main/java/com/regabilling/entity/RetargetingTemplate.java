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
