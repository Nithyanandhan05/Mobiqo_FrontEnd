package com.simats.smartelectroai.ui

import androidx.compose.foundation.BorderStroke // <-- FIXED IMPORT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.OrderTrackingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderInvoiceScreen(onBack: () -> Unit) {
    val order = OrderTrackingManager.currentOrder
    if (order == null) return

    // Fake calculations for visual detail
    val listingPrice = (order.raw_price ?: 0.0) * 1.15
    val discount = listingPrice - (order.raw_price ?: 0.0)
    val fees = 49.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            Text("Delivery details", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

            // Address Card
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Home, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(order.delivery_address ?: "No address provided", fontSize = 14.sp, color = Color(0xFF212121))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${order.delivery_name}  ${order.delivery_phone}", fontSize = 14.sp, color = Color(0xFF212121), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Price details", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

            // Price Details Card
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InvoicePriceRow("Listing price", "₹${listingPrice.toInt()}")
                    InvoicePriceRow("Special price", "-₹${discount.toInt()}", Color(0xFF388E3C)) // Green for discount
                    InvoicePriceRow("Total fees", "₹${fees.toInt()}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total amount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(order.price, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method Row
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Payment method", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("Online / UPI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// FIXED: Renamed to avoid conflicts with other files
@Composable
private fun InvoicePriceRow(label: String, value: String, color: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color(0xFF212121))
        Text(value, fontSize = 14.sp, color = color)
    }
}