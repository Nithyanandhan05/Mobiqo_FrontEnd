package com.simats.smartelectroai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.R
import com.simats.smartelectroai.api.*
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- Thematic Colors ---
private val AiBlue = Color(0xFF2962FF)
private val AiLightBlue = Color(0xFF03A9F4)
private val AiGradient = Brush.linearGradient(listOf(AiBlue, AiLightBlue))
private val GlassBg = Color.White.copy(alpha = 0.85f)
private val BgLightBlue = Color(0xFFF4F8FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isTopVisible by remember { mutableStateOf(false) }
    var isStatsVisible by remember { mutableStateOf(false) }
    var isListVisible by remember { mutableStateOf(false) }
    var isBottomVisible by remember { mutableStateOf(false) }
    var profileData by remember { mutableStateOf<ProfileData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // --- NEW: Accurate tracking states bypassing the backend mock data ---
    var realOrderCount by remember { mutableIntStateOf(0) }
    var realWarrantyCount by remember { mutableIntStateOf(0) }
    var realAddressCount by remember { mutableIntStateOf(0) }
    var realAiCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", "")

        // Accurate AI usage tracking from device
        realAiCount = prefs.getInt("ai_usage_count", 0)

        if (!token.isNullOrEmpty()) {
            val authHeader = "Bearer $token"

            // 1. Fetch Profile Name and Email ONLY
            RetrofitClient.instance.getProfile(authHeader).enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    isLoading = false
                    if (response.isSuccessful) profileData = response.body()?.profile
                }
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) { isLoading = false }
            })

            // 2. Fetch ACCURATE Orders Count
            RetrofitClient.instance.getMyOrders(authHeader).enqueue(object : Callback<MyOrdersResponse> {
                override fun onResponse(call: Call<MyOrdersResponse>, response: Response<MyOrdersResponse>) {
                    if (response.isSuccessful) realOrderCount = response.body()?.orders?.size ?: 0
                }
                override fun onFailure(call: Call<MyOrdersResponse>, t: Throwable) {}
            })

            // 3. Fetch ACCURATE Warranties Count
            RetrofitClient.instance.getMyWarranties(authHeader).enqueue(object : Callback<MyWarrantiesResponse> {
                override fun onResponse(call: Call<MyWarrantiesResponse>, response: Response<MyWarrantiesResponse>) {
                    if (response.isSuccessful) {
                        realWarrantyCount = response.body()?.devices?.filter { it.status != "Expired" && it.status != "Rejected" }?.size ?: 0
                    }
                }
                override fun onFailure(call: Call<MyWarrantiesResponse>, t: Throwable) {}
            })

            // 4. Fetch ACCURATE Addresses Count
            RetrofitClient.instance.getAddresses(authHeader).enqueue(object : Callback<AddressListResponse> {
                override fun onResponse(call: Call<AddressListResponse>, response: Response<AddressListResponse>) {
                    if (response.isSuccessful) realAddressCount = response.body()?.addresses?.size ?: 0
                }
                override fun onFailure(call: Call<AddressListResponse>, t: Throwable) {}
            })

        } else {
            isLoading = false
        }

        isTopVisible = true
        delay(200)
        isStatsVisible = true
        delay(200)
        isListVisible = true
        delay(200)
        isBottomVisible = true
    }

    Scaffold(
        bottomBar = { PrivateFloatingAiDock(onNavigate) },
        containerColor = BgLightBlue
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AiBlue) }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 1: AI Header
            AnimatedVisibility(visible = isTopVisible, enter = scaleIn() + fadeIn()) {
                PrivateAiProfileHeader(profileData)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: Animated Stats (Using REAL dynamic data)
            AnimatedVisibility(visible = isStatsVisible, enter = slideInVertically(initialOffsetY = { it/2 }) + fadeIn()) {
                PrivateAiStatsRow(
                    orders = realOrderCount,
                    warranties = realWarrantyCount,
                    addresses = realAddressCount,
                    aiUses = realAiCount
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 3: Interactive Quick Access
            AnimatedVisibility(visible = isListVisible, enter = fadeIn(tween(800))) {
                Column(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Text("QUICK ACCESS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                    PrivateAiQuickAccessRow(Icons.Default.ShoppingBag, "My Orders") { onNavigate("MyOrders") }
                    PrivateAiQuickAccessRow(Icons.Default.VerifiedUser, "My Warranty") { onNavigate("MyWarranty") }
                    PrivateAiQuickAccessRow(Icons.Default.LocationOn, "Saved Addresses") { onNavigate("SavedAddresses") }
                    PrivateAiQuickAccessRow(Icons.Default.Payment, "Payment Methods") { onNavigate("PaymentMethods") }
                    PrivateAiQuickAccessRow(Icons.Default.Notifications, "Notifications") { onNavigate("Notifications") }
                    PrivateAiQuickAccessRow(Icons.Default.Lock, "Privacy & Security") { onNavigate("PrivacySecurity") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: Pulsing Security Card
            AnimatedVisibility(visible = isListVisible, enter = fadeIn(tween(1000))) {
                PrivateAiSecurityStatusCard()
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 5: Logout
            AnimatedVisibility(visible = isBottomVisible, enter = fadeIn()) {
                TextButton(
                    onClick = {
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                        onNavigate("Login")
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOGOUT", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// PRIVATE UI COMPONENTS
// ==========================================

@Composable
private fun PrivateAiProfileHeader(data: ProfileData?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, AiLightBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = R.drawable.profile), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(data?.full_name ?: "AI Guest", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF1E1E2C))
        Text(data?.email ?: "Sign in to view details", fontSize = 14.sp, color = Color.Gray)
    }
}

// Updated to take explicit integers instead of the profile object
@Composable
private fun PrivateAiStatsRow(orders: Int, warranties: Int, addresses: Int, aiUses: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        PrivateAnimatedStatBox("Orders", orders)
        PrivateAnimatedStatBox("Warranties", warranties)
        PrivateAnimatedStatBox("Addresses", addresses)
        PrivateAnimatedStatBox("AI Uses", aiUses)
    }
}

@Composable
private fun PrivateAnimatedStatBox(label: String, targetValue: Int) {
    val animValue = remember { Animatable(0f) }

    // Animate safely to the target value
    LaunchedEffect(targetValue) {
        animValue.animateTo(targetValue.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.size(80.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${animValue.value.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AiBlue)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PrivateAiQuickAccessRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(if (isPressed) Color(0xFFE1F5FE) else Color.Transparent, label = "")
    val arrowRotation by animateFloatAsState(if (isPressed) 90f else 0f, label = "")
    val scale by animateFloatAsState(if (isPressed) 1.02f else 1f, label = "")

    Row(
        modifier = Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(12.dp)).background(bgColor).clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }.padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(BgLightBlue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AiBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E1E2C), modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.LightGray, modifier = Modifier.rotate(arrowRotation))
    }
}

@Composable
private fun PrivateAiSecurityStatusCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.03f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("AI Security Active", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                Text("256-bit Encryption Enabled", fontSize = 12.sp, color = Color(0xFF388E3C))
            }
        }
    }
}

@Composable
private fun PrivateFloatingAiDock(onNavigate: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp), spotColor = AiBlue),
            shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = GlassBg)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Dashboard") })
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Compare") })
                Box(modifier = Modifier.size(56.dp).background(AiGradient, CircleShape).shadow(8.dp, CircleShape).clickable { onNavigate("Chat") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("MyWarranty") })
                Icon(Icons.Default.Person, null, tint = AiBlue, modifier = Modifier.size(28.dp)) // Already on profile
            }
        }
    }
}