package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.AdminDashboardResponse
import com.simats.smartelectroai.api.AdminOrder
import com.simats.smartelectroai.api.RetrofitClient
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// COLORS
private val BlueMain = Color(0xFF1976D2)
private val LightGray = Color(0xFFF5F5F5)
private val TextGray = Color(0xFF757575)
private val Green = Color(0xFF4CAF50)
private val Orange = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val visibleState = remember { MutableTransitionState(false) }
    var isLoading by remember { mutableStateOf(true) }
    var dashboardData by remember { mutableStateOf<AdminDashboardResponse?>(null) }

    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val token = sharedPrefs.getString("jwt_token", "") ?: ""

    // 🚀 FIXED: The exact route to match your MainActivity.kt
    val handleLogout = {
        sharedPrefs.edit().remove("jwt_token").apply()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        onNavigate("Login") // Matches "Login" in MainActivity.kt perfectly!
    }

    LaunchedEffect(Unit) {
        // 🚀 FIXED: Stop the 422 Error before it happens
        if (token.isBlank()) {
            onNavigate("Login")
            return@LaunchedEffect
        }

        RetrofitClient.instance.getAdminDashboard("Bearer $token").enqueue(object : Callback<AdminDashboardResponse> {
            override fun onResponse(call: Call<AdminDashboardResponse>, response: Response<AdminDashboardResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    dashboardData = response.body()
                } else if (response.code() == 422 || response.code() == 401) {
                    // Token is expired or invalid. Force logout gracefully.
                    handleLogout()
                } else {
                    Toast.makeText(context, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
                visibleState.targetState = true
            }
            override fun onFailure(call: Call<AdminDashboardResponse>, t: Throwable) {
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                isLoading = false
                visibleState.targetState = true
            }
        })
    }

    Scaffold(
        topBar = { EnterpriseTopBar(visibleState, onLogout = handleLogout) },
        bottomBar = { AdminBottomNavBar("AdminDashboard", onNavigate) },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueMain)
            }
        } else {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { it / 8 }, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        val stats = dashboardData?.stats
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Total Users", stats?.total_users ?: "0", "Live", Icons.Default.Group, visibleState, onClick = { onNavigate("AdminUsers") })
                                StatCard("Total Orders", stats?.total_orders ?: "0", "Live", Icons.Default.ShoppingBag, visibleState, onClick = { onNavigate("AdminOrderManagement") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("Active Warranty", stats?.active_warranties ?: "0", "Secure", Icons.Default.Verified, visibleState, onClick = { onNavigate("AdminWarranty") })
                                StatCard("AI Recommend", stats?.ai_searches ?: "0", "+12%", Icons.Default.AutoAwesome, visibleState)
                            }
                        }
                    }
                    item { AiRecommendationCard(visibleState) }
                    item { AdminSectionHeader("Recent Orders", onNavigate) }
                    val recentOrders = dashboardData?.recent_orders ?: emptyList()
                    if (recentOrders.isEmpty()) {
                        item { Text("No orders found.", color = Color.Gray) }
                    } else {
                        itemsIndexed(recentOrders) { index, order ->
                            AnimatedVisibility(visibleState = visibleState, enter = slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(durationMillis = 500, delayMillis = 200 + (index * 150), easing = FastOutSlowInEasing)) + fadeIn(tween(delayMillis = 200 + (index * 150)))) {
                                OrderItem(order)
                            }
                        }
                    }
                    item { ExpiringWarrantyCard(visibleState) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterpriseTopBar(state: MutableTransitionState<Boolean>, onLogout: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(visibleState = state, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it }) {
        TopAppBar(
            title = { Text("EnterpriseAI", fontWeight = FontWeight.Bold) },
            actions = {
                // Profile Icon with Dropdown Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.AccountCircle, "Profile")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showMenu = false
                                onLogout()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red)
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.StatCard(title: String, value: String, change: String, icon: androidx.compose.ui.graphics.vector.ImageVector, state: MutableTransitionState<Boolean>, onClick: () -> Unit = {}) {
    val elevation by animateDpAsState(targetValue = if (state.targetState) 4.dp else 0.dp, animationSpec = spring(stiffness = Spring.StiffnessVeryLow), label = "")
    AnimatedVisibility(visibleState = state, enter = slideInVertically(tween(500)) { it / 2 } + fadeIn(tween(500)), modifier = Modifier.weight(1f)) {
        Card(onClick = onClick, elevation = CardDefaults.cardElevation(elevation), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = BlueMain); Spacer(Modifier.height(8.dp)); Text(title, color = TextGray, fontSize = 12.sp); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(change, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AiRecommendationCard(state: MutableTransitionState<Boolean>) {
    val percent by animateFloatAsState(if (state.targetState) 94.2f else 0f, tween(1500), label = "")
    val progress by animateFloatAsState(if (state.targetState) 0.942f else 0f, tween(1500), label = "")
    AnimatedVisibility(visibleState = state, enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(800)) + fadeIn(tween(800))) {
        Card(colors = CardDefaults.cardColors(containerColor = BlueMain), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) { Text("AI Accuracy Rating", color = Color.White); Spacer(Modifier.height(8.dp)); Text("${"%.1f".format(percent)}%", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f)) }
        }
    }
}

@Composable
fun OrderItem(order: AdminOrder) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LightGray), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(text = order.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(modifier = Modifier.height(4.dp)); Text("Status: ${order.status}", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = order.price, color = BlueMain, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
        }
    }
}

@Composable
fun ExpiringWarrantyCard(state: MutableTransitionState<Boolean>) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    AnimatedVisibility(visibleState = state, enter = scaleIn(initialScale = 0.9f, animationSpec = tween(500)) + fadeIn(tween(500))) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) { Text("Action Required", color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("2 Warranties Expiring Soon", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp)); Button(onClick = { pressed = true }, modifier = Modifier.scale(scale), colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text("SEND ALERTS", fontWeight = FontWeight.Bold) } }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
private fun AdminSectionHeader(text: String, onNavigate: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { onNavigate("AdminOrderManagement") }) {
            Text("View All", color = BlueMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminBottomNavBar(currentScreen: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        val items = listOf(
            Triple("AdminDashboard", Icons.Default.GridView, "Dashboard"),
            Triple("AdminOrderManagement", Icons.Default.ShoppingBag, "Orders"),
            Triple("AdminPaymentScreen", Icons.Default.AttachMoney, "Payments"),
            Triple("AdminWarranty", Icons.Default.VerifiedUser, "Warranty"),
            Triple("AdminUsers", Icons.Default.Group, "Users")
        )
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentScreen == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1976D2), indicatorColor = Color(0xFFF5F5F5))
            )
        }
    }
}