package com.newscheck.newsserver.kafka;

import com.newscheck.newsserver.dto.ArticleEvent;
import com.newscheck.newsserver.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

// Consumes article events from all news Kafka topics, sends push notifications
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleEventConsumer {

    private final NotificationService notificationService;

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

            notificationService.notifySubscribers(event);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing article event [{}]: {}",
                      event != null ? event.getExternalId() : "unknown", e.getMessage(), e);
            // Don't ack — Kafka will re-deliver
        }
    }
}
