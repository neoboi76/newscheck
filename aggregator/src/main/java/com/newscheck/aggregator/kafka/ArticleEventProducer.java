package com.newscheck.aggregator.kafka;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

// Publishes ArticleEvents to category Kafka topics
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleEventProducer {

    private final KafkaTemplate<String, ArticleEvent> kafkaTemplate;

    // Publishes to category topic + breaking topic if applicable
    public void publish(ArticleEvent event) {
        String topic = NewsCategory.fromString(event.getCategory()).toKafkaTopic();
        sendToTopic(topic, event);

        if (event.isBreaking()) {
            sendToTopic(NewsCategory.BREAKING.toKafkaTopic(), event);
        }
    }

    private void sendToTopic(String topic, ArticleEvent event) {
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
