package com.simats.smartelectroai.repository

import com.simats.smartelectroai.api.*
import org.json.JSONObject
import retrofit2.Response

class AuthRepository {
    private val api = RetrofitClient.authApi

    // Helper function to extract real error messages from Python (400/500 errors)
    private fun extractErrorMessage(response: Response<*>): String {
        return try {
            val errorStr = response.errorBody()?.string()
            if (!errorStr.isNullOrEmpty()) {
                JSONObject(errorStr).getString("message")
            } else {
                "Unknown error occurred"
            }
        } catch (e: Exception) {
            "Error processing response"
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = api.loginUser(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendOtp(email: String): Result<String> {
        return try {
            val response = api.sendOtp(OtpRequest(email))
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success("OTP Sent")
            } else {
                // 🚀 Now this will return "Email already exists in our system"
                Result.failure(Exception(extractErrorMessage(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<String> {
        return try {
            val response = api.verifyOtp(OtpRequest(email, otp))
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success("OTP Verified")
            } else {
                Result.failure(Exception(extractErrorMessage(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = api.registerUser(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(extractErrorMessage(response)))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}