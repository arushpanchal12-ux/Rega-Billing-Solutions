package com.regabilling.auth;

import com.regabilling.auth.dto.LoginRequest;
import com.regabilling.auth.dto.TokensResponse;
import com.regabilling.auth.password.PasswordService;
import com.regabilling.entity.User;
import com.regabilling.repository.UserRepository;
import com.regabilling.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    @Transactional
    public TokensResponse authenticateUser(LoginRequest request) {
        log.debug("Authentication attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        if (user.getPasswordHash() == null || 
            !passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password attempt for user: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }
        
        if (user.getStatus() != User.Status.ACTIVE_PAID) {
            throw new RuntimeException("Account access restricted. Please ensure payment is up to date.");
        }
        
        String accessToken = jwtService.generateAccessToken(
            user.getId(), 
            user.getEmail(), 
            user.getStatus().name()
        );
        
        log.info("Successful login for user: {}", user.getEmail());
        
        return new TokensResponse(
            accessToken,
            "refresh_mock_" + user.getId(),
            1L, // Mock device ID
            15 * 60, // 15 minutes
            "Bearer",
            "Login successful!"
        );
    }
}
