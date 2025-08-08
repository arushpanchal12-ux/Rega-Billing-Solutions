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
