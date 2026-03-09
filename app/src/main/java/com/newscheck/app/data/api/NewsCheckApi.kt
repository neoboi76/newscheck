package com.newscheck.app.data.api

import com.newscheck.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NewsCheckApi {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<TokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<TokenResponse>

    // ── Articles ──────────────────────────────────────────────────────────────
    @GET("api/articles")
    suspend fun getAllArticles(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<Article>>

    @GET("api/articles/feed")
    suspend fun getFeed(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<Article>>

    @GET("api/articles/breaking")
    suspend fun getBreaking(): Response<List<Article>>

    @GET("api/articles/category/{category}")
    suspend fun getByCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<Article>>

    @GET("api/articles/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponse<Article>>

    @GET("api/articles/{id}")
    suspend fun getArticleById(@Path("id") id: Long): Response<Article>

    @POST("api/articles/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<Map<String, String>>

    // ── User ──────────────────────────────────────────────────────────────────
    @GET("api/users/me")
    suspend fun getMe(): Response<UserResponse>

    @PUT("api/users/me/fcm-token")
    suspend fun updateFcmToken(@Body req: FcmTokenRequest): Response<Map<String, String>>

    @GET("api/users/me/subscriptions")
    suspend fun getSubscriptions(): Response<List<String>>

    @POST("api/users/me/subscriptions")
    suspend fun subscribe(@Body req: SubscribeRequest): Response<Map<String, String>>

    @DELETE("api/users/me/subscriptions/{category}")
    suspend fun unsubscribe(@Path("category") category: String): Response<Map<String, String>>
}