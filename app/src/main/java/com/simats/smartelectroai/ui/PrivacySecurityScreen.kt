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
import com.simats.smartelectroai.api.ApiConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- LOCAL DATA MODELS & API ---
private data class GenericApiResponse(val status: String, val message: String)

private interface PrivacySecurityApiService {
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

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    val api = remember {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrivacySecurityApiService::class.java)
    }

    LaunchedEffect(Unit) {
        isScreenVisible = true
    }

    fun handleLogoutAll() {
        if (token.isNullOrEmpty()) return
        api.logoutAllDevices("Bearer $token").enqueue(object : Callback<GenericApiResponse> {
            override fun onResponse(call: Call<GenericApiResponse>, response: Response<GenericApiResponse>) {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(context, "Logged out from all devices", Toast.LENGTH_SHORT).show()
                onNavigate("Login")
            }
            override fun onFailure(call: Call<GenericApiResponse>, t: Throwable) {
                Toast.makeText(context, "Logout failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun handleDeleteAccount() {
        if (token.isNullOrEmpty()) return
        api.deleteAccount("Bearer $token").enqueue(object : Callback<GenericApiResponse> {
            override fun onResponse(call: Call<GenericApiResponse>, response: Response<GenericApiResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(context, "Account permanently deleted", Toast.LENGTH_LONG).show()
                    onNavigate("Login")
                } else {
                    Toast.makeText(context, "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GenericApiResponse>, t: Throwable) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
            }
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: DATA & PRIVACY
            AnimatedVisibility(visible = isScreenVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(600))) {
                Column {
                    PrivacySectionHeader("DATA & PRIVACY")
                    PrivacyElevatedCardContainer {
                        PrivacyActionRow(Icons.AutoMirrored.Filled.Notes, "Privacy Policy") { onNavigate("PrivacyPolicy") }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        PrivacyActionRow(Icons.Default.Description, "Terms & Conditions") { onNavigate("TermsConditions") }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        PrivacyActionRow(Icons.Outlined.DeleteOutline, "Delete Account", isDestructive = true) { handleDeleteAccount() }
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
                            Text("This will securely log you out of your account on all Web and Mobile platforms.", fontSize = 11.sp, color = TextSub, textAlign = TextAlign.Center, lineHeight = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- REUSABLE COMPOSABLES ---
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