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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            Text("Last Updated: March 2026", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text("1. Information We Collect", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We collect information you provide directly to us when you register for an account, make a purchase, or submit a warranty claim. This includes your name, email address, phone number, shipping address, and payment transaction IDs.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("2. How We Use Your Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We use the information we collect to:\n" +
                        "• Process and fulfill your electronics orders.\n" +
                        "• Manage your device warranties and repair claims.\n" +
                        "• Provide personalized AI smartphone recommendations based on your searches.\n" +
                        "• Send order updates, security alerts, and support messages.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("3. Data Security & Payments", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your payment information is processed securely through Razorpay. We do not store your full credit card numbers or UPI PINs on our servers. All data transfers are encrypted.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("4. Your Rights (Account Deletion)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You have the right to request the complete deletion of your account and personal data. You can do this at any time via the 'Privacy & Security' settings in our app. Note that deleting your account will void any active warranties tied to your profile.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("5. Contact Us", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "If you have any questions about this Privacy Policy, please contact us at support@[YourCompanyEmail].com.",
                fontSize = 14.sp, color = Color(0xFF424242), lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}