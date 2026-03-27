package com.simats.smartelectroai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- PROFESSIONAL THEME COLORS ---
private val SurfaceWhite = Color(0xFFFFFFFF)
private val BrandBlue = Color(0xFF1976D2) // Professional Corporate Blue
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val BorderLight = Color(0xFFE2E8F0)
private val CardBackground = Color(0xFFF8FAFC)
private val SuccessGreen = Color(0xFF059669)

@Composable
fun SubscriptionScreen(
    onSkip: () -> Unit,
    onSubscribeSuccess: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        isVisible = true
    }

    Scaffold(
        containerColor = SurfaceWhite,
        bottomBar = {
            // Fixed Bottom Action Area for standard professional UX
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                isProcessing = true
                                // Simulate payment gateway delay
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isProcessing = false
                                    onSubscribeSuccess()
                                }, 1500)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upgrade for ₹99", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = onSkip) {
                            Text("Continue to Free Version", color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // --- 1. LOGO SECTION ---
            AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, BorderLight, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Changed from Shield to Security to fix the import error
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Mobiqo Logo",
                        tint = BrandBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. HEADER SECTION ---
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { 20 } + fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Mobiqo Premium",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Professional tools to secure, track, and manage your electronic devices efficiently.",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 3. PRICING CARD (ISO Standard Clean Box) ---
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { 40 } + fadeIn(tween(600))) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.05f)),
                    border = BorderStroke(2.dp, BrandBlue.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LIFETIME ACCESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("₹99", fontSize = 48.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            Text("/one-time", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recurring billing. Pay once, use forever.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 4. FEATURES LIST ---
            AnimatedVisibility(visible = isVisible, enter = slideInVertically { 60 } + fadeIn(tween(800))) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "What's included in Premium:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfessionalFeatureRow("Complete Ad-Free Experience", "Zero interruptions while browsing devices and managing warranties.")
                    ProfessionalFeatureRow("Unlimited AI Recommendations", "Access deep-dive specifications and AI comparison tools without limits.")
                    ProfessionalFeatureRow("Smart Warranty Alerts", "Get priority push notifications before your device warranties expire.")
                    ProfessionalFeatureRow("Secure Cloud Backup", "Store all your invoices and device data securely with 256-bit encryption.")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfessionalFeatureRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Included",
            tint = SuccessGreen,
            modifier = Modifier
                .size(22.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}