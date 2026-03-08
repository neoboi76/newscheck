package com.newscheck.app.data.repository

import com.newscheck.app.data.api.NewsCheckApi
import com.newscheck.app.data.model.Article
import com.newscheck.app.data.model.PagedResponse
import com.newscheck.app.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val api: NewsCheckApi
) {
    suspend fun getFeed(page: Int = 0, size: Int = 20): Result<PagedResponse<Article>> =
        safeCall { api.getFeed(page, size) }

    suspend fun getAllArticles(page: Int = 0, size: Int = 20): Result<PagedResponse<Article>> =
        safeCall { api.getAllArticles(page, size) }

    suspend fun getBreaking(): Result<List<Article>> =
        safeCall { api.getBreaking() }

    suspend fun getByCategory(category: String, page: Int = 0, size: Int = 20): Result<PagedResponse<Article>> =
        safeCall { api.getByCategory(category, page, size) }

    suspend fun search(query: String, page: Int = 0, size: Int = 20): Result<PagedResponse<Article>> =
        safeCall { api.search(query, page, size) }

    suspend fun getById(id: Long): Result<Article> =
        safeCall { api.getArticleById(id) }

    suspend fun markRead(id: Long): Result<Unit> {
        return try {
            val response = api.markRead(id)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to mark as read")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }
}