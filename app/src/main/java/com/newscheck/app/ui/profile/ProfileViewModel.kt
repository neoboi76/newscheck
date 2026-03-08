package com.newscheck.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newscheck.app.data.model.UserResponse
import com.newscheck.app.data.repository.AuthRepository
import com.newscheck.app.data.repository.UserRepository
import com.newscheck.app.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    fun loadProfile() {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            when (val result = userRepository.getMe()) {
                is Result.Success -> _profileState.value = ProfileState.Success(result.data)
                is Result.Error   -> _profileState.value = ProfileState.Error(result.message)
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutEvent.value = true
        }
    }

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val user: UserResponse) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}