package com.newscheck.newsserver.controller;

import com.newscheck.newsserver.dto.ArticleResponse;
import com.newscheck.newsserver.security.AuthenticatedUserResolver;
import com.newscheck.newsserver.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Article feed endpoints (public GET + authenticated feed/mark-read)
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService           articleService;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                articleService.getFeed(null, page, size));
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<ArticleResponse>> getFeed(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = userResolver.resolveId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(
                articleService.getFeed(userId, page, size));
    }

    @GetMapping("/breaking")
    public ResponseEntity<List<ArticleResponse>> getBreaking(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                articleService.getBreakingNews(userResolver.resolveId(principal)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ArticleResponse>> getByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                articleService.getByCategory(category, userResolver.resolveId(principal), page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ArticleResponse>> search(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                articleService.search(q, userResolver.resolveId(principal), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                articleService.getById(id, userResolver.resolveId(principal)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        Long userId = userResolver.resolveId(principal);
        articleService.markRead(userId, id);
        return ResponseEntity.ok(Map.of("status", "marked as read"));
    }
}
