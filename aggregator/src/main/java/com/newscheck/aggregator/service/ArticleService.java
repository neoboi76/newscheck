package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.Article;
import com.newscheck.aggregator.kafka.ArticleEventProducer;
import com.newscheck.aggregator.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Core aggregation logic:
 *   1. Receives raw ArticleEvents from API clients.
 *   2. Deduplicates against the DB (by externalId / URL).
 *   3. Persists new articles.
 *   4. Publishes new articles to Kafka so the News-Server can consume them.
 *
 * Why Kafka here?
 * ──────────────────────────────────────────────────────────────────────────
 * Without Kafka the News-Server would have to poll the DB or this service's
 * REST API continuously to detect new articles – wasting resources and
 * introducing polling latency.
 *
 * With Kafka:
 *   • The Aggregator publishes once when an article is new.
 *   • The News-Server (and any future consumer) receives the event
 *     immediately and exactly once (idempotent producer + consumer groups).
 *   • Multiple News-Server instances can consume the same topics in
 *     parallel using a shared consumer-group without duplicates.
 *   • If the News-Server is temporarily down it automatically catches up
 *     when it restarts (Kafka retains messages for 24 h by default).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository     articleRepository;
    private final ArticleEventProducer  producer;

    /**
     * Processes a batch of events:
     *   - skips entries with blank externalId
     *   - batch-deduplicates against DB (1 query instead of N)
     *   - persists new articles
     *   - publishes new articles to Kafka
     *
     * @return number of new articles ingested
     */
    @Transactional
    public int ingest(List<ArticleEvent> events) {
        // 1. Filter out events with blank externalId
        List<ArticleEvent> valid = events.stream()
                .filter(e -> e.getExternalId() != null && !e.getExternalId().isBlank())
                .toList();

        if (valid.isEmpty()) return 0;

        // 2. Batch dedup: one query to find all existing externalIds
        List<String> allExternalIds = valid.stream()
                .map(ArticleEvent::getExternalId)
                .toList();
        Set<String> existingIds = articleRepository.findExistingExternalIds(allExternalIds);

        // 3. Persist + publish only new articles
        int newCount = 0;
        for (ArticleEvent event : valid) {
            if (existingIds.contains(event.getExternalId())) {
                log.debug("Duplicate – skipping: {}", event.getExternalId());
                continue;
            }

            Article saved = articleRepository.save(toEntity(event));
            event.setArticleId(saved.getId());
            producer.publish(event);
            newCount++;
        }
        log.info("Ingested {}/{} articles (duplicates skipped)", newCount, events.size());
        return newCount;
    }

    private Article toEntity(ArticleEvent e) {
        return Article.builder()
                .externalId(e.getExternalId())
                .title(e.getTitle())
                .description(e.getDescription())
                .content(e.getContent())
                .url(e.getUrl())
                .imageUrl(e.getImageUrl())
                .sourceName(e.getSourceName())
                .author(e.getAuthor())
                .category(e.getCategory())
                .publishedAt(e.getPublishedAt())
                .breaking(e.isBreaking())
                .build();
    }
}
