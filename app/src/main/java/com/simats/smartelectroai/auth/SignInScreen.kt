package com.simats.smartelectroai.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.simats.smartelectroai.R
import com.simats.smartelectroai.api.LoginRequest
import com.simats.smartelectroai.utils.UiState
import com.simats.smartelectroai.viewmodel.AuthViewModel

@Composable
fun SignInScreen(
    viewModel: AuthViewModel = viewModel(),
    onSignIn: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit
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
            .imePadding() // Keeps scrolling smooth when keyboard is open
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        // Top Row with the two College Logos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRQzSASJ8CW7h0pmb79FrMdRMp73kQ96SnFPg&s",
                contentDescription = "College Logo Left",
                modifier = Modifier.height(55.dp).widthIn(max = 110.dp),
                contentScale = ContentScale.Fit
            )

            AsyncImage(
                model = "https://simatscgpa.netlify.app/logo2.png",
                contentDescription = "College Logo Right",
                modifier = Modifier.height(55.dp).widthIn(max = 110.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 🚀 THE FIX: Box limits vertical space, but allows the Image to be huge!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp), // Strictly limits the layout space to remove the gap
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Main App Logo",
                modifier = Modifier.size(280.dp), // Makes the actual graphic massive
                contentScale = ContentScale.Fit
            )
        }

        // Text is now pulled straight up against the visible logo
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

        Spacer(modifier = Modifier.height(48.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = Color.Gray)
            TextButton(onClick = onSignUp) { Text("Sign Up", color = Color(0xFF2874F0), fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Powered by SIMATS Engineering",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}