package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.AdminPaymentItem
import com.simats.smartelectroai.api.AdminPaymentsResponse
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val AdminBlue = Color(0xFF1976D2)
private val AdminBg = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    var payments by remember { mutableStateOf<List<AdminPaymentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        RetrofitClient.instance.getAdminPayments("Bearer $token").enqueue(object : Callback<AdminPaymentsResponse> {
            override fun onResponse(call: Call<AdminPaymentsResponse>, response: Response<AdminPaymentsResponse>) {
                isLoading = false
                if (response.isSuccessful) payments = response.body()?.payments ?: emptyList()
            }
            override fun onFailure(call: Call<AdminPaymentsResponse>, t: Throwable) { isLoading = false }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { onNavigate("AdminDashboard") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { refreshTrigger++ }) { Icon(Icons.Default.Refresh, "Refresh") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { AdminBottomNavBar("AdminPaymentScreen", onNavigate) },
        containerColor = AdminBg
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AdminBlue) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(payments) { payment ->
                    AdminPaymentCard(payment, token) { refreshTrigger++ }
                }
            }
        }
    }
}

@Composable
fun AdminPaymentCard(payment: AdminPaymentItem, token: String, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var isRefunding by remember { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CreditCard, null, tint = AdminBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(payment.user_name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(payment.transaction_id, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Text(payment.amount, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AdminBlue)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("ORDER ID", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(payment.order_id, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (payment.status == "Refunded") {
                    Text("REFUNDED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                } else if (payment.status == "Successful" || payment.status == "Completed" || payment.status == "Paid & Processing") {
                    Button(
                        onClick = {
                            isRefunding = true
                            RetrofitClient.instance.refundPayment("Bearer $token", payment.id).enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                    Toast.makeText(context, "Refund Initiated", Toast.LENGTH_SHORT).show()
                                    onRefresh()
                                    isRefunding = false
                                }
                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isRefunding = false }
                            })
                        },
                        enabled = !isRefunding,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isRefunding) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("REFUND", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(payment.status.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }
    }
}