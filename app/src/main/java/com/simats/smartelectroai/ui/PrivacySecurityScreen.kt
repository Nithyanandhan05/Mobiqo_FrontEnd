package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- LOCAL DATA MODELS & API ---
private data class PrivacySettingsData(val two_factor_auth: Boolean, val biometric_login: Boolean)
private data class PrivacyFetchResponse(val status: String, val settings: PrivacySettingsData?)
private data class GenericApiResponse(val status: String, val message: String)

private interface PrivacySecurityApiService {
    @GET("/privacy/settings")
    fun getSettings(@Header("Authorization") token: String): Call<PrivacyFetchResponse>

    @PUT("/privacy/settings")
    fun updateSettings(@Header("Authorization") token: String, @Body settings: PrivacySettingsData): Call<GenericApiResponse>

    @POST("/auth/logout-all")
    fun logoutAllDevices(@Header("Authorization") token: String): Call<GenericApiResponse>

    @DELETE("/auth/delete-account")
    fun deleteAccount(@Header("Authorization") token: String): Call<GenericApiResponse>
}

// --- Thematic Colors ---
private val PrimaryBlue = Color(0xFF1976D2)
private val BgWhite = Color(0xFFFFFFFF)
private val BgApp = Color(0xFFFAFAFA)
private val TextMain = Color(0xFF1E1E1E)
private val TextSub = Color(0xFF757575)
private val DangerRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isScreenVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // States
    var twoFactorAuth by remember { mutableStateOf(false) }
    var biometricLogin by remember { mutableStateOf(false) }

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    val api = remember {
        Retrofit.Builder().baseUrl("http://10.156.35.203:5000/").addConverterFactory(GsonConverterFactory.create()).build().create(PrivacySecurityApiService::class.java)
    }

    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            api.getSettings("Bearer $token").enqueue(object : Callback<PrivacyFetchResponse> {
                override fun onResponse(call: Call<PrivacyFetchResponse>, response: Response<PrivacyFetchResponse>) {
                    isLoading = false
                    response.body()?.settings?.let {
                        twoFactorAuth = it.two_factor_auth
                        biometricLogin = it.biometric_login
                    }
                    isScreenVisible = true
                }
                override fun onFailure(call: Call<PrivacyFetchResponse>, t: Throwable) {
                    isLoading = false; isScreenVisible = true
                }
            })
        } else { isLoading = false; isScreenVisible = true }
    }

    fun saveSettings() {
        if (token.isNullOrEmpty()) return
        api.updateSettings("Bearer $token", PrivacySettingsData(twoFactorAuth, biometricLogin)).enqueue(object : Callback<GenericApiResponse> {
            override fun onResponse(call: Call<GenericApiResponse>, response: Response<GenericApiResponse>) {}
            override fun onFailure(call: Call<GenericApiResponse>, t: Throwable) {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun handleLogoutAll() {
        api.logoutAllDevices("Bearer $token").enqueue(object : Callback<GenericApiResponse> {
            override fun onResponse(call: Call<GenericApiResponse>, response: Response<GenericApiResponse>) {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                onNavigate("Login")
            }
            override fun onFailure(call: Call<GenericApiResponse>, t: Throwable) {}
        })
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Privacy & Security", fontWeight = FontWeight.Bold, color = TextMain) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        containerColor = BgApp
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 1: ACCOUNT SECURITY
                AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(400))) {
                    Column {
                        PrivacySectionHeader("ACCOUNT SECURITY")
                        PrivacyElevatedCardContainer {
                            PrivacyActionRow(Icons.Outlined.Lock, "Change Password") { onNavigate("ChangePassword") }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            PrivacyToggleRow(Icons.Default.Security, "Two-Factor Authentication", twoFactorAuth) { twoFactorAuth = it; saveSettings() }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            PrivacyToggleRow(Icons.Default.Fingerprint, "Biometric Login", biometricLogin) { biometricLogin = it; saveSettings() }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 2: DATA & PRIVACY
                AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(600))) {
                    Column {
                        PrivacySectionHeader("DATA & PRIVACY")
                        PrivacyElevatedCardContainer {
                            PrivacyActionRow(Icons.AutoMirrored.Filled.Notes, "Privacy Policy") { /* Navigate to webview */ }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            PrivacyActionRow(Icons.Default.Description, "Terms & Conditions") { /* Navigate to webview */ }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            PrivacyActionRow(Icons.Outlined.DeleteOutline, "Delete Account", isDestructive = true) { /* Show Dialog -> API Call */ }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // SECTION 3: SESSION MANAGEMENT
                AnimatedVisibility(visible = isScreenVisible, enter = scaleIn(tween(800)) + fadeIn()) {
                    Column {
                        PrivacySectionHeader("SESSION MANAGEMENT")
                        Card(
                            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite),
                            elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedButton(
                                    onClick = { handleLogoutAll() },
                                    border = BorderStroke(1.5.dp, PrimaryBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGOUT FROM ALL DEVICES", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("This will end all current sessions except for this device. You will need to log back in on other platforms.", fontSize = 11.sp, color = TextSub, textAlign = TextAlign.Center, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- RENAMED REUSABLE COMPOSABLES ---

@Composable
private fun PrivacySectionHeader(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9E9E9E), letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
}

@Composable
private fun PrivacyElevatedCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun PrivacyActionRow(icon: ImageVector, title: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "")
    val color = if (isDestructive) DangerRed else TextMain

    Row(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }.padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun PrivacyToggleRow(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackColor by animateColorAsState(if (isChecked) PrimaryBlue else Color(0xFFE0E0E0), label = "")

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextMain, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = trackColor, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFEEEEEE), uncheckedBorderColor = Color.Transparent),
            modifier = Modifier.scale(0.85f)
        )
    }
}