package com.newscheck.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newscheck.app.data.repository.AuthRepository
import com.newscheck.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(username, password)) {
                is Result.Success -> _authState.value = AuthState.Success(result.data.username)
                is Result.Error   -> _authState.value = AuthState.Error(result.message)
                else -> {}
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }
        if (password.length < 8) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.register(username, email, password)) {
                is Result.Success -> _authState.value = AuthState.Success(result.data.username)
                is Result.Error   -> _authState.value = AuthState.Error(result.message)
                else -> {}
            }
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val username: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}