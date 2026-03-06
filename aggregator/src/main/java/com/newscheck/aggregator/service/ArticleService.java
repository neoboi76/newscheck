package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.Article;
import com.newscheck.aggregator.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Core aggregation logic: deduplication and persistence (SRP).
 *
 * Responsibilities (single reason to change):
 *   1. Validates and deduplicates incoming ArticleEvents against the DB.
 *   2. Persists new articles.
 *
 * Kafka publishing is delegated to {@link ArticlePublisher} so this
 * class has no dependency on messaging infrastructure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticlePublisher  articlePublisher;

    /**
     * Processes a batch of events:
     *   - skips entries with blank externalId
     *   - batch-deduplicates against DB (1 query instead of N)
     *   - persists new articles
     *   - delegates Kafka publishing to ArticlePublisher
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

        // 3. Persist only new articles
        List<ArticleEvent> newEvents = new ArrayList<>();
        for (ArticleEvent event : valid) {
            if (existingIds.contains(event.getExternalId())) {
                log.debug("Duplicate – skipping: {}", event.getExternalId());
                continue;
            }

            Article saved = articleRepository.save(toEntity(event));
            event.setArticleId(saved.getId());
            newEvents.add(event);
        }

        // 4. Publish all new articles to Kafka (delegated to ArticlePublisher)
        if (!newEvents.isEmpty()) {
            articlePublisher.publishAll(newEvents);
        }

        log.info("Ingested {}/{} articles (duplicates skipped)", newEvents.size(), events.size());
        return newEvents.size();
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
