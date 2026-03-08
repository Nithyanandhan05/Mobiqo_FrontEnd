package com.simats.smartelectroai.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class OtpRequest(val email: String, val otp: String? = null)

interface AuthApi {
    @POST("/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<BaseResponse>

    @POST("/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<BaseResponse>

    @POST("/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<AuthResponse>
}