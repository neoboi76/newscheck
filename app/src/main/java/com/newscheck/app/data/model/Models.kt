package com.newscheck.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ── Article ──────────────────────────────────────────────────────────────────
@Parcelize
data class Article(
    val id: Long,
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val sourceName: String?,
    val author: String?,
    val category: String,
    val publishedAt: String,
    val breaking: Boolean,
    val read: Boolean
) : Parcelable

// ── Paged response wrapper ────────────────────────────────────────────────────
data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val last: Boolean
)

// ── Auth ──────────────────────────────────────────────────────────────────────
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val token: String,
    val tokenType: String,
    val userId: Long,
    val username: String,
    val email: String,
    val expiresAt: String
)

// ── User ──────────────────────────────────────────────────────────────────────
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val subscriptions: List<String>,
    val createdAt: String
)

data class SubscribeRequest(
    val category: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

// ── Categories ────────────────────────────────────────────────────────────────
enum class NewsCategory(val slug: String, val displayName: String, val emoji: String) {
    GENERAL("general", "General", "🌐"),
    TECHNOLOGY("technology", "Technology", "💻"),
    SPORTS("sports", "Sports", "⚽"),
    BUSINESS("business", "Business", "📈"),
    ENTERTAINMENT("entertainment", "Entertainment", "🎬"),
    HEALTH("health", "Health", "❤️"),
    SCIENCE("science", "Science", "🔬"),
    POLITICS("politics", "Politics", "🏛️");

    companion object {
        fun fromSlug(slug: String) = values().find { it.slug == slug } ?: GENERAL
    }
}