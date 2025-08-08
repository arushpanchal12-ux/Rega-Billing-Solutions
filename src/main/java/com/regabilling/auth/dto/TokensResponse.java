package com.regabilling.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokensResponse {
    private String accessToken;
    private String refreshToken;
    private Long deviceId;
    private long expiresIn;
    private String tokenType = "Bearer";
    private String message;
}
