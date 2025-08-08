package com.regabilling.payments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionRequest {
    
    @NotNull(message = "Pre-customer ID is required")
    private Long preCustomerId;
    
    private String provider;
    
    private String successUrl;
    
    private String cancelUrl;
}
