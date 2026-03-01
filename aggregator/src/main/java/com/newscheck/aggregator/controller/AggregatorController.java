package com.newscheck.aggregator.controller;

import com.newscheck.aggregator.entity.Article;
import com.newscheck.aggregator.repository.ArticleRepository;
import com.newscheck.aggregator.scheduler.NewsFetcherScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Internal REST endpoints for the Aggregator.
 * Not exposed to the Android app – only used for admin/debugging.
 *
 * In production, lock these down behind a firewall or basic auth.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class AggregatorController {

    private final ArticleRepository     articleRepository;
    private final NewsFetcherScheduler  scheduler;

    /** Manually trigger a fetch cycle (useful for testing). */
    @PostMapping("/fetch")
    public ResponseEntity<Map<String, String>> triggerFetch() {
        try {
            scheduler.fetchAndPublish();
            return ResponseEntity.ok(Map.of("status", "fetch triggered"));
        } catch (Exception e) {
            log.error("Fetch failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /** List articles ingested in the last N hours. */
    @GetMapping("/articles/recent")
    public List<Article> recentArticles(@RequestParam(defaultValue = "1") int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return articleRepository.findRecentArticles(since);
    }

    /** Total article count. */
    @GetMapping("/articles/count")
    public ResponseEntity<Map<String, Long>> count() {
        return ResponseEntity.ok(Map.of("total", articleRepository.count()));
    }
}
