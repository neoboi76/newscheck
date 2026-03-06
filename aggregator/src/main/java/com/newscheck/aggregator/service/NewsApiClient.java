package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Fetches articles from NewsAPI.org.
 *
 * Free tier limits: 100 requests/day, results limited to last 30 days.
 * Endpoint used: GET /v2/top-headlines?category={category}&apiKey={key}
 */
@Service
@Slf4j
public class NewsApiClient {

    private final RestTemplate restTemplate;
    private final ExecutorService fetchExecutor;

    @Value("${newsapi.key}")
    private String apiKey;

    @Value("${newsapi.base-url}")
    private String baseUrl;

    private static final String[] CATEGORIES = {
        "general", "technology", "sports", "business",
        "entertainment", "health", "science"
    };

    public NewsApiClient(RestTemplate restTemplate,
                         @Qualifier("fetchExecutor") ExecutorService fetchExecutor) {
        this.restTemplate = restTemplate;
        this.fetchExecutor = fetchExecutor;
    }

    /**
     * Fetches all categories in parallel.
     * Each category is fetched on the shared fetchExecutor thread pool.
     * If one category fails, the others still return their results.
     */
    public List<ArticleEvent> fetchAllCategories() {
        List<CompletableFuture<List<ArticleEvent>>> futures = Arrays.stream(CATEGORIES)
                .map(category -> CompletableFuture.supplyAsync(
                        () -> fetchByCategory(category), fetchExecutor)
                        .exceptionally(ex -> {
                            log.error("NewsAPI error for category [{}]: {}", category, ex.getMessage());
                            return List.of();
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ArticleEvent> fetchByCategory(String category) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/top-headlines")
                .queryParam("category", category)
                .queryParam("pageSize", 100)
                .queryParam("language", "en")
                .queryParam("apiKey", apiKey)
                .toUriString();

        log.info("Fetching NewsAPI category: {}", category);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !"ok".equals(response.get("status"))) {
            log.warn("NewsAPI returned non-ok status for category [{}]", category);
            return List.of();
        }

        List<Map<String, Object>> rawArticles =
                (List<Map<String, Object>>) response.getOrDefault("articles", List.of());

        List<ArticleEvent> events = new ArrayList<>();
        for (Map<String, Object> raw : rawArticles) {
            try {
                events.add(mapToEvent(raw, category));
            } catch (Exception e) {
                log.warn("Failed to map article: {}", e.getMessage());
            }
        }
        log.info("NewsAPI returned {} articles for category [{}]", events.size(), category);
        return events;
    }

    @SuppressWarnings("unchecked")
    private ArticleEvent mapToEvent(Map<String, Object> raw, String category) {
        Map<String, Object> source = (Map<String, Object>) raw.getOrDefault("source", Map.of());
        String articleUrl = (String) raw.getOrDefault("url", "");

        String publishedAtStr = (String) raw.get("publishedAt");
        Instant publishedAt = publishedAtStr != null
                ? OffsetDateTime.parse(publishedAtStr).toInstant()
                : Instant.now();

        String title = (String) raw.getOrDefault("title", "");
        boolean breaking = title != null && title.toLowerCase().contains("breaking");

        return ArticleEvent.builder()
                .externalId(articleUrl)                         // URL is the unique key
                .title(title)
                .description((String) raw.get("description"))
                .content((String) raw.get("content"))
                .url(articleUrl)
                .imageUrl((String) raw.get("urlToImage"))
                .sourceName((String) source.getOrDefault("name", "Unknown"))
                .author((String) raw.get("author"))
                .category(NewsCategory.fromString(category).getValue())
                .publishedAt(publishedAt)
                .breaking(breaking)
                .build();
    }
}
