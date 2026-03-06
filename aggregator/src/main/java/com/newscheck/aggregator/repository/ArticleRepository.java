package com.newscheck.aggregator.repository;

import com.newscheck.aggregator.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    /**
     * Batch deduplication: returns external IDs (from the given collection)
     * that already exist in the DB. Single query replaces N individual
     * existsByExternalId calls.
     */
    @Query("SELECT a.externalId FROM Article a WHERE a.externalId IN :externalIds")
    Set<String> findExistingExternalIds(@Param("externalIds") Collection<String> externalIds);

    List<Article> findByCategoryOrderByPublishedAtDesc(String category);

    @Query("SELECT a FROM Article a WHERE a.publishedAt > :since ORDER BY a.publishedAt DESC")
    List<Article> findRecentArticles(@Param("since") Instant since);

    @Query("SELECT a FROM Article a WHERE a.breaking = true ORDER BY a.publishedAt DESC")
    List<Article> findBreakingNews();
}
