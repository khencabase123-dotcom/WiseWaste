package com.example.wisewaste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wisewaste.AuthRepository
import com.example.wisewaste.Resource
import com.example.wisewaste.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val authState: StateFlow<Resource<FirebaseUser>?> = _authState.asStateFlow()

    private val _userData = MutableStateFlow<Resource<User>?>(null)
    val userData: StateFlow<Resource<User>?> = _userData.asStateFlow()

    val currentUser get() = authRepository.currentUser
    val authStateFlow = authRepository.authStateFlow

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            _authState.value = authRepository.login(email, password)
        }
    }

    fun register(email: String, password: String, username: String, barangay: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.register(email, password, username, barangay)
            when (result) {
                is Resource.Success -> _authState.value = authRepository.login(email, password)
                is Resource.Error -> _authState.value = Resource.Error(result.message ?: "Registration failed")
                else -> {}
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email)
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = null
        _userData.value = null
    }

    fun loadUserData() {
        viewModelScope.launch {
            _userData.value = Resource.Loading()
            _userData.value = authRepository.getCurrentUserData()
        }
    }

    fun updateProfile(username: String, barangay: String) {
        viewModelScope.launch {
            authRepository.updateProfile(username, barangay)
            loadUserData()
        }
    }

    fun resetAuthState() {
        _authState.value = null
    }
}