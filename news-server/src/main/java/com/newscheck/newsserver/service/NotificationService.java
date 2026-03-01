package com.newscheck.newsserver.service;

import com.newscheck.newsserver.dto.ArticleEvent;
import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends Firebase Cloud Messaging (FCM) push notifications to Android devices.
 *
 * HOW FCM WORKS:
 * ──────────────────────────────────────────────────────────────────────────
 * 1. The Android app registers with FCM on startup and receives a unique
 *    FCM registration token.
 * 2. The app sends that token to our /api/users/me/fcm-token endpoint.
 *    We store it in the users.fcm_token column.
 * 3. When the News-Server receives a Kafka article event, it looks up all
 *    subscribers to that article's category who have a stored FCM token.
 * 4. It sends an HTTP POST to the FCM API with those tokens + a payload.
 * 5. FCM delivers the notification to the device even if the app is closed.
 *
 * NOTE: If FCM_SERVER_KEY is blank (default in dev), notifications are
 *       skipped and a log message is printed instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;
    private final RestTemplate   restTemplate;

    @Value("${fcm.server-key:}")
    private String fcmServerKey;

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

    /**
     * Notifies all subscribers of an article event.
     *
     * @param event the newly published article
     */
    public void notifySubscribers(ArticleEvent event) {
        if (fcmServerKey == null || fcmServerKey.isBlank()) {
            log.debug("FCM server key not configured – skipping push notification for: {}",
                      event.getTitle());
            return;
        }

        List<User> subscribers = userRepository
                .findSubscribersWithFcmToken(event.getCategory());

        if (subscribers.isEmpty()) return;

        log.info("Sending push notification for '{}' to {} subscribers",
                 event.getTitle(), subscribers.size());

        // Send in batches of 500 (FCM multicast limit)
        List<String> tokens = subscribers.stream()
                .map(User::getFcmToken)
                .toList();

        for (int i = 0; i < tokens.size(); i += 500) {
            List<String> batch = tokens.subList(i, Math.min(i + 500, tokens.size()));
            sendBatch(batch, event);
        }
    }

    private void sendBatch(List<String> tokens, ArticleEvent event) {
        Map<String, Object> notification = Map.of(
                "title", event.isBreaking() ? "🔴 Breaking: " + event.getTitle()
                                            : event.getTitle(),
                "body",  event.getDescription() != null ? event.getDescription() : "",
                "image", event.getImageUrl()     != null ? event.getImageUrl()    : ""
        );

        Map<String, Object> data = Map.of(
                "articleId",  String.valueOf(event.getArticleId()),
                "category",   event.getCategory(),
                "url",        event.getUrl(),
                "breaking",   String.valueOf(event.isBreaking())
        );

        Map<String, Object> body = Map.of(
                "registration_ids", tokens,
                "notification",     notification,
                "data",             data,
                "priority",         event.isBreaking() ? "high" : "normal"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    FCM_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("FCM batch sent successfully to {} devices", tokens.size());
            } else {
                log.warn("FCM returned non-2xx: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("FCM send failed: {}", e.getMessage());
        }
    }
}
