package com.newscheck.aggregator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// Persisted article (external_id = dedup key)
@Entity
@Table(name = "articles",
       uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 1024)
    private String url;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "source_name")
    private String sourceName;

    private String author;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    @Builder.Default
    private Instant fetchedAt = Instant.now();

    @Column(name = "is_breaking", nullable = false)
    @Builder.Default
    private boolean breaking = false;
}
