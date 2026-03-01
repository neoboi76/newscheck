package com.newscheck.aggregator.kafka;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Wraps KafkaTemplate and handles publish-confirm logging.
 *
 * HOW KAFKA WORKS (brief):
 * ─────────────────────────
 * 1. A Kafka cluster holds named "topics" (e.g. "news.technology").
 * 2. Each topic is split into "partitions" for parallel throughput.
 * 3. A "producer" (this class) writes a record to a topic.
 *    We use the article's externalId as the message key so that all
 *    updates to the same article always go to the same partition
 *    (preserving order for that article).
 * 4. "Consumers" (News-Server) subscribe to topics and read records
 *    from their own offset, independently of other consumers.
 * 5. Kafka retains records for a configurable window (24h here).
 *    If the News-Server was down, it will catch up when it restarts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleEventProducer {

    private final KafkaTemplate<String, ArticleEvent> kafkaTemplate;

    /**
     * Publishes an article to the appropriate category topic.
     * If the article is also breaking news it is additionally published
     * to the dedicated breaking-news topic.
     *
     * @param event the article event to publish
     */
    public void publish(ArticleEvent event) {
        String topic = NewsCategory.fromString(event.getCategory()).toKafkaTopic();
        sendToTopic(topic, event);

        if (event.isBreaking()) {
            sendToTopic(NewsCategory.BREAKING.toKafkaTopic(), event);
        }
    }

    private void sendToTopic(String topic, ArticleEvent event) {
        // Message key = externalId → guarantees ordering per article inside a partition
        CompletableFuture<SendResult<String, ArticleEvent>> future =
                kafkaTemplate.send(topic, event.getExternalId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish article [{}] to topic [{}]: {}",
                          event.getExternalId(), topic, ex.getMessage());
            } else {
                log.debug("Published article [{}] to topic [{}] partition [{}] offset [{}]",
                          event.getExternalId(),
                          topic,
                          result.getRecordMetadata().partition(),
                          result.getRecordMetadata().offset());
            }
        });
    }
}
