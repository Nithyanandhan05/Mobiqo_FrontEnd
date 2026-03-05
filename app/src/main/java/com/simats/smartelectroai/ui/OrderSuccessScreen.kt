package com.simats.smartelectroai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.OrderContext
import kotlinx.coroutines.delay

// Thematic Colors
private val AiBlue = Color(0xFF2962FF)
private val AiLightBlue = Color(0xFF03A9F4)
private val AiGradient = Brush.linearGradient(listOf(AiBlue, AiLightBlue))
private val GlassBg = Color.White.copy(alpha = 0.7f)

@Composable
fun OrderSuccessScreen(onContinueShopping: () -> Unit = {}) {
    var isTopVisible by remember { mutableStateOf(false) }
    var isBottomVisible by remember { mutableStateOf(false) }
    var isContinueVisible by remember { mutableStateOf(false) }

    // Sequential Timeline State
    var visibleSteps by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isTopVisible = true
        delay(500)
        // Animate Timeline sequentially
        for (i in 1..4) {
            delay(300)
            visibleSteps = i
        }
        delay(400)
        isBottomVisible = true
        delay(1000)
        isContinueVisible = true
    }

    Scaffold(
        containerColor = Color(0xFFF4F8FF) // Very light blue AI background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 1: Top Success Ripple Icon
            AnimatedVisibility(
                visible = isTopVisible,
                enter = scaleIn(tween(600, easing = OvershootInterpolator)) + fadeIn()
            ) {
                TopRippleSuccessSection()
            }

            Spacer(modifier = Modifier.height(32.dp))

            // EXTRA SECTION: Dynamic Backend Data
            AnimatedVisibility(visible = visibleSteps > 0, enter = fadeIn() + slideInVertically()) {
                BackendOrderDetailsCard()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: Timeline
            DeliveryTimeline(visibleSteps)

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: Pulsing AI Protection
            AnimatedVisibility(visible = visibleSteps >= 4, enter = fadeIn(tween(800))) {
                PulsingSmartGuardCard()
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 4 & 5: Buttons
            AnimatedVisibility(
                visible = isBottomVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingTrackButton()
                    Spacer(modifier = Modifier.height(16.dp))
                    GlowingDownloadButton()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 6: Continue Shopping
            AnimatedVisibility(visible = isContinueVisible, enter = fadeIn(tween(1000))) {
                TextButton(onClick = onContinueShopping) {
                    Text("Continue Shopping", color = AiBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AiBlue, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Custom Easing for the pop-in effect
private val OvershootInterpolator = Easing {
    val t = it - 1.0f
    (t * t * ((2.0f + 1) * t + 2.0f) + 1.0f)
}

@Composable
fun TopRippleSuccessSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "rippleAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            // Ripple Background
            Box(modifier = Modifier.fillMaxSize().scale(rippleScale).background(AiLightBlue.copy(alpha = rippleAlpha), CircleShape))
            Box(modifier = Modifier.size(90.dp).scale(rippleScale * 0.8f).background(AiBlue.copy(alpha = rippleAlpha), CircleShape))

            // Solid Icon
            Box(modifier = Modifier.size(70.dp).background(AiGradient, CircleShape).shadow(8.dp, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Order Successfully Placed", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AiBlue)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your order has been verified and processed", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun BackendOrderDetailsCard() {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFE1F5FE), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Order ID", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                // Grabbing real ID from Backend context
                Text("#ORD-${OrderContext.currentOrderId}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Amount Paid", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                // Grabbing real price from Backend context
                Text("₹%,d".format(OrderContext.currentTotalAmount), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AiBlue)
            }
        }
    }
}

@Composable
fun DeliveryTimeline(visibleSteps: Int) {
    // Pair defines the text and whether it is completed (true) or pending (false)
    val steps = listOf(
        Pair("Payment Verified", true),
        Pair("Warranty Registered", true),
        Pair("Delivery Assigned", false),
        Pair("Out for Delivery", false)
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            steps.forEachIndexed { index, step ->
                val title = step.first
                val isCompleted = step.second

                AnimatedVisibility(
                    visible = visibleSteps > index,
                    enter = slideInHorizontally(initialOffsetX = { -it/2 }) + fadeIn(tween(500))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dynamic Icon based on completion
                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = AiLightBlue, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Schedule, contentDescription = "Pending", tint = Color.LightGray, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Dynamic Text color
                        Text(
                            text = title,
                            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCompleted) Color.DarkGray else Color.Gray,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Pending Badge
                        if (!isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Pending", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingSmartGuardCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cardPulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Soft Green
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("SmartGuard Protection Enabled", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your product warranty is now stored safely", fontSize = 12.sp, color = Color(0xFF388E3C))
            }
        }
    }
}

@Composable
fun FloatingTrackButton() {
    Button(
        onClick = { /* Navigate to tracking */ },
        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AiBlue),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(AiGradient), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Track Delivery", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun GlowingDownloadButton() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = AiLightBlue.copy(alpha = 0.3f),
        targetValue = AiBlue.copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "borderColor"
    )

    OutlinedButton(
        onClick = { /* Download PDF */ },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, glowColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = GlassBg)
    ) {
        Icon(Icons.Default.Download, contentDescription = "Download", tint = AiBlue)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download Invoice", color = AiBlue, fontWeight = FontWeight.Bold)
    }
}