package com.newscheck.aggregator.scheduler;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.service.ArticleService;
import com.newscheck.aggregator.service.GuardianApiClient;
import com.newscheck.aggregator.service.NewsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the periodic news-fetching pipeline.
 *
 * Schedule: every 15 minutes (configurable via aggregator.fetch.cron).
 *
 * Flow:
 *   NewsApiClient ──┐
 *                   ├──▶ ArticleService.ingest() ──▶ DB + Kafka
 *   GuardianClient──┘
 *
 * Each source runs sequentially to stay within API rate limits.
 * If one source fails, the others still run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsFetcherScheduler {

    private final NewsApiClient    newsApiClient;
    private final GuardianApiClient guardianApiClient;
    private final ArticleService   articleService;

    @Scheduled(cron = "${aggregator.fetch.cron:0 0/15 * * * *}")
    public void fetchAndPublish() {
        log.info("=== News fetch cycle starting ===");
        List<ArticleEvent> allEvents = new ArrayList<>();

        // ── NewsAPI.org ──────────────────────────────────────────────────────
        try {
            List<ArticleEvent> newsApiArticles = newsApiClient.fetchAllCategories();
            allEvents.addAll(newsApiArticles);
            log.info("NewsAPI fetched: {} articles", newsApiArticles.size());
        } catch (Exception e) {
            log.error("NewsAPI fetch failed: {}", e.getMessage(), e);
        }

        // ── The Guardian ─────────────────────────────────────────────────────
        try {
            List<ArticleEvent> guardianArticles = guardianApiClient.fetchAllSections();
            allEvents.addAll(guardianArticles);
            log.info("Guardian fetched: {} articles", guardianArticles.size());
        } catch (Exception e) {
            log.error("Guardian fetch failed: {}", e.getMessage(), e);
        }

        // ── Ingest (deduplicate + persist + publish to Kafka) ─────────────────
        int ingested = articleService.ingest(allEvents);
        log.info("=== News fetch cycle complete: {}/{} new articles ingested ===",
                 ingested, allEvents.size());
    }

    /**
     * Also run once on startup so the DB/Kafka are populated immediately
     * rather than waiting for the first cron trigger.
     */
    @Scheduled(initialDelay = 5_000, fixedDelay = Long.MAX_VALUE)
    public void fetchOnStartup() {
        log.info("Running initial news fetch on startup...");
        fetchAndPublish();
    }
}
