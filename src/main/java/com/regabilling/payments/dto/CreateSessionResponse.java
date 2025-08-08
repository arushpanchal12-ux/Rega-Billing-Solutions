package com.regabilling.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSessionResponse {
    private String provider;
    private String paymentSessionId;
    private String publicKey;
    private String orderOrIntentId;
    private String currency;
    private String amount;
    private String checkoutUrl;
    private String message;
}
