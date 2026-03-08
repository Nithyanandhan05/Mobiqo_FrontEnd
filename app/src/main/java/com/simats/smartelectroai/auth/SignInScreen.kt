package com.simats.smartelectroai.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image // 🚀 IMPORTED IMAGE FOR FULL COLOR LOGO
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.smartelectroai.R
import com.simats.smartelectroai.api.LoginRequest
import com.simats.smartelectroai.utils.UiState
import com.simats.smartelectroai.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    viewModel: AuthViewModel = viewModel(),
    onSignIn: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit // 🚀 ADDED FORGOT PASSWORD PARAMETER
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val signInState by viewModel.signInState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(signInState) {
        when (signInState) {
            is UiState.Success -> {
                val data = (signInState as UiState.Success).data
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                    .putString("jwt_token", data.token)
                    .putBoolean("is_admin", data.is_admin ?: false)
                    .apply()
                Toast.makeText(context, "Welcome ${data.user_name}!", Toast.LENGTH_SHORT).show()
                viewModel.resetStates()
                onSignIn(data.is_admin ?: false)
            }
            is UiState.Error -> {
                Toast.makeText(context, (signInState as UiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // 🚀 CHANGED TO IMAGE AND INCREASED SIZE TO 180.dp
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Text("Sign in to continue", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                }
            }
        )

        // 🚀 FORGOT PASSWORD BUTTON ADDED HERE
        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?", color = Color(0xFF2874F0), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(LoginRequest(email.trim(), password.trim())) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2874F0)),
            enabled = email.isNotBlank() && password.isNotBlank() && signInState !is UiState.Loading
        ) {
            if (signInState is UiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Or sign in with", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = {
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("jwt_token", "")
                val isAdmin = prefs.getBoolean("is_admin", false)
                if (token.isNullOrEmpty() || activity == null) {
                    Toast.makeText(context, "Login with password first", Toast.LENGTH_SHORT).show()
                } else {
                    val executor = ContextCompat.getMainExecutor(activity)
                    BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onSignIn(isAdmin)
                        }
                    }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Login").setNegativeButtonText("Cancel").build())
                }
            },
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFF1F3F6), CircleShape)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = Color(0xFF2874F0), modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = Color.Gray)
            TextButton(onClick = onSignUp) { Text("Sign Up", color = Color(0xFF2874F0), fontWeight = FontWeight.Bold) }
        }
    }
}