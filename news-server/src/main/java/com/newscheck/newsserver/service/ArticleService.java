package com.newscheck.newsserver.service;

import com.newscheck.newsserver.dto.ArticleResponse;
import com.newscheck.newsserver.entity.Article;
import com.newscheck.newsserver.entity.ReadArticle;
import com.newscheck.newsserver.exception.ResourceNotFoundException;
import com.newscheck.newsserver.repository.ArticleRepository;
import com.newscheck.newsserver.repository.ReadArticleRepository;
import com.newscheck.newsserver.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository      articleRepository;
    private final ReadArticleRepository  readArticleRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${news.default-page-size:20}")
    private int defaultPageSize;

    @Value("${news.max-page-size:100}")
    private int maxPageSize;

    @Transactional(readOnly = true)
    public Page<ArticleResponse> getFeed(Long userId, int page, int size) {
        Pageable pageable = pageOf(page, size);

        // Anonymous user → return general feed, no read-status needed
        if (userId == null) {
            return articleRepository
                    .findAllByOrderByPublishedAtDesc(pageable)
                    .map(a -> toResponse(a, false));
        }

        List<String> categories = subscriptionRepository.findByUserId(userId)
                .stream()
                .map(s -> s.getCategory())
                .toList();

        if (categories.isEmpty()) {
            Page<Article> articles = articleRepository
                    .findAllByOrderByPublishedAtDesc(pageable);
            return toResponsePage(articles, userId);
        }

        Page<Article> articles = articleRepository
                .findUnreadForUser(userId, categories, pageable);
        return toResponsePage(articles, userId);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> getByCategory(String category, Long userId,
                                                int page, int size) {
        Page<Article> articles = articleRepository
                .findByCategoryOrderByPublishedAtDesc(category, pageOf(page, size));
        return toResponsePage(articles, userId);
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> search(String query, Long userId, int page, int size) {
        Page<Article> articles = articleRepository
                .search(query, pageOf(page, size));
        return toResponsePage(articles, userId);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBreakingNews(Long userId) {
        List<Article> articles = articleRepository
                .findBreakingNews(PageRequest.of(0, 10));
        Set<Long> readIds = getReadIds(userId, articles);
        return articles.stream()
                .map(a -> toResponse(a, readIds.contains(a.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse getById(Long id, Long userId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        boolean read = userId != null &&
                readArticleRepository.existsByUserIdAndArticleId(userId, id);
        return toResponse(article, read);
    }

    @Transactional
    public void markRead(Long userId, Long articleId) {
        if (!readArticleRepository.existsByUserIdAndArticleId(userId, articleId)) {
            readArticleRepository.save(
                    ReadArticle.builder().userId(userId).articleId(articleId).build());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Batch-load read IDs for a page of articles, then map.
     * 1 SQL query for the entire page instead of N individual queries.
     */
    private Page<ArticleResponse> toResponsePage(Page<Article> articles, Long userId) {
        Set<Long> readIds = getReadIds(userId, articles.getContent());
        return articles.map(a -> toResponse(a, readIds.contains(a.getId())));
    }

    /**
     * Single batch query: returns the set of article IDs the user has read.
     * Returns empty set for anonymous users (userId == null).
     */
    private Set<Long> getReadIds(Long userId, List<Article> articles) {
        if (userId == null || articles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        return readArticleRepository.findReadArticleIds(userId, articleIds);
    }

    private ArticleResponse toResponse(Article a, boolean read) {
        return ArticleResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .url(a.getUrl())
                .imageUrl(a.getImageUrl())
                .sourceName(a.getSourceName())
                .author(a.getAuthor())
                .category(a.getCategory())
                .publishedAt(a.getPublishedAt())
                .breaking(a.isBreaking())
                .read(read)
                .build();
    }

    private Pageable pageOf(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), maxPageSize);
        return PageRequest.of(Math.max(page, 0), safeSize);
    }
}
