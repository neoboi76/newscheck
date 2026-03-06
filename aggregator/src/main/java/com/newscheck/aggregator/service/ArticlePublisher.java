package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.kafka.ArticleEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Responsible for publishing persisted articles to Kafka (SRP).
 *
 * Separated from ArticleService so that:
 *   - ArticleService owns only persistence + deduplication logic
 *   - ArticlePublisher owns only event publishing logic
 *
 * This makes each class easier to test independently and gives
 * a single reason to change (persistence vs. messaging).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePublisher {

    private final ArticleEventProducer producer;

    /**
     * Publishes a list of newly persisted article events to Kafka.
     *
     * @param events the events to publish (must already have articleId set)
     */
    public void publishAll(List<ArticleEvent> events) {
        for (ArticleEvent event : events) {
            producer.publish(event);
        }
        log.info("Published {} article events to Kafka", events.size());
    }
}

