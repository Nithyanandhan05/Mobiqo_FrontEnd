package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- LOCAL API MODEL ---
private data class ChangePasswordRequest(val current_password: String, val new_password: String)
private data class ChangePasswordResponse(val status: String, val message: String)

private interface ChangePasswordApiService {
    @PUT("/auth/change-password")
    fun changePassword(@Header("Authorization") token: String, @Body req: ChangePasswordRequest): Call<ChangePasswordResponse>
}

// --- COLORS ---
private val PrimaryBlue = Color(0xFF1976D2)
private val BgWhite = Color(0xFFFFFFFF)
private val TextMain = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    val api = remember {
        Retrofit.Builder()
            .baseUrl("http://10.156.35.203:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChangePasswordApiService::class.java)
    }

    fun handlePasswordChange() {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword != confirmPassword) {
            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        if (token.isNullOrEmpty()) return

        isLoading = true
        api.changePassword("Bearer $token", ChangePasswordRequest(currentPassword, newPassword)).enqueue(object : Callback<ChangePasswordResponse> {
            override fun onResponse(call: Call<ChangePasswordResponse>, response: Response<ChangePasswordResponse>) {
                isLoading = false
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    onBack()
                } else {
                    Toast.makeText(context, response.body()?.message ?: "Failed to update password", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold, color = TextMain) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Create a new strong password for your account to enhance security.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // Current Password
            PrivatePasswordTextField(
                value = currentPassword, label = "Current Password", isVisible = currentPasswordVisible,
                onValueChange = { currentPassword = it }, onVisibilityChange = { currentPasswordVisible = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            PrivatePasswordTextField(
                value = newPassword, label = "New Password", isVisible = newPasswordVisible,
                onValueChange = { newPassword = it }, onVisibilityChange = { newPasswordVisible = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            PrivatePasswordTextField(
                value = confirmPassword, label = "Confirm New Password", isVisible = confirmPasswordVisible,
                onValueChange = { confirmPassword = it }, onVisibilityChange = { confirmPasswordVisible = it }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { handlePasswordChange() },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("UPDATE PASSWORD", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PrivatePasswordTextField(
    value: String, label: String, isVisible: Boolean,
    onValueChange: (String) -> Unit, onVisibilityChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { onVisibilityChange(!isVisible) }) { Icon(imageVector = image, contentDescription = null) }
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
            unfocusedIndicatorColor = Color(0xFFE0E0E0), focusedIndicatorColor = PrimaryBlue
        )
    )
}