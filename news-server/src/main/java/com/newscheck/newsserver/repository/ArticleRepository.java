package com.newscheck.newsserver.repository;

import com.newscheck.newsserver.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Page<Article> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);

    Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.breaking = true ORDER BY a.publishedAt DESC")
    List<Article> findBreakingNews(Pageable pageable);

    // Full-text search via PostgreSQL GIN index
    @Query(value = "SELECT * FROM articles " +
           "WHERE to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, '')) " +
           "    @@ plainto_tsquery('english', :q) " +
           "ORDER BY published_at DESC",
           countQuery = "SELECT COUNT(*) FROM articles " +
           "WHERE to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, '')) " +
           "    @@ plainto_tsquery('english', :q)",
           nativeQuery = true)
    Page<Article> search(@Param("q") String query, Pageable pageable);

    Optional<Article> findByExternalId(String externalId);

    // Unread articles in user's subscribed categories
    @Query("SELECT a FROM Article a WHERE a.category IN :categories " +
           "AND a.id NOT IN (SELECT r.articleId FROM ReadArticle r WHERE r.userId = :userId) " +
           "ORDER BY a.publishedAt DESC")
    Page<Article> findUnreadForUser(@Param("userId") Long userId,
                                    @Param("categories") List<String> categories,
                                    Pageable pageable);
}
