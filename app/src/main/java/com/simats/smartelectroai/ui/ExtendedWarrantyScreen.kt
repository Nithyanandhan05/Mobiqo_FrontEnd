package com.simats.smartelectroai.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.razorpay.Checkout
import org.json.JSONObject
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- IMPORTS ---
import com.simats.smartelectroai.PaymentCallbackHandler
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.ExtendWarrantyReq
import com.simats.smartelectroai.api.ExtendWarrantyRes
import com.simats.smartelectroai.api.WarrantyDetailResponse
import com.simats.smartelectroai.api.WarrantyDetailData
import com.simats.smartelectroai.api.AppConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- PREMIUM BRANDING COLORS ---
private val PrimaryBlue = Color(0xFF2962FF)
private val DeepBlue = Color(0xFF0D47A1)
private val LightBlue = Color(0xFFE3F2FD)
private val BgWhite = Color(0xFFFFFFFF)
private val GrayText = Color(0xFF757575)
private val DarkText = Color(0xFF1E1E1E)
private val AppBg = Color(0xFFF8FAFC)
private val SuccessGreen = Color(0xFF00C853)
private val ErrorRed = Color(0xFFE53935)
private val PremiumGold = Color(0xFFFFD700)

data class WarrantyPlan(
    val id: String,
    val title: String,
    val price: Int,
    val durationMonths: Int,
    val displayDuration: String,
    val isPopular: Boolean = false,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val features: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendedWarrantyScreen(warrantyId: Int, onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as Activity
    val scrollState = rememberScrollState()

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    var isLoading by remember { mutableStateOf(true) }
    var deviceData by remember { mutableStateOf<WarrantyDetailData?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Logic States
    var isExpired by remember { mutableStateOf(false) }
    var maxLimitReached by remember { mutableStateOf(false) }

    val allPlans = listOf(
        WarrantyPlan(
            id = "1", title = "Screen Protection", price = 1499, durationMonths = 12, displayDuration = "1 Year", icon = Icons.Default.PhoneAndroid,
            features = listOf("Accidental screen cracks", "1 Free screen replacement", "Does NOT cover liquid damage", "Does NOT cover hardware defects")
        ),
        WarrantyPlan(
            id = "2", title = "Comprehensive Cover", price = 3499, durationMonths = 24, displayDuration = "2 Years", isPopular = true, icon = Icons.Default.VerifiedUser,
            features = listOf("All hardware & software defects", "Accidental physical & liquid damage", "Free doorstep pickup & drop", "Zero hidden deductibles")
        ),
        WarrantyPlan(
            id = "3", title = "Standard Extension", price = 1999, durationMonths = 12, displayDuration = "1 Year", icon = Icons.Default.AddModerator,
            features = listOf("All hardware & software defects", "Official brand service centers", "Does NOT cover physical drops", "Does NOT cover liquid damage")
        )
    )

    var availablePlans by remember { mutableStateOf(allPlans) }
    var selectedPlan by remember { mutableStateOf(allPlans[1]) }

    var isHeaderVisible by remember { mutableStateOf(false) }
    var isDeviceVisible by remember { mutableStateOf(false) }
    var isPlansVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Checkout.preload(context.applicationContext)

        RetrofitClient.instance.getWarrantyDetail("Bearer $token", warrantyId).enqueue(object : Callback<WarrantyDetailResponse> {
            override fun onResponse(call: Call<WarrantyDetailResponse>, response: Response<WarrantyDetailResponse>) {
                val data = response.body()?.data
                if (data != null) {
                    deviceData = data
                    try {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
                        val pDate = sdf.parse(data.purchase_date)
                        val eDate = sdf.parse(data.expiry_date)
                        val today = Date()

                        if ((eDate != null && eDate.before(today)) || data.status.equals("Expired", true)) {
                            isExpired = true
                        }

                        if (pDate != null && eDate != null) {
                            val diffInMillies = Math.abs(eDate.time - pDate.time)
                            val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)
                            val totalMonths = (diffInDays / 30).toInt()
                            val extendedMonths = totalMonths - 12

                            if (extendedMonths >= 24) {
                                maxLimitReached = true
                            } else if (extendedMonths >= 12) {
                                availablePlans = allPlans.filter { it.durationMonths <= 12 }
                                selectedPlan = availablePlans.first()
                            }
                        }
                    } catch (e: Exception) {
                        if (data.status.equals("Expired", true)) isExpired = true
                    }
                }
                isLoading = false
            }
            override fun onFailure(call: Call<WarrantyDetailResponse>, t: Throwable) { isLoading = false }
        })

        isHeaderVisible = true
        delay(150); isDeviceVisible = true
        delay(150); isPlansVisible = true
    }

    DisposableEffect(Unit) {
        PaymentCallbackHandler.onSuccess = { paymentId, _, _ ->
            val req = ExtendWarrantyReq(selectedPlan.id, selectedPlan.durationMonths, selectedPlan.price, paymentId)
            RetrofitClient.instance.extendWarranty("Bearer $token", warrantyId, req).enqueue(object : Callback<ExtendWarrantyRes> {
                override fun onResponse(call: Call<ExtendWarrantyRes>, response: Response<ExtendWarrantyRes>) {
                    isProcessing = false
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Warranty Extended Successfully!", Toast.LENGTH_LONG).show()
                        onBack()
                    } else {
                        Toast.makeText(context, "Payment succeeded, but backend update failed.", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ExtendWarrantyRes>, t: Throwable) {
                    isProcessing = false
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
        }

        PaymentCallbackHandler.onError = { errorString ->
            isProcessing = false
            Toast.makeText(context, "Payment Failed: $errorString", Toast.LENGTH_LONG).show()
        }
        onDispose {
            PaymentCallbackHandler.onSuccess = null
            PaymentCallbackHandler.onError = null
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isHeaderVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Upgrade Warranty", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DarkText) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DarkText) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg)
                )
            }
        },
        bottomBar = {
            if (!isExpired && !maxLimitReached && availablePlans.isNotEmpty()) {
                AnimatedVisibility(visible = isPlansVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()) {
                    PremiumBottomPaymentBar(selectedPlan, isProcessing) {
                        if (deviceData == null) return@PremiumBottomPaymentBar
                        isProcessing = true
                        try {
                            val checkout = Checkout()
                            checkout.setKeyID(AppConfig.RAZORPAY_KEY_ID)

                            val options = JSONObject()
                            options.put("name", "Smart Electro AI")
                            options.put("description", "${selectedPlan.title} for ${deviceData?.device_name}")
                            options.put("theme.color", "#2962FF")
                            options.put("currency", "INR")
                            options.put("amount", selectedPlan.price * 100)

                            checkout.open(activity, options)
                        } catch (e: Exception) {
                            isProcessing = false
                            Toast.makeText(context, "Error initializing payment", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        },
        containerColor = AppBg
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = isDeviceVisible, enter = scaleIn(initialScale = 0.95f) + fadeIn(tween(500))) {
                    PremiumDeviceCard(deviceData)
                }

                AnimatedVisibility(visible = isPlansVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(600))) {
                    if (isExpired) {
                        WarningCard(Icons.Default.WarningAmber, "Warranty Expired", "Your device warranty has expired. You can no longer purchase an extension online. Please visit a service center.", ErrorRed)
                    } else if (maxLimitReached) {
                        WarningCard(Icons.Default.VerifiedUser, "Maximum Coverage Reached", "You have already extended this device to the maximum allowed 2-year limit. Your device is fully secured!", PrimaryBlue)
                    } else {
                        Column {
                            Text("SELECT PROTECTION PLAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                availablePlans.forEach { plan ->
                                    PremiumPlanCard(plan = plan, isSelected = selectedPlan.id == plan.id, onClick = { selectedPlan = plan })
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ==========================================
// PREMIUM UI COMPONENTS
// ==========================================

@Composable
fun WarningCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                Spacer(modifier = Modifier.height(6.dp))
                Text(message, color = DarkText, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun PremiumDeviceCard(device: WarrantyDetailData?) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), spotColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color.White, LightBlue.copy(alpha = 0.3f))))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).background(Brush.linearGradient(listOf(PrimaryBlue, DeepBlue)), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    if (!device?.image_url.isNullOrEmpty()) {
                        AsyncImage(
                            model = device?.image_url,
                            contentDescription = device?.device_name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Smartphone, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device?.device_name ?: "Unknown Device", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, tint = GrayText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Current Expiry: ${device?.expiry_date ?: "N/A"}", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPlanCard(plan: WarrantyPlan, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.97f else if (isSelected) 1.01f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val borderColor by animateColorAsState(if (isSelected) PrimaryBlue else Color(0xFFE0E0E0), label = "border")
    val borderWidth by animateDpAsState(if (isSelected) 2.dp else 1.dp, label = "borderWidth")
    val bgColor by animateColorAsState(if (isSelected) LightBlue.copy(alpha = 0.4f) else BgWhite, label = "bg")
    val shadow by animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "shadow")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).shadow(shadow, RoundedCornerShape(16.dp), spotColor = if(isSelected) PrimaryBlue else Color.Black).clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor), border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Checkbox
                Box(modifier = Modifier.size(22.dp).border(2.dp, if(isSelected) PrimaryBlue else Color.LightGray, CircleShape).background(if(isSelected) PrimaryBlue else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                    if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = plan.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp, // Reduced Font Size
                            color = DarkText,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (plan.isPopular) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFA000), PremiumGold)), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text("BEST VALUE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, softWrap = false)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(plan.icon, null, tint = PrimaryBlue, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+${plan.displayDuration} coverage", fontSize = 12.sp, color = DeepBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("₹${plan.price}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = PrimaryBlue) // Resized Price
            }

            // Expanding Feature Details
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp, start = 32.dp)) {
                    HorizontalDivider(color = Color(0xFFCFD8DC), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                    plan.features.forEach { feature ->
                        val isNegative = feature.contains("NOT") || feature.contains("No ")
                        val iconTint = if (isNegative) ErrorRed else SuccessGreen
                        val iconVector = if (isNegative) Icons.Default.Close else Icons.Default.CheckCircle

                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feature, fontSize = 12.sp, color = if(isNegative) GrayText else DarkText, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumBottomPaymentBar(selectedPlan: WarrantyPlan, isProcessing: Boolean, onPayClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btn_scale")

    Surface(shadowElevation = 24.dp, color = BgWhite, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Total Secure Payment", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                Text("₹${selectedPlan.price}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = DarkText)
            }
            Box(
                modifier = Modifier.width(170.dp).height(56.dp).scale(scale).shadow(if (isPressed) 2.dp else 12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryBlue).background(Brush.horizontalGradient(listOf(PrimaryBlue, DeepBlue)), RoundedCornerShape(16.dp)).clickable(interactionSource = interaction, indication = null, enabled = !isProcessing) { onPayClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = BgWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay Securely", color = BgWhite, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}