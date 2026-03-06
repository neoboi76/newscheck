package com.newscheck.newsserver.security;

import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated user from a Spring Security principal (SRP).
 *
 * Centralises the repetitive {@code principal → User / userId} lookup
 * that was duplicated across ArticleController and UserController.
 *
 * Controllers no longer need a direct dependency on UserService just
 * for identity resolution.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserService userService;

    /**
     * Resolves the full User entity from an @AuthenticationPrincipal.
     *
     * @param principal the authenticated principal (may be null for public endpoints)
     * @return the User entity, or null if principal is null
     */
    public User resolve(UserDetails principal) {
        if (principal == null) return null;
        return userService.getByUsername(principal.getUsername());
    }

    /**
     * Resolves just the user ID from an @AuthenticationPrincipal.
     * Useful for optional-auth endpoints where userId can be null.
     *
     * @param principal the authenticated principal (may be null)
     * @return the user ID, or null if principal is null
     */
    public Long resolveId(UserDetails principal) {
        User user = resolve(principal);
        return user != null ? user.getId() : null;
    }
}

