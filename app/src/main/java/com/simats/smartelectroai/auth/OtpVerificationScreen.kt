package com.simats.smartelectroai.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simats.smartelectroai.utils.UiState
import com.simats.smartelectroai.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    viewModel: AuthViewModel = viewModel(),
    onVerificationSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val otpState by viewModel.otpState.collectAsState()

    var otpValue by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    LaunchedEffect(otpState) {
        if (otpState is UiState.Success && (otpState as UiState.Success).data == "REGISTER_SUCCESS") {
            Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_LONG).show()
            viewModel.resetStates()
            onVerificationSuccess()
        } else if (otpState is UiState.Error) {
            Toast.makeText(context, (otpState as UiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetStates()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("Verify Email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter the 6-digit OTP sent to your email", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        // 🚀 INTEGRATED THE MODERN OTP INPUT HERE
        ModernRegistrationOtpInput(otp = otpValue, onOtpChange = { otpValue = it })

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.verifyOtpAndRegister(otpValue) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2874F0)),
            enabled = otpValue.length == 6 && otpState !is UiState.Loading
        ) {
            if (otpState is UiState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Verify & Register", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (timeLeft > 0) {
            Text("Resend OTP in ${timeLeft}s", color = Color.Gray, fontSize = 14.sp)
        } else {
            TextButton(onClick = {
                timeLeft = 60
                viewModel.pendingRegisterRequest?.let { viewModel.initiateRegistration(it) }
            }) {
                Text("Resend OTP", color = Color(0xFF2874F0), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Cancel", color = Color.Gray) }
    }
}

// ==========================================
// MODERN 6-DIGIT OTP COMPOSABLE
// ==========================================
@Composable
fun ModernRegistrationOtpInput(otp: String, onOtpChange: (String) -> Unit) {
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
                                color = if (isFocused) Color(0xFF2874F0) else Color(0xFFE0E0E0), // Deep blue focus
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2874F0) // Deep blue text
                        )
                    }
                }
            }
        }
    )
}