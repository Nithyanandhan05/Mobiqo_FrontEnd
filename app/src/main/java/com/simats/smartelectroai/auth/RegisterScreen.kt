package com.simats.smartelectroai.ui.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.RegisterRequest
import com.simats.smartelectroai.utils.UiState
import com.simats.smartelectroai.utils.ValidationUtils
import com.simats.smartelectroai.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToOtp: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val otpState by viewModel.otpState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otpState) {
        if (otpState is UiState.Success && (otpState as UiState.Success).data == "OTP_SENT") {
            viewModel.resetStates()
            onNavigateToOtp()
        } else if (otpState is UiState.Error) {
            Toast.makeText(context, (otpState as UiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetStates()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // 🚀 ADDED: Top Row with the two College Logos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Logo
            AsyncImage(
                model = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRQzSASJ8CW7h0pmb79FrMdRMp73kQ96SnFPg&s",
                contentDescription = "College Logo Left",
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(max = 120.dp),
                contentScale = ContentScale.Fit
            )

            // Right Logo
            AsyncImage(
                model = "https://simatscgpa.netlify.app/logo2.png",
                contentDescription = "College Logo Right",
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(max = 120.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Text("Join the SmartElectro platform", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it; nameError = if (ValidationUtils.isValidFullName(it)) null else "Only letters & spaces allowed" },
            label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = nameError != null
        )
        // 🚀 FIXED: Added ?: "" to prevent NullPointerException
        AnimatedVisibility(visible = nameError != null) { Text(nameError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; emailError = if (ValidationUtils.isValidEmail(it)) null else "Must be @gmail.com, @email.com, @saveetha.com" },
            label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = emailError != null
        )
        // 🚀 FIXED: Added ?: "" to prevent NullPointerException
        AnimatedVisibility(visible = emailError != null) { Text(emailError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone, onValueChange = { phone = it; phoneError = if (ValidationUtils.isValidPhone(it)) null else "Must be exactly 10 digits" },
            label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), isError = phoneError != null
        )
        // 🚀 FIXED: Added ?: "" to prevent NullPointerException
        AnimatedVisibility(visible = phoneError != null) { Text(phoneError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it; passwordError = if (ValidationUtils.isValidPassword(it)) null else "8-16 chars, 1 Upper, 1 Lower, 1 Num, 1 Spec Char" },
            label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } }
        )
        LinearProgressIndicator(progress = { ValidationUtils.calculatePasswordStrength(password) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = if (ValidationUtils.calculatePasswordStrength(password) == 1f) Color(0xFF4CAF50) else Color(0xFFFF9800))

        // 🚀 FIXED: Added ?: "" to prevent NullPointerException
        AnimatedVisibility(visible = passwordError != null) { Text(passwordError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && confirmPassword != password
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nameError == null && emailError == null && phoneError == null && passwordError == null && confirmPassword == password && name.isNotBlank()) {
                    viewModel.initiateRegistration(RegisterRequest(name.trim(), email.trim(), phone.trim(), password))
                } else {
                    Toast.makeText(context, "Please fix the errors above", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2874F0)),
            enabled = otpState !is UiState.Loading
        ) {
            if (otpState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", color = Color.Gray)
            TextButton(onClick = onLoginClick) { Text("Login", color = Color(0xFF2874F0), fontWeight = FontWeight.Bold) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🚀 ADDED: Powered by Text at the absolute bottom
        Text(
            text = "Powered by SIMATS Engineering",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF9E9E9E),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
}