package com.newscheck.newsserver.service;

import com.newscheck.newsserver.entity.Subscription;
import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.repository.SubscriptionRepository;
import com.newscheck.newsserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository         userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder        passwordEncoder;

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsername(username))
            throw new IllegalArgumentException("Username already taken: " + username);
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered: " + email);

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public Subscription subscribe(Long userId, String category) {
        if (subscriptionRepository.existsByUserIdAndCategory(userId, category))
            throw new IllegalArgumentException("Already subscribed to: " + category);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return subscriptionRepository.save(
                Subscription.builder().user(user).category(category).build());
    }

    @Transactional
    public void unsubscribe(Long userId, String category) {
        subscriptionRepository.deleteByUserIdAndCategory(userId, category);
    }

    @Transactional(readOnly = true)
    public List<String> getSubscribedCategories(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .stream()
                .map(Subscription::getCategory)
                .toList();
    }
}
