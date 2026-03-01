package com.newscheck.newsserver.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

// ────────────────────────────────────────────────────────────────────────────
// Auth
// ────────────────────────────────────────────────────────────────────────────

public class AuthDtos {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 100)
        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8, max = 100)
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TokenResponse {
        private String  token;
        private String  tokenType;   // "Bearer"
        private Long    userId;
        private String  username;
        private String  email;
        private Instant expiresAt;
    }
}

// ────────────────────────────────────────────────────────────────────────────
// User
// ────────────────────────────────────────────────────────────────────────────

class UserDtos {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserResponse {
        private Long         id;
        private String       username;
        private String       email;
        private List<String> subscriptions;
        private Instant      createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateFcmTokenRequest {
        @NotBlank private String fcmToken;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscribeRequest {
        @NotBlank private String category;
    }
}
