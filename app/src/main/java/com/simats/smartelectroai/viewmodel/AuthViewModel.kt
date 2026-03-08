package com.simats.smartelectroai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.smartelectroai.api.AuthResponse
import com.simats.smartelectroai.api.LoginRequest
import com.simats.smartelectroai.api.RegisterRequest
import com.simats.smartelectroai.repository.AuthRepository
import com.simats.smartelectroai.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _signInState = MutableStateFlow<UiState<AuthResponse>>(UiState.Idle)
    val signInState: StateFlow<UiState<AuthResponse>> = _signInState

    private val _registerState = MutableStateFlow<UiState<AuthResponse>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthResponse>> = _registerState

    private val _otpState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val otpState: StateFlow<UiState<String>> = _otpState

    // Temporary storage for passing data between screens
    var pendingRegisterRequest: RegisterRequest? = null

    fun login(request: LoginRequest) {
        _signInState.value = UiState.Loading
        viewModelScope.launch {
            repository.login(request).fold(
                onSuccess = { _signInState.value = UiState.Success(it) },
                onFailure = { _signInState.value = UiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun initiateRegistration(request: RegisterRequest) {
        pendingRegisterRequest = request
        _otpState.value = UiState.Loading
        viewModelScope.launch {
            repository.sendOtp(request.email).fold(
                onSuccess = { _otpState.value = UiState.Success("OTP_SENT") },
                onFailure = { _otpState.value = UiState.Error(it.message ?: "Failed to send OTP") }
            )
        }
    }

    fun verifyOtpAndRegister(otp: String) {
        val request = pendingRegisterRequest ?: return
        _otpState.value = UiState.Loading

        viewModelScope.launch {
            repository.verifyOtp(request.email, otp).fold(
                onSuccess = {
                    repository.register(request).fold(
                        onSuccess = { _otpState.value = UiState.Success("REGISTER_SUCCESS") },
                        onFailure = { _otpState.value = UiState.Error(it.message ?: "Registration failed") }
                    )
                },
                onFailure = { _otpState.value = UiState.Error(it.message ?: "Invalid OTP") }
            )
        }
    }

    fun resetStates() {
        _signInState.value = UiState.Idle
        _registerState.value = UiState.Idle
        _otpState.value = UiState.Idle
    }
}