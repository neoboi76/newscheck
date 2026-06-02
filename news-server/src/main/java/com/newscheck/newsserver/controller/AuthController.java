package com.newscheck.newsserver.controller;

import com.newscheck.newsserver.dto.AuthDtos.*;
import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.security.JwtUtils;
import com.newscheck.newsserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

// Register + Login endpoints → returns JWT
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService           userService;
    private final AuthenticationManager authManager;
    private final JwtUtils              jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
            @Valid @RequestBody RegisterRequest req) {

        User user = userService.register(req.getUsername(),
                                          req.getEmail(),
                                          req.getPassword());

        UserDetails userDetails = org.springframework.security.core.userdetails
                .User.withUsername(user.getUsername())
                     .password(user.getPasswordHash())
                     .authorities("ROLE_USER")
                     .build();

        String token = jwtUtils.generateToken(userDetails);

        return ResponseEntity.ok(TokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .expiresAt(Instant.now().plusMillis(86_400_000))
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest req) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(),
                                                        req.getPassword()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);
        User user = userService.getByUsername(req.getUsername());

        return ResponseEntity.ok(TokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .expiresAt(Instant.now().plusMillis(86_400_000))
                .build());
    }
}
