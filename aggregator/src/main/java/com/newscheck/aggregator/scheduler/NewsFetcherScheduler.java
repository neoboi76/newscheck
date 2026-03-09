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

// Periodic fetch pipeline — all NewsSourceClient impls run in parallel
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsFetcherScheduler {

    private final List<NewsSourceClient> newsSourceClients;
    private final ArticleService         articleService;

    @Scheduled(cron = "${aggregator.fetch.cron:0 0/15 * * * *}")
    public void fetchAndPublish() {
        log.info("=== News fetch cycle starting ({} sources) ===", newsSourceClients.size());

        List<CompletableFuture<List<ArticleEvent>>> futures = newsSourceClients.stream()
                .map(client -> CompletableFuture
                        .supplyAsync(client::fetchAll)
                        .exceptionally(ex -> {
                            log.error("{} fetch failed: {}", client.getSourceName(), ex.getMessage(), ex);
                            return List.of();
                        }))
                .toList();

        List<ArticleEvent> allEvents = futures.stream()
                .map(CompletableFuture::join)
                .peek(articles -> log.info("Source fetched: {} articles", articles.size()))
                .flatMap(List::stream)
                .toList();

        int ingested = articleService.ingest(allEvents);
        log.info("=== News fetch cycle complete: {}/{} new articles ingested ===",
                 ingested, allEvents.size());
    }

    // Run once on startup so DB/Kafka are populated immediately
    @Scheduled(initialDelay = 5_000, fixedDelay = Long.MAX_VALUE)
    public void fetchOnStartup() {
        log.info("Running initial news fetch on startup...");
        fetchAndPublish();
    }
}
