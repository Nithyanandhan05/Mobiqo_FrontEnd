package com.simats.smartelectroai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.OrderTrackingManager
import kotlinx.coroutines.delay
import android.util.Log

private val AmzGreen = Color(0xFF067D62)
private val AmzGray = Color(0xFF565959)
private val AmzBorder = Color(0xFFD5D9D9)
private val AmzBg = Color(0xFFF2F4F8)
private val AmzDark = Color(0xFF0F1111)

fun titleToStep(title: String): Int {
    return when (title.lowercase()) {
        "order confirmed" -> 0
        "shipped" -> 1
        "out for delivery" -> 2
        "delivered" -> 3
        else -> 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderInvoiceScreen(onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        Log.d("SCREEN", "OrderInvoiceScreen opened")
    }

    val order = OrderTrackingManager.currentOrder

    // 🚀 FIXED: Handle null order gracefully instead of returning a blank screen
    if (order == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Order Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmzGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading order details...", color = AmzGray)
                    // Note: If it stays stuck here, it means OrderTrackingManager.currentOrder was never set!
                }
            }
        }
        return
    }

    val statusTitle = when {
        order.status.contains("deliver", true) -> "Delivered"
        order.status.contains("out", true) -> "Out for Delivery"
        order.status.contains("ship", true) -> "Shipped"
        else -> "Order Confirmed"
    }

    val activeStep = titleToStep(statusTitle)
    val price = order.raw_price ?: order.price.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    val listingPrice = price * 1.15
    val discount = listingPrice - price
    val fees = 39.0

    Scaffold(
        containerColor = AmzBg,
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // HEADER
            Column(Modifier.fillMaxWidth().background(Color.White).padding(20.dp)) {
                Text(
                    statusTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (statusTitle == "Delivered") AmzGreen else AmzDark
                )
            }

            HorizontalOrderTracker(activeStep)
            Spacer(Modifier.height(8.dp))

            // PRODUCT
            Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(80.dp).border(1.dp, AmzBorder, RoundedCornerShape(8.dp)).padding(8.dp)) {
                        AsyncImage(
                            model = order.image_url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(order.product_name)
                        Spacer(Modifier.height(6.dp))
                        Text("₹${price.toInt()}", fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
            Spacer(Modifier.height(8.dp))

            // ADDRESS
            Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text("Delivery details", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(order.delivery_name ?: "")
                Text(order.delivery_address ?: "")
                Text(order.delivery_phone ?: "")
            }
            Spacer(Modifier.height(8.dp))

            // PRICE
            Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text("Price details", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                InvoicePriceRow("Listing price", "₹${listingPrice.toInt()}")
                InvoicePriceRow("Special price", "-₹${discount.toInt()}", true)
                InvoicePriceRow("Delivery fee", "₹${fees.toInt()}")
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total amount", fontWeight = FontWeight.Bold)
                    Text("₹${price.toInt()}", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun HorizontalOrderTracker(activeStep: Int) {
    val labels = listOf("Confirmed", "Shipped", "Out for\nDelivery", "Delivered")
    val progress = remember { Animatable(-0.5f) }

    LaunchedEffect(activeStep) {
        progress.snapTo(-0.5f)
        delay(300)
        progress.animateTo(activeStep.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i ->
                TrackerNode(i, progress.value)
                if (i != 3) TrackerLine(i, progress.value, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    color = if (i <= activeStep) AmzGreen else AmzGray
                )
            }
        }
    }
}

@Composable
fun TrackerNode(index: Int, progress: Float) {
    val reached = progress >= index
    val scale by animateFloatAsState(if (reached) 1f else 0.7f, animationSpec = spring(), label = "")

    Box(
        Modifier.size(28.dp).scale(scale).background(if (reached) AmzGreen else Color.LightGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (reached) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun TrackerLine(index: Int, progress: Float, modifier: Modifier) {
    val fill = (progress - index).coerceIn(0f, 1f)
    Box(modifier.height(3.dp).background(Color.LightGray)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(AmzGreen))
    }
}

@Composable
private fun InvoicePriceRow(label: String, value: String, isDiscount: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = AmzGray)
        Text(
            value,
            fontSize = 14.sp,
            color = if (isDiscount) Color(0xFFB12704) else AmzDark,
            fontWeight = if (isDiscount) FontWeight.Bold else FontWeight.Medium
        )
    }
}