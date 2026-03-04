package com.newscheck.aggregator.service;

import com.newscheck.aggregator.dto.ArticleEvent;
import com.newscheck.aggregator.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches articles from The Guardian Open Platform API.
 * Free tier: 500 requests/day, 200 results per page.
 * Endpoint: GET /search?section={section}&api-key={key}
 *
 * Guardian sections → NewsCheck categories mapping:
 *   technology  → technology
 *   sport       → sports
 *   business    → business
 *   film,music  → entertainment
 *   science     → science
 *   politics    → politics
 *   world       → general
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianApiClient {

    private final RestTemplate restTemplate;

    @Value("${guardian.key}")
    private String apiKey;

    @Value("${guardian.base-url}")
    private String baseUrl;

    private static final Map<String, String> SECTION_TO_CATEGORY = Map.of(
            "technology",   "technology",
            "sport",        "sports",
            "business",     "business",
            "culture",      "entertainment",
            "science",      "science",
            "politics",     "politics",
            "world",        "general",
            "us-news",      "general"
    );

    public List<ArticleEvent> fetchAllSections() {
        List<ArticleEvent> all = new ArrayList<>();
        for (Map.Entry<String, String> entry : SECTION_TO_CATEGORY.entrySet()) {
            try {
                all.addAll(fetchBySection(entry.getKey(), entry.getValue()));
            } catch (Exception e) {
                log.error("Guardian API error for section [{}]: {}", entry.getKey(), e.getMessage());
            }
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    public List<ArticleEvent> fetchBySection(String section, String category) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/search")
                .queryParam("section", section)
                .queryParam("show-fields", "headline,trailText,bodyText,thumbnail,byline")
                .queryParam("page-size", 50)
                .queryParam("order-by", "newest")
                .queryParam("api-key", apiKey)
                .toUriString();

        log.info("Fetching Guardian section: {}", section);
        Map<String, Object> root = restTemplate.getForObject(url, Map.class);
        if (root == null) return List.of();

        Map<String, Object> response = (Map<String, Object>) root.get("response");
        if (response == null || !"ok".equals(response.get("status"))) return List.of();

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.getOrDefault("results", List.of());

        List<ArticleEvent> events = new ArrayList<>();
        for (Map<String, Object> r : results) {
            try {
                events.add(mapToEvent(r, category));
            } catch (Exception e) {
                log.warn("Failed to map Guardian article: {}", e.getMessage());
            }
        }
        log.info("Guardian returned {} articles for section [{}]", events.size(), section);
        return events;
    }

    @SuppressWarnings("unchecked")
    private ArticleEvent mapToEvent(Map<String, Object> r, String category) {
        Map<String, Object> fields = (Map<String, Object>) r.getOrDefault("fields", Map.of());
        String articleUrl  = (String) r.getOrDefault("webUrl", "");
        String publishedAt = (String) r.get("webPublicationDate");

        Instant instant = publishedAt != null
                ? OffsetDateTime.parse(publishedAt).toInstant()
                : Instant.now();

        String headline = (String) fields.getOrDefault("headline",
                          (String) r.getOrDefault("webTitle", ""));

        return ArticleEvent.builder()
                .externalId(articleUrl)
                .title(headline)
                .description((String) fields.get("trailText"))
                .content((String) fields.get("bodyText"))
                .url(articleUrl)
                .imageUrl((String) fields.get("thumbnail"))
                .sourceName("The Guardian")
                .author((String) fields.get("byline"))
                .category(NewsCategory.fromString(category).getValue())
                .publishedAt(instant)
                .breaking(false)
                .build();
    }
}
