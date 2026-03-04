package com.newscheck.newsserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.Instant;

/**
 * Mirrors com.newscheck.aggregator.dto.ArticleEvent.
 * This is the Kafka message payload the Aggregator publishes
 * and this service consumes.
 *
 * Must stay in sync with the Aggregator's version.
 * In a larger system, extract this into a shared library (e.g. newscheck-common).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleEvent {
    private Long    articleId;
    private String  externalId;
    private String  title;
    private String  description;
    private String  content;
    private String  url;
    private String  imageUrl;
    private String  sourceName;
    private String  author;
    private String  category;
    private Instant publishedAt;
    private boolean breaking;
}
