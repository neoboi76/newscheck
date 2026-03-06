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
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the periodic news-fetching pipeline.
 *
 * Schedule: every 15 minutes (configurable via aggregator.fetch.cron).
 *
 * Flow:
 *   NewsApiClient ──┐  (parallel)
 *                   ├──▶ ArticleService.ingest() ──▶ DB + Kafka
 *   GuardianClient──┘  (parallel)
 *
 * Both sources and their internal category fetches run in parallel.
 * If one source fails entirely, the other still contributes articles.
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

        // Fetch from both sources in parallel
        CompletableFuture<List<ArticleEvent>> newsApiFuture = CompletableFuture
                .supplyAsync(newsApiClient::fetchAllCategories)
                .exceptionally(ex -> {
                    log.error("NewsAPI fetch failed: {}", ex.getMessage(), ex);
                    return List.of();
                });

        CompletableFuture<List<ArticleEvent>> guardianFuture = CompletableFuture
                .supplyAsync(guardianApiClient::fetchAllSections)
                .exceptionally(ex -> {
                    log.error("Guardian fetch failed: {}", ex.getMessage(), ex);
                    return List.of();
                });

        // Wait for both to complete
        List<ArticleEvent> newsApiArticles = newsApiFuture.join();
        List<ArticleEvent> guardianArticles = guardianFuture.join();

        log.info("NewsAPI fetched: {} articles", newsApiArticles.size());
        log.info("Guardian fetched: {} articles", guardianArticles.size());

        List<ArticleEvent> allEvents = new ArrayList<>(newsApiArticles.size() + guardianArticles.size());
        allEvents.addAll(newsApiArticles);
        allEvents.addAll(guardianArticles);

        // Ingest (deduplicate + persist + publish to Kafka)
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
