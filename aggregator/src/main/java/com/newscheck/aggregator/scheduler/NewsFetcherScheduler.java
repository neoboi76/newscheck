package com.newscheck.aggregator.scheduler;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.service.ArticleService;
import com.newscheck.aggregator.service.NewsSourceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrates the periodic news-fetching pipeline.
 *
 * Schedule: every 15 minutes (configurable via aggregator.fetch.cron).
 *
 * Depends on the {@link NewsSourceClient} interface (OCP / DIP):
 * all registered implementations are auto-injected by Spring.
 * Adding a new source only requires creating a new @Service that
 * implements NewsSourceClient — this scheduler needs zero changes.
 *
 * Flow:
 *   NewsSourceClient[0] ──┐  (parallel)
 *   NewsSourceClient[1] ──┤
 *   NewsSourceClient[N] ──┘──▶ ArticleService.ingest() ──▶ DB + Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsFetcherScheduler {

    private final List<NewsSourceClient> newsSourceClients;
    private final ArticleService         articleService;

    @Scheduled(cron = "${aggregator.fetch.cron:0 0/15 * * * *}")
    public void fetchAndPublish() {
        log.info("=== News fetch cycle starting ({} sources) ===", newsSourceClients.size());

        // Fetch from all sources in parallel
        List<CompletableFuture<List<ArticleEvent>>> futures = newsSourceClients.stream()
                .map(client -> CompletableFuture
                        .supplyAsync(client::fetchAll)
                        .exceptionally(ex -> {
                            log.error("{} fetch failed: {}", client.getSourceName(), ex.getMessage(), ex);
                            return List.of();
                        }))
                .toList();

        // Collect results
        List<ArticleEvent> allEvents = futures.stream()
                .map(CompletableFuture::join)
                .peek(articles -> log.info("Source fetched: {} articles", articles.size()))
                .flatMap(List::stream)
                .toList();

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
