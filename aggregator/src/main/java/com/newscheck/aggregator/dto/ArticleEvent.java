package com.newscheck.aggregator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;

// Kafka message payload — must stay in sync with news-server's version
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
