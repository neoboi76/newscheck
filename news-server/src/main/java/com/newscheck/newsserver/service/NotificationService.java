package com.newscheck.newsserver.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.newscheck.newsserver.dto.ArticleEvent;
import com.newscheck.newsserver.entity.User;
import com.newscheck.newsserver.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Sends Firebase Cloud Messaging (FCM) push notifications to Android devices
 * using the <b>FCM v1 API</b> via the Firebase Admin SDK.
 *
 * HOW IT WORKS:
 * ──────────────────────────────────────────────────────────────────────────
 * 1. On startup, the service reads a Google service-account JSON file and
 *    initialises the Firebase Admin SDK. The SDK handles OAuth2 token
 *    generation and refresh automatically.
 * 2. When a Kafka article event arrives, the service looks up all users
 *    subscribed to that article's category who have an FCM device token.
 * 3. It sends one FCM message per device token via the Admin SDK.
 *    (FCM v1 does NOT support multicast "registration_ids" — the SDK's
 *     sendEach() helper sends individual messages in one HTTP batch.)
 * 4. FCM delivers the notification to the device even if the app is closed.
 *
 * CONFIGURATION:
 *   fcm.service-account-path  → path to the JSON file inside the container
 *   fcm.project-id            → Firebase project ID (from the JSON file)
 *
 * If the path is blank, push notifications are silently skipped (dev mode).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserRepository userRepository;

    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    @Value("${fcm.project-id:}")
    private String projectId;

    private boolean fcmInitialised = false;

    /**
     * Initialise Firebase Admin SDK on startup.
     * If the service-account JSON is not provided, FCM is disabled (dev mode).
     */
    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FCM service-account path not configured – push notifications DISABLED. "
                   + "Set FCM_SERVICE_ACCOUNT_PATH to enable.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(fis))
                    .setProjectId(projectId != null && !projectId.isBlank() ? projectId : null)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            fcmInitialised = true;
            log.info("✅ Firebase Admin SDK initialised – push notifications ENABLED (project: {})",
                     projectId);
        } catch (IOException e) {
            log.error("❌ Failed to initialise Firebase Admin SDK from [{}]: {}",
                      serviceAccountPath, e.getMessage());
        }
    }

    /**
     * Notifies all subscribers of an article event.
     *
     * @param event the newly published article
     */
    public void notifySubscribers(ArticleEvent event) {
        if (!fcmInitialised) {
            log.debug("FCM not initialised – skipping push notification for: {}",
                      event.getTitle());
            return;
        }

        List<User> subscribers = userRepository
                .findSubscribersWithFcmToken(event.getCategory());

        if (subscribers.isEmpty()) return;

        log.info("Sending FCM v1 push for '{}' to {} subscribers",
                 event.getTitle(), subscribers.size());

        List<String> tokens = subscribers.stream()
                .map(User::getFcmToken)
                .toList();

        // Build one Message per token (FCM v1 requirement)
        List<Message> messages = tokens.stream()
                .map(token -> buildMessage(token, event))
                .toList();

        // sendEach() sends up to 500 messages in a single HTTP batch
        for (int i = 0; i < messages.size(); i += 500) {
            List<Message> batch = messages.subList(i, Math.min(i + 500, messages.size()));
            sendBatch(batch);
        }
    }

    private Message buildMessage(String token, ArticleEvent event) {
        String title = event.isBreaking()
                ? "🔴 Breaking: " + event.getTitle()
                : event.getTitle();
        String body = event.getDescription() != null ? event.getDescription() : "";
        String imageUrl = event.getImageUrl() != null ? event.getImageUrl() : null;

        // Build the notification (shown in system tray)
        Notification.Builder notifBuilder = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null && !imageUrl.isBlank()) {
            notifBuilder.setImage(imageUrl);
        }

        // Build Android-specific config (priority)
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(event.isBreaking()
                        ? AndroidConfig.Priority.HIGH
                        : AndroidConfig.Priority.NORMAL)
                .build();

        // Build the data payload (for deep-linking in the app)
        Map<String, String> data = Map.of(
                "articleId", String.valueOf(event.getArticleId()),
                "category",  event.getCategory(),
                "url",       event.getUrl(),
                "breaking",  String.valueOf(event.isBreaking())
        );

        return Message.builder()
                .setToken(token)
                .setNotification(notifBuilder.build())
                .setAndroidConfig(androidConfig)
                .putAllData(data)
                .build();
    }

    private void sendBatch(List<Message> messages) {
        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);
            log.info("FCM batch: {} sent, {} failed",
                     response.getSuccessCount(), response.getFailureCount());

            // Log individual failures for debugging
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        log.warn("FCM send failed for message [{}]: {}",
                                 i, responses.get(i).getException().getMessage());
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM batch send failed: {}", e.getMessage());
        }
    }
}
