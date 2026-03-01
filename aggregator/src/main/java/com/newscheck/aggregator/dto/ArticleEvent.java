package com.newscheck.aggregator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;

/**
 * The object that is serialised to JSON and published onto a Kafka topic.
 * The News-Server (consumer) deserialises this same class.
 *
 * Keep this class stable – changing field names is a breaking change for
 * all consumers unless you handle schema evolution explicitly.
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
