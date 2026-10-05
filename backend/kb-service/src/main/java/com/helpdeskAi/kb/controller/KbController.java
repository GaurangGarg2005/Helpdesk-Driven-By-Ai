package com.helpdeskAi.kb.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.UserPrincipal;
import com.helpdeskAi.kb.dto.*;
import com.helpdeskAi.kb.service.KbService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KbController {

    private final KbService kbService;

    // ── Categories (agent / admin) ─────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories() {
        return ok("Categories retrieved", kbService.listCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        return created("Category created", kbService.createCategory(req));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        kbService.deleteCategory(id);
        return ok("Category deleted", null);
    }

    // ── Categories (public) ────────────────────────────────────────────

    @GetMapping("/categories/public")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listPublicCategories(@RequestParam UUID orgId) {
        return ok("Categories retrieved", kbService.listPublicCategories(orgId));
    }

    // ── Articles (agent / admin) ───────────────────────────────────────

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> listArticles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok("Articles retrieved",
                kbService.listArticles(status, categoryId, PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(@PathVariable UUID id) {
        return ok("Article retrieved", kbService.getArticleById(id));
    }

    @PostMapping("/articles")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(@Valid @RequestBody CreateArticleRequest req) {
        UserPrincipal user = currentUser();
        ArticleResponse article = kbService.createArticle(req, user.getId(), user.getUsername());
        return created("Article created", article);
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable UUID id, @RequestBody CreateArticleRequest req) {
        return ok("Article updated", kbService.updateArticle(id, req));
    }

    @PostMapping("/articles/{id}/publish")
    public ResponseEntity<ApiResponse<ArticleResponse>> publishArticle(@PathVariable UUID id) {
        return ok("Article published", kbService.publishArticle(id));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable UUID id) {
        kbService.deleteArticle(id);
        return ok("Article deleted", null);
    }

    @GetMapping("/articles/featured")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getFeatured() {
        return ok("Featured articles retrieved", kbService.getFeaturedPublished());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> search(
            @RequestParam String q,
            @RequestParam(required = false) UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ArticleResponse> results = orgId != null
                ? kbService.searchPublic(orgId, q, PageRequest.of(page, size))
                : kbService.searchInternal(q, PageRequest.of(page, size));
        return ok("Search results", results);
    }

    // ── Public article by slug ─────────────────────────────────────────

    @GetMapping("/articles/public/{orgId}/{slug}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getPublicArticle(
            @PathVariable UUID orgId, @PathVariable String slug) {
        return ok("Article retrieved", kbService.getPublicArticleBySlug(orgId, slug));
    }

    // ── Feedback ───────────────────────────────────────────────────────

    @PostMapping("/articles/{id}/rate")
    public ResponseEntity<ApiResponse<Void>> rateArticle(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        kbService.rateArticle(id, Boolean.TRUE.equals(body.get("helpful")));
        return ok("Rating recorded", null);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private <T> ResponseEntity<ApiResponse<T>> ok(String msg, T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, msg, data, Instant.now()));
    }

    private <T> ResponseEntity<ApiResponse<T>> created(String msg, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, msg, data, Instant.now()));
    }

    private UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }
}
