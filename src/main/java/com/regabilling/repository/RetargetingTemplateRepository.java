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
