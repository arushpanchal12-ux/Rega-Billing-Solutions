package com.regabilling.auth;

import com.regabilling.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SignupDecoyService signupDecoyService;
    private final LoginService loginService;

    @PostMapping("/signup-decoy")
    public ResponseEntity<SignupDecoyResponse> signupDecoy(
            @Valid @RequestBody SignupDecoyRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Decoy signup request from IP: {} for email: {}", 
            httpRequest.getRemoteAddr(), request.getEmail());
        
        try {
            SignupDecoyResponse response = signupDecoyService.initiateSignup(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Signup failed: {}", e.getMessage());
            SignupDecoyResponse errorResponse = new SignupDecoyResponse();
            errorResponse.setMessage("Signup failed: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokensResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = httpRequest.getRemoteAddr();
        log.info("Login attempt from IP: {} for email: {}", ipAddress, request.getEmail());
        
        try {
            TokensResponse response = loginService.authenticateUser(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            TokensResponse errorResponse = new TokensResponse();
            errorResponse.setMessage("Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}
