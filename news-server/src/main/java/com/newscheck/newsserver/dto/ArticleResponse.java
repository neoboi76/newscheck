package com.newscheck.newsserver.dto;

import lombok.*;

import java.time.Instant;

/**
 * The article shape returned to the Android app.
 * Does not include the full article content to keep payload small –
 * clients fetch the full article via the /articles/{id} endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
    private Long    id;
    private String  title;
    private String  description;
    private String  url;
    private String  imageUrl;
    private String  sourceName;
    private String  author;
    private String  category;
    private Instant publishedAt;
    private boolean breaking;
    private boolean read;   // per-user, populated by ArticleService
}
