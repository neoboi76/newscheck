package com.newscheck.newsserver.dto;

import lombok.*;

import java.time.Instant;

// Article shape returned to the Android app
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
    private boolean read;
}
