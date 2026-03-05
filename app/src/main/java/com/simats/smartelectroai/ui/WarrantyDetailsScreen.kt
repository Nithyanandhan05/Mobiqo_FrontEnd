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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- CLEANED UP: Imported data models from RetrofitClient directly ---
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.WarrantyDetailResponse
import com.simats.smartelectroai.api.WarrantyDetailData
import com.simats.smartelectroai.api.HistoryItem

// --- DESIGN COLORS ---
private val DetailBlueMain = Color(0xFF1976D2)
private val DetailTextHeader = Color(0xFF1E1E1E)
private val DetailSectionTitle = Color(0xFF757575)
private val DetailBorder = Color(0xFFEEEEEE)
private val DetailLightBlueBg = Color(0xFFE3F2FD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyDetailsScreen(warrantyId: Int, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var detailData by remember { mutableStateOf<WarrantyDetailData?>(null) }

    // Animation States
    var isVisible by remember { mutableStateOf(false) }
    var isTimelineVisible by remember { mutableStateOf(false) }
    var isInvoiceVisible by remember { mutableStateOf(false) }
    var isServiceVisible by remember { mutableStateOf(false) }

    // Progress Bar State
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "progress"
    )

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            // FIXED: Using centralized RetrofitClient instead of local builder!
            RetrofitClient.instance.getWarrantyDetail("Bearer $token", warrantyId)
                .enqueue(object : Callback<WarrantyDetailResponse> {
                    override fun onResponse(call: Call<WarrantyDetailResponse>, response: Response<WarrantyDetailResponse>) {
                        if (response.isSuccessful) {
                            detailData = response.body()?.data
                            targetProgress = detailData?.progress ?: 0f
                        }
                        isLoading = false
                    }
                    override fun onFailure(call: Call<WarrantyDetailResponse>, t: Throwable) { isLoading = false }
                })
        } else { isLoading = false }

        isVisible = true
        delay(200); isTimelineVisible = true
        delay(200); isInvoiceVisible = true
        delay(200); isServiceVisible = true
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Warranty Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DetailTextHeader) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DetailTextHeader) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DetailBlueMain) }
        } else if (detailData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No device data found.", color = Color.Gray) }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- 1. PRODUCT HEADER ---
                AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.9f) + fadeIn()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when (detailData!!.device_type.lowercase()) {
                            "laptop" -> Icons.Default.Laptop
                            "headphones" -> Icons.Default.Headphones
                            else -> Icons.Default.Smartphone
                        }
                        Box(modifier = Modifier.size(120.dp).shadow(8.dp, RoundedCornerShape(20.dp)).background(Color.White, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = DetailBlueMain, modifier = Modifier.size(60.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(detailData!!.device_name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DetailTextHeader)
                        Spacer(modifier = Modifier.height(8.dp))

                        val statUpper = detailData!!.status.uppercase()
                        val (badgeColor, badgeBg) = when {
                            statUpper == "ACTIVE" || statUpper == "SECURE" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                            statUpper.contains("ALERT") || statUpper.contains("EXPIRING") || statUpper == "PENDING" -> Pair(Color(0xFFEF6C00), Color(0xFFFFF3E0))
                            statUpper == "EXPIRED" || statUpper == "REJECTED" -> Pair(Color(0xFFC62828), Color(0xFFFFEBEE))
                            else -> Pair(DetailBlueMain, DetailLightBlueBg)
                        }

                        val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (statUpper.contains("ALERT") || statUpper.contains("EXPIRING")) 0.6f else 1f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
                        )

                        Box(modifier = Modifier.alpha(alpha).background(badgeBg, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(statUpper, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 2. TIMELINE ---
                AnimatedVisibility(visible = isTimelineVisible, enter = expandVertically() + fadeIn()) {
                    Column {
                        DetailSectionLabel("WARRANTY TIMELINE")
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, DetailBorder), elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("PURCHASE DATE", fontSize = 10.sp, color = DetailSectionTitle, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(detailData!!.purchase_date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DetailTextHeader)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EXPIRY DATE", fontSize = 10.sp, color = DetailSectionTitle, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(detailData!!.expiry_date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DetailTextHeader)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                val barColor = when {
                                    detailData!!.status.uppercase() == "EXPIRED" -> Color(0xFFC62828)
                                    detailData!!.status.uppercase().contains("ALERT") -> Color(0xFFEF6C00)
                                    else -> DetailBlueMain
                                }

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = barColor, trackColor = Color(0xFFF0F0F0)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Standard 1-Year", fontSize = 12.sp, color = DetailSectionTitle)
                                    AnimatedVisibility(visible = animatedProgress > 0f, enter = fadeIn()) {
                                        Text(detailData!!.months_left, fontSize = 12.sp, color = barColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. INVOICE ---
                AnimatedVisibility(visible = isInvoiceVisible, enter = slideInHorizontally(initialOffsetX = { 50 }) + fadeIn()) {
                    Column {
                        DetailSectionLabel("INVOICE")
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp)).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, "PDF", tint = Color(0xFF9E9E9E), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(detailData!!.invoice_name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DetailTextHeader, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    AnimatedActionText("View") { onNavigate("Invoice") }
                                    AnimatedActionText("Download") { onNavigate("Invoice") }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 4. SERVICE INFO ---
                AnimatedVisibility(visible = isServiceVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()) {
                    Column {
                        DetailSectionLabel("SERVICE INFORMATION")
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            AnimatedServiceItem(Icons.Default.LocationOn, "Locate Service Center")
                            Spacer(modifier = Modifier.height(10.dp))
                            AnimatedServiceItem(Icons.Default.Headphones, "Contact Support")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 5. HISTORY ---
                AnimatedVisibility(visible = isServiceVisible, enter = fadeIn(tween(1000))) {
                    Column {
                        DetailSectionLabel("WARRANTY HISTORY")
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            detailData!!.history.forEach { item ->
                                AnimatedHistoryTimeline(item)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 6. CTAS ---
                AnimatedVisibility(visible = isServiceVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()) {
                    Column {
                        AnimatedPrimaryButton("Claim Warranty") {
                            onNavigate("ClaimWarranty")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedSecondaryButton("Add Extended Warranty") {
                            onNavigate("ExtendWarranty")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ==========================================
// CUSTOM ANIMATED COMPONENTS
// ==========================================

@Composable
private fun DetailSectionLabel(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DetailSectionTitle, modifier = Modifier.fillMaxWidth().padding(start = 4.dp), letterSpacing = 1.sp)
}

@Composable
private fun AnimatedActionText(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "scale")

    Text(
        text, fontSize = 12.sp, color = DetailBlueMain, fontWeight = FontWeight.Bold,
        modifier = Modifier.scale(scale).clickable(interactionSource = interaction, indication = null) { onClick() }
    )
}

@Composable
private fun AnimatedServiceItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")
    val elevation by animateDpAsState(if (isPressed) 4.dp else 0.dp, label = "elev")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interaction, indication = null) { },
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DetailBorder), elevation = CardDefaults.cardElevation(elevation)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = DetailBlueMain, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DetailTextHeader, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun AnimatedHistoryTimeline(item: HistoryItem) {
    var isDotVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(500); isDotVisible = true }

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            AnimatedVisibility(visible = isDotVisible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy))) {
                Box(modifier = Modifier.size(10.dp).background(DetailBlueMain, CircleShape))
            }
            if (!item.is_last) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xFFEEEEEE)))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DetailTextHeader)
                Text(item.date, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(item.desc, fontSize = 13.sp, color = Color(0xFF616161), lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AnimatedPrimaryButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")
    val elev by animateDpAsState(if (isPressed) 2.dp else 8.dp, label = "")

    Button(
        onClick = onClick, interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(55.dp).scale(scale).shadow(elev, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DetailBlueMain)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
private fun AnimatedSecondaryButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")

    OutlinedButton(
        onClick = onClick, interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(55.dp).scale(scale),
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, DetailBlueMain),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailBlueMain)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}