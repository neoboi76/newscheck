package com.newscheck.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "newscheck_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val TOKEN_KEY    = stringPreferencesKey("jwt_token")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_ID_KEY  = longPreferencesKey("user_id")
        private val EMAIL_KEY    = stringPreferencesKey("email")
    }

    suspend fun saveSession(token: String, userId: Long, username: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY]    = token
            prefs[USER_ID_KEY]  = userId
            prefs[USERNAME_KEY] = username
            prefs[EMAIL_KEY]    = email
        }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.first()[TOKEN_KEY]

    suspend fun getUsername(): String? =
        context.dataStore.data.first()[USERNAME_KEY]

    suspend fun getUserId(): Long? =
        context.dataStore.data.first()[USER_ID_KEY]

    suspend fun getEmail(): String? =
        context.dataStore.data.first()[EMAIL_KEY]

    suspend fun isLoggedIn(): Boolean =
        !getToken().isNullOrEmpty()

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
}