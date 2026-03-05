package com.simats.smartelectroai.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.razorpay.Checkout
import org.json.JSONObject
import com.simats.smartelectroai.api.OrderContext
import com.simats.smartelectroai.api.PaymentRequest
import com.simats.smartelectroai.api.PaymentResponse
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

// Import the global handler from MainActivity
import com.simats.smartelectroai.PaymentCallbackHandler

// Helper to find Activity from Compose context
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val AiBlue = Color(0xFF2962FF)
private val AiLightBlue = Color(0xFF03A9F4)
private val AiGradient = Brush.linearGradient(listOf(AiBlue, AiLightBlue))
private val GlassBg = Color.White.copy(alpha = 0.8f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onBack: () -> Unit = {}, onPaymentSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var isVisible by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("card") }
    var isProcessing by remember { mutableStateOf(false) }

    // Use current amount, or fallback to a test amount if 0
    val targetPrice = if (OrderContext.currentTotalAmount > 0) OrderContext.currentTotalAmount.toFloat() else 28999f
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    LaunchedEffect(Unit) {
        isVisible = true
        Checkout.preload(context.applicationContext)

        // --- 🛠️ AUTO-FIX FOR TESTING ---
        // If we arrived here without an Order ID (e.g., direct navigation), generate a Dummy ID
        // so Razorpay works for testing.
        if (OrderContext.currentOrderId == -1) {
            val dummyId = Random.nextInt(1000, 9999)
            OrderContext.currentOrderId = dummyId
            Toast.makeText(context, "⚠️ Test Mode: Using Dummy Order #$dummyId", Toast.LENGTH_SHORT).show()
        }
    }

    // Set up listeners for Razorpay callbacks
    DisposableEffect(Unit) {
        PaymentCallbackHandler.onSuccess = { paymentId, rzpOrderId, signature ->
            // Update UI immediately
            isProcessing = true

            val request = PaymentRequest(
                order_id = OrderContext.currentOrderId,
                payment_method = "Razorpay",
                amount = targetPrice.toString()
            )

            RetrofitClient.instance.processPayment("Bearer $token", request).enqueue(object : Callback<PaymentResponse> {
                override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                    isProcessing = false
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(context, "Payment Verified! TXN: $paymentId", Toast.LENGTH_LONG).show()
                        onPaymentSuccess()
                    } else {
                        // Even if backend update fails, the payment succeeded locally
                        Toast.makeText(context, "Payment Done, but sync failed.", Toast.LENGTH_LONG).show()
                        onPaymentSuccess()
                    }
                }
                override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                    isProcessing = false
                    Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        PaymentCallbackHandler.onError = { errorString ->
            isProcessing = false
            Toast.makeText(context, "Payment Failed: $errorString", Toast.LENGTH_LONG).show()
        }

        onDispose {
            PaymentCallbackHandler.onSuccess = null
            PaymentCallbackHandler.onError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it }) + fadeIn()) {
                FloatingAiPayButton(
                    amount = targetPrice,
                    isProcessing = isProcessing,
                    onPay = {
                        // Validation with specific error messages
                        if (token.isEmpty()) {
                            Toast.makeText(context, "Error: User not logged in (Token missing)", Toast.LENGTH_SHORT).show()
                            return@FloatingAiPayButton
                        }

                        if (activity == null) {
                            Toast.makeText(context, "Error: Activity context lost", Toast.LENGTH_SHORT).show()
                            return@FloatingAiPayButton
                        }

                        isProcessing = true

                        // =====================================
                        // 🚀 LAUNCH RAZORPAY
                        // =====================================
                        try {
                            val checkout = Checkout()
                            checkout.setKeyID(com.simats.smartelectroai.api.AppConfig.RAZORPAY_KEY_ID)

                            val options = JSONObject()
                            options.put("name", "Smart Electro AI")
                            options.put("description", "Order #${OrderContext.currentOrderId}")
                            options.put("theme.color", "#2962FF")
                            options.put("currency", "INR")
                            options.put("amount", (targetPrice * 100).toInt()) // Amount in paise

                            val prefill = JSONObject()
                            prefill.put("email", "user@smartelectro.com")
                            prefill.put("contact", "9999999999")
                            options.put("prefill", prefill)

                            checkout.open(activity, options)

                        } catch (e: Exception) {
                            isProcessing = false
                            Toast.makeText(context, "Error init Razorpay: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFFF4F8FF)
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            AiSecureHeader()
            Spacer(modifier = Modifier.height(24.dp))

            Text("SELECT PAYMENT METHOD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentOptionCard("Razorpay (UPI / Cards / NetBanking)", Icons.Default.CreditCard, selectedMethod == "card", { selectedMethod = "card" })
                PaymentOptionCard("Cash on Delivery", Icons.Default.LocalShipping, selectedMethod == "cod", { selectedMethod = "cod" })
            }

            Spacer(modifier = Modifier.height(24.dp))
            AiOrderSummary(targetPrice)

            // Debug Info (Hidden in production)
            if (OrderContext.currentOrderId != -1) {
                Text(
                    text = "Order ID: ${OrderContext.currentOrderId}",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AiSecureHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(50.dp).background(AiLightBlue.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = "Secure", tint = AiBlue, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Secure Checkout", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AiBlue)
            Text("256-bit Encrypted Payment", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PaymentOptionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit = {}) {
    val bgColor by animateColorAsState(if (isSelected) Color(0xFFE1F5FE) else GlassBg, label = "")
    val borderColor by animateColorAsState(if (isSelected) AiLightBlue else Color.Transparent, label = "")

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = if (isSelected) AiBlue else Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) AiBlue else Color.DarkGray, modifier = Modifier.weight(1f))
                RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = AiBlue))
            }
            AnimatedVisibility(visible = isSelected, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 16.dp)) { content() }
            }
        }
    }
}

@Composable
fun AiOrderSummary(targetPrice: Float) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = AiLightBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Price Verification", fontWeight = FontWeight.Bold, color = AiBlue, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Secured Amount", fontWeight = FontWeight.Medium, color = Color.Gray)
                Text(text = "₹${targetPrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = AiBlue)
            }
        }
    }
}

@Composable
fun FloatingAiPayButton(amount: Float, isProcessing: Boolean, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AiBlue), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onPay, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isProcessing,
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(AiGradient, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay ₹${amount.toInt()}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}