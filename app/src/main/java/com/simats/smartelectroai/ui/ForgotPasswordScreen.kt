package com.simats.smartelectroai.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.ForgotPasswordRequest
import com.simats.smartelectroai.api.ResetPasswordRequest
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onResetSuccess: () -> Unit) {
    val context = LocalContext.current

    // 1: Email, 2: OTP Entry, 3: New Password Setup
    var step by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Custom back logic to go to previous step instead of quitting immediately
                        if (step > 1) step -= 1 else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Dynamic Icon based on step
            val icon = if (step == 1) Icons.Default.LockReset else Icons.Default.MarkEmailRead
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Title
            val title = when (step) {
                1 -> "Reset Password"
                2 -> "Check Your Email"
                else -> "Create New Password"
            }
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // STEP 1: ENTER EMAIL
            // ==========================================
            if (step == 1) {
                Text("Enter your registered email address. We will send you a 6-digit verification code.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(text = "Send OTP", isLoading = isLoading) {
                    if (email.isNotBlank()) {
                        isLoading = true
                        val req = ForgotPasswordRequest(email.trim())
                        RetrofitClient.instance.forgotPassword(req).enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                isLoading = false
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show()
                                    step = 2
                                } else {
                                    Toast.makeText(context, response.body()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                isLoading = false
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            }
            // ==========================================
            // STEP 2: MODERN OTP INPUT
            // ==========================================
            else if (step == 2) {
                Text("We've sent a 6-digit code to \n${email}", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                ModernOtpInput(otp = otp, onOtpChange = { otp = it })

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(text = "Verify Code", isLoading = false) {
                    if (otp.length == 6) {
                        step = 3 // Move to password setup
                    } else {
                        Toast.makeText(context, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // ==========================================
            // STEP 3: NEW PASSWORD & CONFIRM
            // ==========================================
            else {
                Text("Your new password must be different from previously used passwords.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it },
                    label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val img = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(img, null) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val img = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(img, null) }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(text = "Reset Password", isLoading = isLoading) {
                    if (newPassword.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    } else if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        val req = ResetPasswordRequest(email.trim(), otp.trim(), newPassword.trim())
                        RetrofitClient.instance.resetPassword(req).enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                isLoading = false
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "Password Updated! Please login.", Toast.LENGTH_LONG).show()
                                    onResetSuccess()
                                } else {
                                    Toast.makeText(context, response.body()?.message ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                                    // If OTP is invalid, send them back to step 2 to fix it
                                    if (response.body()?.message?.contains("OTP") == true) step = 2
                                }
                            }
                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                isLoading = false
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }
    }
}

// ==========================================
// MODERN 6-DIGIT OTP COMPOSABLE
// ==========================================
@Composable
fun ModernOtpInput(otp: String, onOtpChange: (String) -> Unit) {
    BasicTextField(
        value = otp,
        onValueChange = {
            if (it.length <= 6 && it.all { char -> char.isDigit() }) onOtpChange(it)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                repeat(6) { index ->
                    val char = when {
                        index >= otp.length -> ""
                        else -> otp[index].toString()
                    }
                    val isFocused = otp.length == index

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }
        }
    )
}

// ==========================================
// REUSABLE BUTTON COMPOSABLE
// ==========================================
@Composable
fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        enabled = !isLoading
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        else Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}