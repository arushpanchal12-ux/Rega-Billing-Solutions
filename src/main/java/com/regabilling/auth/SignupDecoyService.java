package com.regabilling.auth;

import com.regabilling.auth.dto.SignupDecoyRequest;
import com.regabilling.auth.dto.SignupDecoyResponse;
import com.regabilling.auth.password.PasswordService;
import com.regabilling.entity.PreCustomer;
import com.regabilling.repository.PreCustomerRepository;
import com.regabilling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupDecoyService {
    
    private final PreCustomerRepository preCustomerRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Transactional
    public SignupDecoyResponse initiateSignup(SignupDecoyRequest request) {
        log.info("Initiating decoy signup for email: {}", request.getEmail());
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail()) || 
            preCustomerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email address is already registered");
        }
        
        // Create pre-customer record
        PreCustomer.PreCustomerBuilder builder = PreCustomer.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .marketingConsent(request.isMarketingConsent())
            .status(PreCustomer.PreCustomerStatus.PENDING_PAYMENT);
        
        // Hash password if provided
        if (StringUtils.hasText(request.getPassword())) {
            builder.password(passwordService.hashPassword(request.getPassword()));
        }
        
        PreCustomer preCustomer = preCustomerRepository.save(builder.build());
        
        log.info("Created pre-customer {} for email {}", preCustomer.getId(), request.getEmail());
        
        // Mock payment session response
        return new SignupDecoyResponse(
            preCustomer.getId(),
            "session_mock_" + preCustomer.getId(),
            "RAZORPAY",
            "rzp_test_mock_key",
            "order_mock_" + preCustomer.getId(),
            "INR",
            "49900",
            "Signup successful! Please complete payment to activate your account."
        );
    }
}
