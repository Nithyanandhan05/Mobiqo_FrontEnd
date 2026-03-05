package com.simats.smartelectroai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.MyWarrantiesResponse
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.WarrantyDevice
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- THEME COLORS ---
private val PrimaryBlue = Color(0xFF1976D2)
private val LightBlueGradient = Color(0xFF42A5F5)
private val BgWhite = Color(0xFFFFFFFF)
private val AlertOrange = Color(0xFFEF6C00)
private val AlertOrangeBg = Color(0xFFFFF3E0)
private val SecureGreen = Color(0xFF2E7D32)
private val SecureGreenBg = Color(0xFFE8F5E9)
private val ExpiredGray = Color(0xFF757575)
private val ExpiredGrayBg = Color(0xFFEEEEEE)
private val GlassBg = Color.White.copy(alpha = 0.85f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWarrantyScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var responseData by remember { mutableStateOf<MyWarrantiesResponse?>(null) }

    // Visibility States for staggered animations
    var isTopBarVisible by remember { mutableStateOf(false) }
    var isStatsVisible by remember { mutableStateOf(false) }
    var isListVisible by remember { mutableStateOf(false) }
    var isAiVisible by remember { mutableStateOf(false) }

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getMyWarranties("Bearer $token").enqueue(object : Callback<MyWarrantiesResponse> {
                override fun onResponse(call: Call<MyWarrantiesResponse>, response: Response<MyWarrantiesResponse>) {
                    if (response.isSuccessful) {
                        responseData = response.body()
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<MyWarrantiesResponse>, t: Throwable) { isLoading = false }
            })
        } else { isLoading = false }

        // Staggered enter animations
        isTopBarVisible = true
        delay(150)
        isStatsVisible = true
        delay(200)
        isListVisible = true
        delay(300)
        isAiVisible = true
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isTopBarVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                AnimatedWarrantyTopBar(onNavigate)
            }
        },
        bottomBar = { WarrantyFloatingAiDock(onNavigate) },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // 1. DYNAMIC STATS CARD
                item {
                    AnimatedVisibility(visible = isStatsVisible, enter = scaleIn(initialScale = 0.9f) + fadeIn(tween(500))) {
                        // Calculate stats locally from the device list
                        val devices = responseData?.devices ?: emptyList()
                        val total = devices.size
                        val active = devices.count { it.status.lowercase() == "secure" || it.status.lowercase() == "active" }
                        val expiring = devices.count { it.status.lowercase().contains("alert") }
                        val expired = devices.count { it.status.lowercase() == "expired" || it.status.lowercase() == "rejected" }

                        AnimatedStatsCard(total, active, expiring, expired)
                    }
                }

                // 2. HEADER
                item {
                    AnimatedVisibility(visible = isListVisible, enter = fadeIn()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("YOUR DEVICES", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }

                // 3. DEVICE LIST
                val devices = responseData?.devices ?: emptyList()
                if (devices.isEmpty() && isListVisible) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No devices found. Click '+' to add your first device!", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(devices) { index, device ->
                        AnimatedVisibility(
                            visible = isListVisible,
                            enter = slideInVertically(initialOffsetY = { 50 * (index + 1) }) + fadeIn(tween(400))
                        ) {
                            AnimatedDeviceCard(device) { onNavigate("WarrantyDetail/${device.id}") }
                        }
                    }
                }

                // 4. AI RECOMMENDATION
                item {
                    AnimatedVisibility(visible = isAiVisible, enter = expandVertically() + fadeIn(tween(600))) {
                        responseData?.ai_recommendation?.let { rec ->
                            AnimatedAiRecommendationCard(rec.message)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

// ==========================================
// ANIMATED COMPOSABLES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedWarrantyTopBar(onNavigate: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot"
    )

    val addInteractionSource = remember { MutableInteractionSource() }
    val isAddPressed by addInteractionSource.collectIsPressedAsState()
    val addRotation by animateFloatAsState(if (isAddPressed) 90f else 0f, label = "rotate")

    TopAppBar(
        title = { Text("My Warranties", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFF1E1E1E)) },
        actions = {
            Box(modifier = Modifier.clickable { onNavigate("WarrantyAlerts") }.padding(8.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF1E1E1E), modifier = Modifier.size(26.dp))
                Box(modifier = Modifier.align(Alignment.TopEnd).scale(dotScale).size(8.dp).background(Color.Red, CircleShape))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Add, contentDescription = "Add", tint = PrimaryBlue,
                modifier = Modifier.size(30.dp).rotate(addRotation).clickable(interactionSource = addInteractionSource, indication = LocalIndication.current) { onNavigate("AddWarranty") }
            )
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AnimatedStatsCard(total: Int, active: Int, expiring: Int, expired: Int) {
    val totalState by animateIntAsState(total, tween(1000), label = "")
    val activeState by animateIntAsState(active, tween(1000), label = "")
    val expiringState by animateIntAsState(expiring, tween(1000), label = "")
    val expiredState by animateIntAsState(expired, tween(1000), label = "")

    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryBlue, LightBlueGradient)))) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    WarrantyStatItem("TOTAL REGISTERED", "$totalState", Modifier.weight(1f))
                    WarrantyStatItem("ACTIVE", "$activeState", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    WarrantyStatItem("EXPIRING SOON", "$expiringState", Modifier.weight(1f), showTag = true)
                    WarrantyStatItem("EXPIRED", "$expiredState", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WarrantyStatItem(label: String, value: String, modifier: Modifier = Modifier, showTag: Boolean = false) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (showTag) {
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(visible = value != "0", enter = scaleIn()) {
                    Box(modifier = Modifier.background(Color(0xFFFF9800), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("<30d", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedDeviceCard(device: WarrantyDevice, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")
    val elevation by animateDpAsState(if (isPressed) 8.dp else 2.dp, label = "elevation")

    // Dynamic icon based on name
    val icon = when {
        device.name.lowercase().contains("laptop") -> Icons.Default.Laptop
        device.name.lowercase().contains("headphones") -> Icons.Default.Headphones
        else -> Icons.Default.Smartphone
    }

    val statLower = device.status.lowercase()
    val (textColor, bgColor) = when {
        statLower == "secure" || statLower == "active" -> Pair(SecureGreen, SecureGreenBg)
        statLower.contains("alert") || statLower == "pending" -> Pair(AlertOrange, AlertOrangeBg)
        else -> Pair(ExpiredGray, ExpiredGrayBg)
    }
    val animatedBgColor by animateColorAsState(bgColor, label = "bg")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BgWhite),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0)), elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = device.expiry, color = Color.Gray, fontSize = 13.sp)
            }
            Box(modifier = Modifier.background(animatedBgColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = device.status, color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AnimatedAiRecommendationCard(message: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val alphaAnim by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha")

    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = PrimaryBlue.copy(alpha = alphaAnim), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Mobiqo Warranty Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E1E1E))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(message, color = Color(0xFF424242), fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View Extension Plans", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { })
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp).padding(start = 2.dp))
                    }
                }
            }
        }
    }
}

// EXACT MATCH FOR DASHBOARD BOTTOM DOCK
@Composable
private fun WarrantyFloatingAiDock(onNavigate: (String) -> Unit) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4)))

    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp), spotColor = PrimaryBlue),
            shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = GlassBg)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Dashboard") })
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Compare") })

                // Center Glowing AI Button
                Box(
                    modifier = Modifier.size(56.dp).background(gradient, CircleShape).shadow(8.dp, CircleShape).clickable { onNavigate("Chat") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                // Warranty icon is PrimaryBlue because we are ON the warranty screen
                Icon(Icons.Default.VerifiedUser, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp).clickable { onNavigate("MyWarranty") })
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Profile") })
            }
        }
    }
}