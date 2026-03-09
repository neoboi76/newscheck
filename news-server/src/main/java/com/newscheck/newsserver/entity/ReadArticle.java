package com.newscheck.newsserver.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "read_articles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ReadArticle.ReadArticleId.class)
public class ReadArticle {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "read_at")
    @Builder.Default
    private Instant readAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadArticleId implements Serializable {
        private Long userId;
        private Long articleId;
    }
}
