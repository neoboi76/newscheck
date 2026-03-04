package com.newscheck.newsserver.service;

import com.newscheck.newsserver.dto.ArticleResponse;
import com.newscheck.newsserver.entity.Article;
import com.newscheck.newsserver.entity.ReadArticle;
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

import java.util.List;

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
        List<String> categories = subscriptionRepository.findByUserId(userId)
                .stream()
                .map(s -> s.getCategory())
                .toList();

        if (categories.isEmpty()) {
            // No subscriptions yet → return general feed
            return articleRepository
                    .findAllByOrderByPublishedAtDesc(pageable)
                    .map(a -> toResponse(a, userId));
        }

        return articleRepository
                .findUnreadForUser(userId, categories, pageable)
                .map(a -> toResponse(a, userId));
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> getByCategory(String category, Long userId,
                                                int page, int size) {
        return articleRepository
                .findByCategoryOrderByPublishedAtDesc(category, pageOf(page, size))
                .map(a -> toResponse(a, userId));
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> search(String query, Long userId, int page, int size) {
        return articleRepository
                .search(query, pageOf(page, size))
                .map(a -> toResponse(a, userId));
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> getBreakingNews(Long userId) {
        return articleRepository
                .findBreakingNews(PageRequest.of(0, 10))
                .stream()
                .map(a -> toResponse(a, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse getById(Long id, Long userId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found: " + id));
        return toResponse(article, userId);
    }

    @Transactional
    public void markRead(Long userId, Long articleId) {
        if (!readArticleRepository.existsByUserIdAndArticleId(userId, articleId)) {
            readArticleRepository.save(
                    ReadArticle.builder().userId(userId).articleId(articleId).build());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ArticleResponse toResponse(Article a, Long userId) {
        boolean read = userId != null &&
                       readArticleRepository.existsByUserIdAndArticleId(userId, a.getId());
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
