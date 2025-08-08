package com.regabilling.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupDecoyResponse {
    private Long preCustomerId;
    private String paymentSessionId;
    private String provider;
    private String publicKey;
    private String orderOrIntentId;
    private String currency;
    private String amount;
    private String message;
}
