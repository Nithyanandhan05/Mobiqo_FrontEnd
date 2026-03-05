package com.simats.smartelectroai.ui

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.AiLogItem
import com.simats.smartelectroai.api.AiSettingsResponse
import com.simats.smartelectroai.api.AuthResponse
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.UpdateAiSettingsRequest
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import androidx.compose.foundation.border

// Figma Exact Colors
private val AdminBlue = Color(0xFF1976D2)
private val AdminLightBlue = Color(0xFFE3F2FD)
private val AdminTextDark = Color(0xFF1E1E1E)
private val AdminTextSub = Color(0xFF757575)
private val AdminGreenText = Color(0xFF2E7D32)
private val AdminGreenBg = Color(0xFFE8F5E9)
private val AdminBorder = Color(0xFFEEEEEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    // State Variables
    var isEnabled by remember { mutableStateOf(false) }
    var gamingWeight by remember { mutableFloatStateOf(0f) }
    var cameraWeight by remember { mutableFloatStateOf(0f) }
    var batteryWeight by remember { mutableFloatStateOf(0f) }
    var budgetWeight by remember { mutableFloatStateOf(0f) }
    var engineMode by remember { mutableStateOf("Hybrid Mode") }
    var logs by remember { mutableStateOf<List<AiLogItem>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    val pageVisibleState = remember { MutableTransitionState(false) }

    // Fetch Data
    LaunchedEffect(Unit) {
        RetrofitClient.instance.getAiSettings("Bearer $token").enqueue(object : Callback<AiSettingsResponse> {
            override fun onResponse(call: Call<AiSettingsResponse>, response: Response<AiSettingsResponse>) {
                isLoading = false
                response.body()?.data?.let { data ->
                    isEnabled = data.is_enabled
                    gamingWeight = data.gaming.toFloat()
                    cameraWeight = data.camera.toFloat()
                    batteryWeight = data.battery.toFloat()
                    budgetWeight = data.budget.toFloat()
                    engineMode = data.engine_mode
                    logs = data.logs
                }
                pageVisibleState.targetState = true // Trigger page animations
            }
            override fun onFailure(call: Call<AiSettingsResponse>, t: Throwable) { isLoading = false }
        })
    }

    // Save Function
    fun saveSettings() {
        val req = UpdateAiSettingsRequest(isEnabled, gamingWeight.toInt(), cameraWeight.toInt(), batteryWeight.toInt(), budgetWeight.toInt(), engineMode)
        RetrofitClient.instance.updateAiSettings("Bearer $token", req).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                Toast.makeText(context, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {}
        })
    }

    // CSV Export Function
    fun exportToCsv() {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AILogs_Export.csv")
            val writer = FileWriter(file)
            writer.append("User ID,Preferences,Product,Match %,Date\n")
            logs.forEach { writer.append("${it.log_id},\"${it.preferences}\",\"${it.product}\",${it.match_percent}%,${it.date}\n") }
            writer.flush()
            writer.close()
            Toast.makeText(context, "Exported to Downloads: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Recommendation\nSettings", fontWeight = FontWeight.Bold, color = AdminTextDark, fontSize = 18.sp, lineHeight = 22.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AdminTextDark) } },
                actions = {
                    TextButton(onClick = { saveSettings() }) {
                        Text("Save\nChanges", color = AdminBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { AdminBottomNavBar("AdminSettings", onNavigate) },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AdminBlue) }
        } else {
            AnimatedVisibility(
                visibleState = pageVisibleState,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 8 }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // 1. ENGINE TOGGLE CARD
                    item {
                        val cardElevation by animateDpAsState(if (isEnabled) 8.dp else 2.dp, tween(500), label = "")
                        Card(
                            elevation = CardDefaults.cardElevation(cardElevation),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, AdminBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("AI Recommendation Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AdminTextDark)
                                    Text("Enable or disable core logic", color = AdminTextSub, fontSize = 13.sp)
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AdminBlue, uncheckedTrackColor = Color.LightGray)
                                )
                            }
                        }
                    }

                    // 2. AI WEIGHT SETTINGS
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = AdminBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("AI WEIGHT SETTINGS", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            AnimatedSliderRow("Gaming Priority", gamingWeight, { gamingWeight = it }, pageVisibleState.targetState)
                            AnimatedSliderRow("Camera Priority", cameraWeight, { cameraWeight = it }, pageVisibleState.targetState)
                            AnimatedSliderRow("Battery Priority", batteryWeight, { batteryWeight = it }, pageVisibleState.targetState)
                            AnimatedSliderRow("Budget Weight", budgetWeight, { budgetWeight = it }, pageVisibleState.targetState)
                        }
                    }

                    // 3. RECOMMENDATION LOGIC
                    item {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val arrowRotation by animateFloatAsState(if (dropdownExpanded) 180f else 0f, label = "")

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = AdminBlue, modifier = Modifier.size(18.dp)) // Placeholder icon
                                Spacer(Modifier.width(8.dp))
                                Text("RECOMMENDATION LOGIC", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Engine Algorithm", color = AdminTextSub, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))

                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(1.dp, AdminBorder, RoundedCornerShape(8.dp)).clickable { dropdownExpanded = !dropdownExpanded }.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(engineMode, fontWeight = FontWeight.Bold, color = AdminTextDark)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.rotate(arrowRotation), tint = AdminTextSub)
                                }
                                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }, modifier = Modifier.background(Color.White)) {
                                    listOf("Hybrid Mode", "Performance Mode", "Budget Saver Mode").forEach { mode ->
                                        DropdownMenuItem(text = { Text(mode) }, onClick = { engineMode = mode; dropdownExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    // 4. AI LOGS
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = AdminBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("AI LOGS", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                            // Export Button with press animation
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val btnScale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(stiffness = Spring.StiffnessLow), label = "")

                            Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(8.dp)).background(AdminLightBlue).clickable(interactionSource = interactionSource, indication = null) { exportToCsv() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("EXPORT CSV", color = AdminBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // Table Header
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Text("User ID", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.2f))
                            Text("Preferences", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.3f))
                            Text("Product", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.25f))
                            Text("Match", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.15f))
                            Text("Date", color = AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.15f))
                        }
                    }

                    // Table Items with Staggered Entry
                    itemsIndexed(logs) { index, log ->
                        AnimatedVisibility(
                            visible = pageVisibleState.targetState,
                            enter = slideInHorizontally(tween(400, delayMillis = index * 100)) { it / 2 } + fadeIn(tween(400, delayMillis = index * 100))
                        ) {
                            Column {
                                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.log_id, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AdminTextDark, modifier = Modifier.weight(0.2f))
                                    Text(log.preferences, fontSize = 12.sp, color = AdminTextSub, modifier = Modifier.weight(0.3f))
                                    Text(log.product, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AdminBlue, modifier = Modifier.weight(0.25f))

                                    // Animated Badge
                                    Box(modifier = Modifier.weight(0.15f)) {
                                        Box(modifier = Modifier.background(AdminGreenBg, RoundedCornerShape(12.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("${log.match_percent}%", color = AdminGreenText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
                                    }
                                    Text(log.date, fontSize = 12.sp, color = AdminTextSub, modifier = Modifier.weight(0.15f))
                                }
                                HorizontalDivider(color = AdminBorder)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) } // Bottom padding
                }
            }
        }
    }
}

// Custom Animated Slider matching Figma
@Composable
fun AnimatedSliderRow(title: String, value: Float, onValueChange: (Float) -> Unit, isLoaded: Boolean) {
    // Animate the float value smoothly on load
    val animatedValue by animateFloatAsState(targetValue = if (isLoaded) value else 0f, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "")

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Bold, color = AdminTextDark, fontSize = 14.sp)
            Text("${animatedValue.toInt()}%", fontWeight = FontWeight.Bold, color = AdminBlue, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = AdminBlue,
                activeTrackColor = AdminBlue,
                inactiveTrackColor = AdminBorder
            ),
            modifier = Modifier.fillMaxWidth().height(24.dp)
        )
    }
}