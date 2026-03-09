package com.newscheck.app.data.repository

import com.newscheck.app.data.api.NewsCheckApi
import com.newscheck.app.data.model.LoginRequest
import com.newscheck.app.data.model.RegisterRequest
import com.newscheck.app.data.model.TokenResponse
import com.newscheck.app.utils.TokenManager
import com.newscheck.app.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: NewsCheckApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveSession(body.token, body.userId, body.username, body.email)
                Result.Success(body)
            } else {
                val msg = when (response.code()) {
                    401  -> "Invalid username or password"
                    else -> "Login failed (${response.code()})"
                }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveSession(body.token, body.userId, body.username, body.email)
                Result.Success(body)
            } else {
                val msg = when (response.code()) {
                    409  -> "Username or email already taken"
                    400  -> "Please check your input"
                    else -> "Registration failed (${response.code()})"
                }
                Result.Error(msg)
            }
        } catch (e: Exception) {
            Result.Error("Network error: ${e.message}")
        }
    }

    suspend fun logout() = tokenManager.clearSession()

    suspend fun isLoggedIn() = tokenManager.isLoggedIn()
}