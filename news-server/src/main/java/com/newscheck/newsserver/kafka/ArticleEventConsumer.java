package com.newscheck.newsserver.kafka;

import com.newscheck.newsserver.dto.ArticleEvent;
import com.newscheck.newsserver.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes article events from ALL news Kafka topics.
 *
 * HOW KAFKA CONSUMERS WORK (explained):
 * ──────────────────────────────────────────────────────────────────────────
 * 1. We declare a @KafkaListener with a list of topic patterns.
 *    Spring Kafka manages the underlying consumer loop for us.
 *
 * 2. GROUP ID ("news-server-group"):
 *    Each instance of the News-Server joins the same consumer group.
 *    Kafka distributes topic partitions across group members so no
 *    two instances process the same message simultaneously.
 *    This gives us horizontal scalability for free.
 *
 * 3. OFFSET MANAGEMENT (manual ACK):
 *    We use AckMode.MANUAL_IMMEDIATE so we only commit the offset
 *    AFTER we have successfully processed the message.
 *    If the service crashes mid-processing, the message is re-delivered
 *    on restart (at-least-once semantics).
 *
 * 4. AUTO-OFFSET-RESET = earliest:
 *    On first start (no committed offset yet), consume from the
 *    beginning of the topic, ensuring we don't miss articles published
 *    while the service was down.
 *
 * 5. The payload (ArticleEvent) is automatically deserialized from the
 *    JSON bytes Kafka stored, back into a Java object.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleEventConsumer {

    private final NotificationService notificationService;

    /**
     * Listens to all category topics.
     * topics = {news.general, news.technology, news.sports, ...}
     *
     * The breaking topic (news.breaking) is also consumed here.
     * Articles published there will additionally trigger high-priority
     * push notifications.
     */
    @KafkaListener(
        topics = {
            "news.general",
            "news.technology",
            "news.sports",
            "news.business",
            "news.entertainment",
            "news.health",
            "news.science",
            "news.politics",
            "news.breaking"
        },
        groupId    = "news-server-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ArticleEvent> record,
                        Acknowledgment ack) {
        ArticleEvent event = record.value();

        log.info("Consumed article [{}] from topic [{}] partition [{}] offset [{}]",
                 event != null ? event.getExternalId() : "null",
                 record.topic(),
                 record.partition(),
                 record.offset());

        try {
            if (event == null) {
                log.warn("Received null article event – skipping");
                ack.acknowledge();
                return;
            }

            // Send push notifications to subscribed users
            notificationService.notifySubscribers(event);

            // Acknowledge offset commit to Kafka
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing article event [{}]: {}",
                      event != null ? event.getExternalId() : "unknown", e.getMessage(), e);
            // DO NOT acknowledge – Kafka will re-deliver this message
            // In production, add a dead-letter topic after N retries
        }
    }
}
