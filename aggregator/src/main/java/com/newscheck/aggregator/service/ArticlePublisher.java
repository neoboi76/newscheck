package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.kafka.ArticleEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

// Publishes persisted articles to Kafka (separated from ArticleService for SRP)
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePublisher {

    private final ArticleEventProducer producer;

    public void publishAll(List<ArticleEvent> events) {
        for (ArticleEvent event : events) {
            producer.publish(event);
        }
        log.info("Published {} article events to Kafka", events.size());
    }
}

