package com.newscheck.newsserver.controller;

import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * User profile and subscription management.
 *
 * ┌────────────────────────────────────────────────────────────┐
 * │ Endpoint                          │ Description            │
 * ├────────────────────────────────────────────────────────────┤
 * │ GET  /api/users/me                │ Get current user       │
 * │ PUT  /api/users/me/fcm-token      │ Update FCM token       │
 * │ GET  /api/users/me/subscriptions  │ List subscriptions     │
 * │ POST /api/users/me/subscriptions  │ Subscribe to category  │
 * │ DELETE /api/users/me/subs/{cat}   │ Unsubscribe            │
 * └────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getByUsername(principal.getUsername());
        List<String> subs = userService.getSubscribedCategories(user.getId());

        return ResponseEntity.ok(UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .subscriptions(subs)
                .createdAt(user.getCreatedAt())
                .build());
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<Map<String, String>> updateFcmToken(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody FcmTokenRequest req) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getByUsername(principal.getUsername());
        userService.updateFcmToken(user.getId(), req.getFcmToken());
        return ResponseEntity.ok(Map.of("status", "FCM token updated"));
    }

    @GetMapping("/me/subscriptions")
    public ResponseEntity<List<String>> getSubscriptions(
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getByUsername(principal.getUsername());
        return ResponseEntity.ok(userService.getSubscribedCategories(user.getId()));
    }

    @PostMapping("/me/subscriptions")
    public ResponseEntity<Map<String, String>> subscribe(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SubscribeRequest req) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getByUsername(principal.getUsername());
        userService.subscribe(user.getId(), req.getCategory());
        return ResponseEntity.ok(Map.of("status", "subscribed", "category", req.getCategory()));
    }

    @DeleteMapping("/me/subscriptions/{category}")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String category) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getByUsername(principal.getUsername());
        userService.unsubscribe(user.getId(), category);
        return ResponseEntity.ok(Map.of("status", "unsubscribed", "category", category));
    }

    // ── inner DTOs ────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class FcmTokenRequest {
        @NotBlank private String fcmToken;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscribeRequest {
        @NotBlank private String category;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserResponse {
        private Long         id;
        private String       username;
        private String       email;
        private List<String> subscriptions;
        private Instant      createdAt;
    }
}
