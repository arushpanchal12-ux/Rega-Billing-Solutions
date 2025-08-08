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
