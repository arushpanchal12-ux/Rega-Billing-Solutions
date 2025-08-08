package com.regabilling.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupDecoyRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;
    
    private boolean marketingConsent = false;
    
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
}
