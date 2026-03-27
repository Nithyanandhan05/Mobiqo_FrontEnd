package com.simats.smartelectroai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Effective Date: March 2026", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text("1. Agreement to Terms", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "By accessing or using the Mobiqo / SmartElectro AI application, you agree to be bound by these Terms and Conditions. If you disagree with any part of the terms, you may not access our services.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("2. User Accounts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "When you create an account with us, you must provide accurate and complete information. You are responsible for safeguarding your password and for all activities that occur under your account.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("3. Products & AI Recommendations", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Our AI provides smartphone comparisons and recommendations based on web data. While we strive for accuracy, we do not warrant that product descriptions, pricing, or specifications are entirely error-free. We reserve the right to cancel orders arising from pricing errors.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("4. Warranties & Claims", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Warranties registered through our platform are subject to approval by our Admin team. Submitting false invoices or fraudulent repair claims will result in immediate account termination.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}