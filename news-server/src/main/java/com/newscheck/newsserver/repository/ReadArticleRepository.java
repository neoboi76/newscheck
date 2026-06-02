package com.newscheck.newsserver.repository;

import com.newscheck.newsserver.entity.ReadArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

@Repository
public interface ReadArticleRepository
        extends JpaRepository<ReadArticle, ReadArticle.ReadArticleId> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    // Batch read-status lookup (avoids N+1)
    @Query("SELECT r.articleId FROM ReadArticle r " +
           "WHERE r.userId = :userId AND r.articleId IN :articleIds")
    Set<Long> findReadArticleIds(@Param("userId") Long userId,
                                 @Param("articleIds") Collection<Long> articleIds);
}
