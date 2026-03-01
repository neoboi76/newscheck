package com.newscheck.newsserver.repository;

import com.newscheck.newsserver.entity.ReadArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadArticleRepository
        extends JpaRepository<ReadArticle, ReadArticle.ReadArticleId> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);
}
