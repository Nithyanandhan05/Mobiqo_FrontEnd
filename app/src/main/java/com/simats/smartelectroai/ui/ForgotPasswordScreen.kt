package com.simats.smartelectroai.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.simats.smartelectroai.utils.ValidationUtils // 🚀 IMPORTED VALIDATION UTILS
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Modern Theme Color
private val PrimaryBlue = Color(0xFF2874F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onResetSuccess: () -> Unit) {
    val context = LocalContext.current

    // 1: Email, 2: OTP Entry, 3: New Password Setup
    var step by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) } // 🚀 Email Validation State

    var otp by remember { mutableStateOf("") }

    var newPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) } // 🚀 Password Validation State

    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = {
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

            val icon = if (step == 1) Icons.Default.LockReset else Icons.Default.MarkEmailRead
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val title = when (step) {
                1 -> "Reset Password"
                2 -> "Check Your Email"
                else -> "Create New Password"
            }
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // STEP 1: ENTER EMAIL
            // ==========================================
            if (step == 1) {
                Text("Enter your registered email address. We will send you a 6-digit verification code.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))

                // 🚀 ADDED REAL-TIME EMAIL VALIDATION
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = if (ValidationUtils.isValidEmail(it)) null else "Must be @gmail.com, @email.com, @saveetha.com"
                    },
                    label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    singleLine = true
                )
                AnimatedVisibility(visible = emailError != null) {
                    Text(emailError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(text = "Send OTP", isLoading = isLoading) {
                    if (ValidationUtils.isValidEmail(email.trim())) {
                        isLoading = true
                        val req = ForgotPasswordRequest(email.trim())
                        RetrofitClient.instance.forgotPassword(req).enqueue(object : Callback<SimpleResponse> {
                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                isLoading = false
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show()
                                    step = 2
                                } else {
                                    Toast.makeText(context, "Email not found in our system", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                isLoading = false
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                    }
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

                // 🚀 ADDED REAL-TIME PASSWORD VALIDATION
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordError = if (ValidationUtils.isValidPassword(it)) null else "8-16 chars, 1 Upper, 1 Lower, 1 Num, 1 Spec Char"
                    },
                    label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError != null,
                    singleLine = true,
                    trailingIcon = {
                        val img = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(img, null) }
                    }
                )
                // 🚀 PASSWORD STRENGTH METER
                LinearProgressIndicator(
                    progress = { ValidationUtils.calculatePasswordStrength(newPassword) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = if (ValidationUtils.calculatePasswordStrength(newPassword) == 1f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                AnimatedVisibility(visible = passwordError != null) {
                    Text(passwordError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    singleLine = true,
                    trailingIcon = {
                        val img = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(img, null) }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(text = "Reset Password", isLoading = isLoading) {
                    if (passwordError != null || newPassword.isEmpty()) {
                        Toast.makeText(context, "Please enter a strong valid password", Toast.LENGTH_SHORT).show()
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
                                color = if (isFocused) PrimaryBlue else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
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
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        enabled = !isLoading
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        else Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}