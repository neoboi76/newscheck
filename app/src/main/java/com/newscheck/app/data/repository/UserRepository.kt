package com.newscheck.app.data.repository

import com.newscheck.app.data.api.NewsCheckApi
import com.newscheck.app.data.model.SubscribeRequest
import com.newscheck.app.data.model.UserResponse
import com.newscheck.app.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: NewsCheckApi
) {
    suspend fun getMe(): Result<UserResponse> = safeCall { api.getMe() }

    suspend fun getSubscriptions(): Result<List<String>> = safeCall { api.getSubscriptions() }

    suspend fun subscribe(category: String): Result<Unit> {
        return try {
            val response = api.subscribe(SubscribeRequest(category))
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to subscribe")
        } catch (e: Exception) { Result.Error(e.message ?: "Unknown error") }
    }

    suspend fun unsubscribe(category: String): Result<Unit> {
        return try {
            val response = api.unsubscribe(category)
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error("Failed to unsubscribe")
        } catch (e: Exception) { Result.Error(e.message ?: "Unknown error") }
    }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) Result.Success(response.body()!!)
            else Result.Error("Error ${response.code()}: ${response.message()}")
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }
}