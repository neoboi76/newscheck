package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;

import java.util.List;

/**
 * Abstraction for external news source clients (OCP / DIP).
 *
 * Each implementation fetches articles from a specific news API.
 * The scheduler depends only on this interface, so adding a new
 * source (e.g., Reuters, NYT) requires only creating a new @Service
 * class that implements this interface — no scheduler changes needed.
 */
public interface NewsSourceClient {

    /**
     * Human-readable name of the source (for logging).
     */
    String getSourceName();

    /**
     * Fetch all available articles from this source.
     * Implementations handle their own category/section iteration
     * and parallelism internally.
     *
     * @return list of article events (never null, may be empty)
     */
    List<ArticleEvent> fetchAll();
}

