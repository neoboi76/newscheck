package com.newscheck.newsserver.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Read-only JPA mapping of the articles table (written by the Aggregator).
 * The News-Server never directly inserts into this table – it only reads.
 * Articles arrive via Kafka events; the entity is used for REST feed queries.
 */
@Entity
@Table(name = "articles")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 512)
    private String externalId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 1024)
    private String url;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "source_name")
    private String sourceName;

    private String author;

    @Column(length = 100)
    private String category;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @Column(name = "is_breaking")
    private boolean breaking;
}
