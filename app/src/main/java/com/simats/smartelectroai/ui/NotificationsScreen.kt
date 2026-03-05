package com.simats.smartelectroai.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.UniqueNotifFetchResponse
import com.simats.smartelectroai.api.UniqueNotifPrefsData
import com.simats.smartelectroai.api.UniqueNotifUpdateResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- Thematic Colors ---
private val NotifBlue = Color(0xFF1976D2)
private val NotifLightBlue = Color(0xFF03A9F4)
private val NotifGradient = Brush.linearGradient(listOf(NotifBlue, NotifLightBlue))
private val BgWhite = Color(0xFFFFFFFF)
private val TextMain = Color(0xFF1E1E1E)
private val TextSub = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isScreenVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // State Variables
    var orderUpdates by remember { mutableStateOf(true) }
    var warrantyAlerts by remember { mutableStateOf(true) }
    var aiUpdates by remember { mutableStateOf(true) }
    var promotions by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("Daily summary") }

    // Permission Launcher for Android 13+ Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showTestNotification(context)
            } else {
                Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val token = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
    }

    // Load Data using the Global RetrofitClient!
    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getPrefs("Bearer $token").enqueue(object : Callback<UniqueNotifFetchResponse> {
                override fun onResponse(call: Call<UniqueNotifFetchResponse>, response: Response<UniqueNotifFetchResponse>) {
                    isLoading = false
                    val prefs = response.body()?.preferences
                    if (prefs != null) {
                        orderUpdates = prefs.order_updates
                        warrantyAlerts = prefs.warranty_alerts
                        aiUpdates = prefs.ai_updates
                        promotions = prefs.promotions
                        selectedFrequency = prefs.frequency
                    }
                    isScreenVisible = true
                }
                override fun onFailure(call: Call<UniqueNotifFetchResponse>, t: Throwable) {
                    isLoading = false; isScreenVisible = true
                }
            })
        } else { isLoading = false; isScreenVisible = true }
    }

    // Save Function using the Global RetrofitClient!
    fun savePrefs() {
        if (token.isNullOrEmpty()) return
        val newPrefs = UniqueNotifPrefsData(orderUpdates, warrantyAlerts, aiUpdates, promotions, selectedFrequency)
        RetrofitClient.instance.updatePrefs("Bearer $token", newPrefs).enqueue(object : Callback<UniqueNotifUpdateResponse> {
            override fun onResponse(call: Call<UniqueNotifUpdateResponse>, response: Response<UniqueNotifUpdateResponse>) {}
            override fun onFailure(call: Call<UniqueNotifUpdateResponse>, t: Throwable) {
                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Notifications", fontWeight = FontWeight.Bold, color = TextMain) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        bottomBar = { PrivateFloatingAiDock(onNavigate) },
        containerColor = BgWhite
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NotifBlue) }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 1: TOGGLES
                AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(500))) {
                    Column {
                        PrivateSectionHeader("NOTIFICATION PREFERENCES")
                        Card(
                            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)), elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column {
                                PrivateAnimatedToggle("Order Updates", "Track your electronics purchases", orderUpdates) { orderUpdates = it; savePrefs() }
                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                                PrivateAnimatedToggle("Warranty Expiry Alerts", "Get notified before coverage ends", warrantyAlerts) { warrantyAlerts = it; savePrefs() }
                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                                PrivateAnimatedToggle("AI Recommendation Updates", "New electronics matched for you", aiUpdates) { aiUpdates = it; savePrefs() }
                                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                                PrivateAnimatedToggle("Promotional Offers", "Exclusive deals and discounts", promotions) { promotions = it; savePrefs() }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // SECTION 2: RADIO BUTTONS
                AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(800))) {
                    Column {
                        PrivateSectionHeader("REMINDER FREQUENCY")
                        Card(
                            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)), elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column {
                                val options = listOf("Instant", "Daily summary", "Weekly summary")
                                options.forEachIndexed { index, option ->
                                    PrivateAnimatedRadio(option, selectedFrequency == option) { selectedFrequency = option; savePrefs() }
                                    if (index < options.size - 1) HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // SECTION 3: FOOTER & TEST BUTTON
                AnimatedVisibility(visible = isScreenVisible, enter = fadeIn(tween(1500))) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                        showTestNotification(context)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    showTestNotification(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NotifBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Test Notification", fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enterprise secure notification system active", fontSize = 11.sp, color = Color(0xFFBDBDBD), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ==========================================
// NOTIFICATION HELPER FUNCTION
// ==========================================
private fun showTestNotification(context: Context) {
    val channelId = "mobiqo_alerts"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // FIXED: Changed Build.VERSION.CODES to Build.VERSION_CODES
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Mobiqo Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("\uD83E\uDD16 Mobiqo")
        .setContentText("This is a test notification! Your AI settings are active.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}

// ==========================================
// PRIVATE UI COMPONENTS
// ==========================================

@Composable
private fun PrivateSectionHeader(text: String) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9E9E9E), letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun PrivateAnimatedToggle(title: String, desc: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    val trackColor by animateColorAsState(if (isChecked) NotifBlue else Color(0xFFE0E0E0), label = "trackColor")

    Row(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onCheckedChange(!isChecked) }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = TextSub)
        }
        Switch(
            checked = isChecked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = trackColor, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFEEEEEE), uncheckedBorderColor = Color.Transparent),
            modifier = Modifier.scale(0.9f)
        )
    }
}

@Composable
private fun PrivateAnimatedRadio(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    val textColor by animateColorAsState(if (isSelected) NotifBlue else TextMain, label = "textColor")
    val bgColor by animateColorAsState(if (isSelected) Color(0xFFF0F8FF) else Color.Transparent, label = "bgColor")

    Row(
        modifier = Modifier.fillMaxWidth().scale(scale).background(bgColor).clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = textColor, modifier = Modifier.weight(1f))
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = NotifBlue, unselectedColor = Color(0xFFE0E0E0)))
    }
}

@Composable
private fun PrivateFloatingAiDock(onNavigate: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp), spotColor = NotifBlue),
            shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Dashboard") })
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Compare") })
                Box(modifier = Modifier.size(56.dp).background(NotifGradient, CircleShape).shadow(8.dp, CircleShape).clickable { onNavigate("Chat") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("MyWarranty") })
                Icon(Icons.Default.Person, null, tint = NotifBlue, modifier = Modifier.size(28.dp).clickable { onNavigate("Profile") })
            }
        }
    }
}