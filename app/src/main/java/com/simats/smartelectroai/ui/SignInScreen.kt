package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.simats.smartelectroai.R
import com.simats.smartelectroai.api.AuthResponse
import com.simats.smartelectroai.api.LoginRequest
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Helper to launch Android's native fingerprint dialog
fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess() // Fingerprint matched!
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(activity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }
            override fun onAuthenticationFailed() {
                Toast.makeText(activity, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Mobiqo Login")
        .setSubtitle("Log in using your biometric credential")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onSignIn: (Boolean) -> Unit, // <--- FIXED: Now accepts the isAdmin boolean!
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // State Variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
            Text("Sign in to manage your warranties", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("EMAIL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                    focusedContainerColor = Color.Gray.copy(alpha = 0.1f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("PASSWORD") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide" else "Show")
                    }
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                    focusedContainerColor = Color.Gray.copy(alpha = 0.1f)
                )
            )
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot Password?", color = Color(0xFF2196F3))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                    // .trim() removes any accidental spaces Android added!
                    val loginRequest = LoginRequest(email.trim(), password.trim())
                    RetrofitClient.instance.loginUser(loginRequest).enqueue(object : Callback<AuthResponse> {
                        override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                            if (response.isSuccessful && response.body()?.status == "success") {
                                val token = response.body()?.token
                                val isAdmin = response.body()?.is_admin ?: false // <--- Extract Admin Flag

                                if (!token.isNullOrEmpty()) {
                                    // Save the token AND the admin flag for biometric login later
                                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("jwt_token", token)
                                        .putBoolean("is_admin", isAdmin)
                                        .apply()
                                }
                                Toast.makeText(context, "Welcome ${response.body()?.user_name}!", Toast.LENGTH_SHORT).show()

                                onSignIn(isAdmin) // <--- Pass the flag to MainActivity!
                            } else {
                                Toast.makeText(context, response.body()?.message ?: "Login Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("Login", fontSize = 16.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BIOMETRIC LOGIN BUTTON ---
        Text("Or sign in with", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        IconButton(
            onClick = {
                if (activity != null) {
                    val prefs = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("jwt_token", "")
                    val isAdminSaved = prefs.getBoolean("is_admin", false) // <--- Retrieve saved Admin status

                    if (token.isNullOrEmpty()) {
                        Toast.makeText(activity, "Please login with password first to enable biometrics.", Toast.LENGTH_LONG).show()
                    } else {
                        showBiometricPrompt(activity, onSuccess = {
                            Toast.makeText(activity, "Biometric Verification Successful!", Toast.LENGTH_SHORT).show()
                            onSignIn(isAdminSaved) // <--- Pass saved flag!
                        })
                    }
                }
            },
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFE3F2FD), CircleShape)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Login", tint = Color(0xFF1976D2), modifier = Modifier.size(32.dp))
        }

        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account?", color = Color.Gray)
            TextButton(onClick = onSignUp) {
                Text("Sign Up", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
        }
    }
}