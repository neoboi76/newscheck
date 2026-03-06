package com.newscheck.newsserver.security;

import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// Resolves @AuthenticationPrincipal → User/userId (shared by controllers)
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserService userService;

    // Returns User entity, or null if unauthenticated
    public User resolve(UserDetails principal) {
        if (principal == null) return null;
        return userService.getByUsername(principal.getUsername());
    }

    // Returns userId, or null if unauthenticated
    public Long resolveId(UserDetails principal) {
        User user = resolve(principal);
        return user != null ? user.getId() : null;
    }
}

