package com.simats.smartelectroai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- IMPORT MODELS FROM GLOBAL API CLIENT ---
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.AlertResponse
import com.simats.smartelectroai.api.AlertDevice

// --- DESIGN COLORS ---
private val AlertBlue = Color(0xFF1976D2)
private val AlertGradient = Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF42A5F5)))
private val AlertTextDark = Color(0xFF1E1E1E)
private val AlertTextGray = Color(0xFF757575)

// FIXED: Changed Alert Cards to Red to match the rest of the app
private val ExpiringBg = Color(0xFFFFEBEE)
private val ExpiringText = Color(0xFFC62828)

private val ExpiredBg = Color(0xFFEEEEEE)
private val ExpiredText = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyAlertsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var responseData by remember { mutableStateOf<AlertResponse?>(null) }

    // FIXED: Default filter is now "Alert" instead of "All"
    var selectedFilter by remember { mutableStateOf("Alert") }

    // Animation visibility states
    var isTopVisible by remember { mutableStateOf(false) }
    var isListVisible by remember { mutableStateOf(false) }

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getAlerts("Bearer $token").enqueue(object : Callback<AlertResponse> {
                override fun onResponse(call: Call<AlertResponse>, response: Response<AlertResponse>) {
                    if (response.isSuccessful) responseData = response.body()
                    isLoading = false
                }
                override fun onFailure(call: Call<AlertResponse>, t: Throwable) { isLoading = false }
            })
        } else { isLoading = false }

        isTopVisible = true
        delay(300)
        isListVisible = true
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isTopVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Warranty Alerts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AlertTextDark) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AlertTextDark) } },
                    actions = { IconButton(onClick = { }) { Icon(Icons.Default.FilterList, "Filter", tint = AlertTextDark) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AlertBlue) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

                Spacer(modifier = Modifier.height(32.dp))

                // --- 1. DASHBOARD SUMMARY CARD ---
                AnimatedVisibility(visible = isTopVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                    val devices = responseData?.devices ?: emptyList()
                    val total = devices.size
                    val active = devices.count { it.status.lowercase() == "secure" || it.status.lowercase() == "active" }
                    val expiring = devices.count { it.status.lowercase().contains("alert") || it.status.lowercase() == "pending" }
                    val expired = devices.count { it.status.lowercase() == "expired" || it.status.lowercase() == "rejected" }

                    AnimatedAlertDashboard(total, active, expiring, expired)
                }

                // --- 2. FILTER CHIPS ---
                AnimatedVisibility(visible = isTopVisible, enter = fadeIn()) {
                    // FIXED: Moved "Alert" to the front of the list
                    val filters = listOf("Alert", "Expired", "All", "Secure")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 24.dp)) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            val bgColor by animateColorAsState(if (isSelected) AlertBlue else Color(0xFFF5F5F5), label = "")
                            val txtColor by animateColorAsState(if (isSelected) Color.White else AlertTextGray, label = "")
                            val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "")

                            Box(
                                modifier = Modifier.scale(scale).clip(RoundedCornerShape(50)).background(bgColor).clickable { selectedFilter = filter }.padding(horizontal = 24.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(filter.uppercase(), color = txtColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // --- 3. DYNAMIC LIST ---
                val filteredList = responseData?.devices?.filter {
                    if (selectedFilter == "All") true
                    else if (selectedFilter == "Alert") it.status.lowercase().contains("alert") || it.status.lowercase() == "pending"
                    else if (selectedFilter == "Expired") it.status.lowercase() == "expired" || it.status.lowercase() == "rejected"
                    else it.status.lowercase() == "secure" || it.status.lowercase() == "active"
                } ?: emptyList()

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {

                    val expiringCount = filteredList.count { it.status.lowercase().contains("alert") || it.status.lowercase() == "pending" }
                    if (expiringCount > 0) {
                        item {
                            AnimatedVisibility(visible = isListVisible, enter = expandVertically() + fadeIn()) {
                                AnimatedAiAlertCard(expiringCount)
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No devices found in this category.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else {
                        itemsIndexed(filteredList) { index, device ->
                            AnimatedVisibility(visible = isListVisible, enter = slideInVertically(initialOffsetY = { 50 * (index + 1) }) + fadeIn()) {
                                val statLower = device.status.lowercase()
                                if (statLower.contains("alert") || statLower == "pending") {
                                    AnimatedExpiringCard(device) { onNavigate("WarrantyDetail/${device.id}") }
                                } else if (statLower == "expired" || statLower == "rejected") {
                                    AnimatedExpiredCard(device) { onNavigate("WarrantyDetail/${device.id}") }
                                } else {
                                    AnimatedSecureCard(device) { onNavigate("WarrantyDetail/${device.id}") }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}

// ==========================================
// ANIMATED UI COMPONENTS
// ==========================================

@Composable
private fun AnimatedAlertDashboard(total: Int, active: Int, expiring: Int, expired: Int) {
    val alertState by animateIntAsState(expiring, tween(1500), label = "")
    val expiredState by animateIntAsState(expired, tween(1500), label = "")
    val activeState by animateIntAsState(active, tween(1500), label = "")

    Card(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(AlertGradient).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DashboardStatItem("Alerts", alertState, Color(0xFFFFB74D))
                DashboardStatItem("Expired", expiredState, Color(0xFFE0E0E0))
                DashboardStatItem("Active", activeState, Color.White)
            }
        }
    }
}

@Composable
private fun DashboardStatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label.uppercase(), fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun AnimatedAiAlertCard(expiringCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alphaAnim by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), border = BorderStroke(1.dp, AlertBlue.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, "AI", tint = AlertBlue.copy(alpha = alphaAnim), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("AI recommends renewing $expiringCount device(s) within the next 30 days to avoid out-of-pocket repair risks.", color = Color(0xFF1E1E2C), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AnimatedExpiringCard(device: AlertDevice, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")
    val elev by animateDpAsState(if (isPressed) 2.dp else 8.dp, label = "")

    val icon = when {
        device.name.lowercase().contains("laptop") -> Icons.Default.Laptop
        device.name.lowercase().contains("headphones") -> Icons.Default.Headphones
        else -> Icons.Default.Smartphone
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elev, RoundedCornerShape(16.dp)).clickable { onClick() },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AlertTextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(device.expiry, fontSize = 12.sp, color = ExpiringText, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.background(ExpiringBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("ALERT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpiringText)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onClick() }, interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().height(45.dp).scale(scale),
                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AlertBlue)
            ) {
                Text("Renew Warranty", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnimatedExpiredCard(device: AlertDevice, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")

    val icon = when {
        device.name.lowercase().contains("laptop") -> Icons.Default.Laptop
        device.name.lowercase().contains("headphones") -> Icons.Default.Headphones
        else -> Icons.Default.Smartphone
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AlertTextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(device.expiry, fontSize = 12.sp, color = AlertTextGray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.background(ExpiredBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("EXPIRED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpiredText)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onClick() }, interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().height(45.dp).scale(scale),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, AlertBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertBlue)
            ) {
                Text("Extend Warranty", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnimatedSecureCard(device: AlertDevice, onClick: () -> Unit) {
    val icon = when {
        device.name.lowercase().contains("laptop") -> Icons.Default.Laptop
        device.name.lowercase().contains("headphones") -> Icons.Default.Headphones
        else -> Icons.Default.Smartphone
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AlertTextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(device.expiry, fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("SECURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }
    }
}