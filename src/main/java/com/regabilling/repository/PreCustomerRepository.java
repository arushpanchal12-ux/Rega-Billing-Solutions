package com.regabilling.repository;

import com.regabilling.entity.PreCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreCustomerRepository extends JpaRepository<PreCustomer, Long> {
    
    Optional<PreCustomer> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    @Query("SELECT pc FROM PreCustomer pc WHERE pc.status = :status AND pc.createdAt <= :cutoffDate AND pc.unsubscribedAt IS NULL AND pc.marketingConsent = true AND pc.retargetingWeek < 4")
    List<PreCustomer> findEligibleForRetargeting(
        @Param("status") PreCustomer.PreCustomerStatus status,
        @Param("cutoffDate") LocalDateTime cutoffDate
    );
    
    @Query("SELECT COUNT(pc) FROM PreCustomer pc WHERE pc.status = :status AND pc.createdAt >= :startDate")
    Long countByStatusAndCreatedAtAfter(@Param("status") PreCustomer.PreCustomerStatus status, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT pc FROM PreCustomer pc WHERE pc.lastRetargetingSent IS NULL OR pc.lastRetargetingSent <= :lastSentBefore")
    List<PreCustomer> findDueForRetargeting(@Param("lastSentBefore") LocalDateTime lastSentBefore);
}
