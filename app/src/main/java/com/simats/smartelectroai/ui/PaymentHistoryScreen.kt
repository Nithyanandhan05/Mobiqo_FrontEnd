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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.PaymentHistoryItem
import com.simats.smartelectroai.api.PaymentHistoryResponse
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- Thematic Colors ---
private val PayBlue = Color(0xFF1976D2)
private val PayLightBlue = Color(0xFFE3F2FD)
private val PayTextMain = Color(0xFF1E1E1E)
private val PayTextSub = Color(0xFF757575)
private val BgWhite = Color(0xFFFFFFFF)

// Status Colors
private val StatusGreenBg = Color(0xFFE8F5E9)
private val StatusGreenText = Color(0xFF2E7D32)
private val StatusOrangeBg = Color(0xFFFFF3E0)
private val StatusOrangeText = Color(0xFFEF6C00)
private val StatusRedBg = Color(0xFFFFEBEE)
private val StatusRedText = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Successful", "Pending", "Failed")

    var historyList by remember { mutableStateOf<List<PaymentHistoryItem>>(emptyList()) }
    val visibleState = remember { MutableTransitionState(false) }

    // Fetch from Global RetrofitClient
    LaunchedEffect(Unit) {
        val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getPaymentHistory("Bearer $token").enqueue(object : Callback<PaymentHistoryResponse> {
                override fun onResponse(call: Call<PaymentHistoryResponse>, response: Response<PaymentHistoryResponse>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        historyList = response.body()?.history ?: emptyList()

                        // FALLBACK FOR UI TESTING (Remove this when backend is ready)
                        if (historyList.isEmpty()) {
                            historyList = listOf(
                                PaymentHistoryItem(1, "ORD-10024", "TXN-AB892", "Credit Card", "₹84,999", "Oct 12, 10:30 AM", "Successful"),
                                PaymentHistoryItem(2, "ORD-10025", "TXN-CD113", "UPI", "₹12,499", "Oct 14, 02:15 PM", "Pending"),
                                PaymentHistoryItem(3, "ORD-10026", "TXN-EF554", "Wallet", "₹1,999", "Oct 15, 09:00 AM", "Failed")
                            )
                        }

                        visibleState.targetState = true
                    }
                }
                override fun onFailure(call: Call<PaymentHistoryResponse>, t: Throwable) {
                    isLoading = false
                    visibleState.targetState = true
                }
            })
        } else {
            isLoading = false
            visibleState.targetState = true
        }
    }

    // Filter Logic
    val filteredList = historyList.filter { item ->
        (selectedFilter == "All" || item.status.equals(selectedFilter, ignoreCase = true)) &&
                (item.order_id.contains(searchQuery, ignoreCase = true) || item.transaction_id.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History", fontWeight = FontWeight.Bold, color = PayTextMain) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite)
            )
        },
        bottomBar = { PrivateFloatingAiDock(onNavigate) },
        containerColor = BgWhite
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // --- SEARCH BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = PayTextSub)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = PayTextMain, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (searchQuery.isEmpty()) Text("Search by Order or Txn ID", color = Color.Gray, fontSize = 14.sp) else inner() }
                    )
                }
            }

            // --- FILTER ROW ---
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PayBlue else PayLightBlue)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(filter, color = if (isSelected) Color.White else PayBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PayBlue) }
            } else if (filteredList.isEmpty()) {
                // --- EMPTY STATE ---
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AccountBalanceWallet, null, modifier = Modifier.size(64.dp), tint = Color(0xFFE0E0E0))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No payment transactions yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PayTextSub)
                    }
                }
            } else {
                // --- TRANSACTION LIST ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                ) {
                    itemsIndexed(filteredList) { index, item ->
                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = slideInHorizontally(
                                initialOffsetX = { it / 2 },
                                animationSpec = tween(durationMillis = 400, delayMillis = index * 50)
                            ) + fadeIn(tween(delayMillis = index * 50))
                        ) {
                            TransactionItem(item)
                        }
                    }

                    // --- SECURITY FOOTER ---
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All payments are secured with 256-bit encryption", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(item: PaymentHistoryItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "card_scale")

    // Dynamic UI based on status
    val (statusBg, statusText) = when (item.status.lowercase()) {
        "successful" -> StatusGreenBg to StatusGreenText
        "failed" -> StatusRedBg to StatusRedText
        else -> StatusOrangeBg to StatusOrangeText
    }

    // Dynamic Icon based on payment method
    val icon = when {
        item.payment_method.contains("UPI", ignoreCase = true) -> Icons.Outlined.QrCodeScanner
        item.payment_method.contains("Wallet", ignoreCase = true) -> Icons.Outlined.AccountBalanceWallet
        else -> Icons.Outlined.CreditCard
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PayLightBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null) {}
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(PayLightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PayBlue, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Details
            Column(modifier = Modifier.weight(1f)) {
                Text(item.order_id, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PayTextMain)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.payment_method, fontSize = 13.sp, color = PayTextSub)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.date, fontSize = 11.sp, color = Color.Gray)
                    Text(" • ", fontSize = 11.sp, color = Color.Gray)
                    Text(item.transaction_id, fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Right: Amount & Status
            Column(horizontalAlignment = Alignment.End) {
                Text(item.amount, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PayTextMain)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier.background(statusBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.status, fontSize = 10.sp, color = statusText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- KEEPING THE SAME NAVIGATION DOCK ---
@Composable
private fun PrivateFloatingAiDock(onNavigate: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp), spotColor = PayBlue),
            shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Dashboard") })
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Compare") })
                Box(modifier = Modifier.size(56.dp).background(Brush.linearGradient(listOf(PayBlue, Color(0xFF03A9F4))), CircleShape).shadow(8.dp, CircleShape).clickable { onNavigate("Chat") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("MyWarranty") })
                Icon(Icons.Default.Person, null, tint = PayBlue, modifier = Modifier.size(28.dp).clickable { onNavigate("Profile") })
            }
        }
    }
}