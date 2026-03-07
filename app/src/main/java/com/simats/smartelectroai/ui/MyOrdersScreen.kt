package com.simats.smartelectroai.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.MyOrdersResponse
import com.simats.smartelectroai.api.OrderHistoryItem
import com.simats.smartelectroai.api.OrderTrackingManager
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var orders by remember { mutableStateOf<List<OrderHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getMyOrders("Bearer $token").enqueue(object : Callback<MyOrdersResponse> {
                override fun onResponse(call: Call<MyOrdersResponse>, response: Response<MyOrdersResponse>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        orders = response.body()?.orders ?: emptyList()
                        visibleState.targetState = true
                    }
                }
                override fun onFailure(call: Call<MyOrdersResponse>, t: Throwable) { isLoading = false }
            })
        } else isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F3F6)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
                itemsIndexed(orders, key = { _, it -> it.order_id }) { index, order ->
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(durationMillis = 400, delayMillis = index * 50)) +
                                fadeIn(tween(delayMillis = index * 50))
                    ) {
                        OrderCard(order) {
                            OrderTrackingManager.currentOrder = order
                            onNavigate("TrackOrder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderHistoryItem, onClick: () -> Unit) {
    val statusColor = when {
        order.status.contains("Delivered", true) -> Color(0xFF388E3C)
        order.status.contains("Shipped", true) -> Color(0xFFF57C00)
        order.status.contains("Pending", true) -> Color(0xFFD32F2F)
        else -> Color(0xFF2874F0)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(70.dp).padding(4.dp)) {
                AsyncImage(model = order.image_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.status, fontWeight = FontWeight.Bold, color = statusColor)
                Text("On ${order.date}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(order.product_name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${order.price.replace(Regex("[^0-9.]"), "")}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.Black)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
        }
    }
}