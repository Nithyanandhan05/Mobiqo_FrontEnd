package com.simats.smartelectroai.ui

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- AI DESIGN SYSTEM COLORS ---
private val AiPrimary = Color(0xFF2962FF)
private val AiAccent = Color(0xFF03A9F4)
private val AiBackground = Color(0xFFF4F8FF)
private val AiGlassWhite = Color(0xD9FFFFFF)
private val AiTextMain = Color(0xFF1E1E2C)
private val AiSuccessGreen = Color(0xFF00C853)
private val AiGradient = Brush.linearGradient(listOf(AiPrimary, AiAccent))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCartScreen(onBack: () -> Unit = {}, onCheckoutSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val cartItems = CartManager.items
    var isOrdering by remember { mutableStateOf(false) }

    // --- NEW: Calculate Total including Warranty ---
    val baseTotal = CartManager.getTotal()
    val totalWarrantyCost = cartItems.sumOf { it.warrantyPrice * it.quantity }
    val finalAmount = baseTotal + totalWarrantyCost

    val totalSavings = CartManager.getSavings()
    val deliveryDate = remember { LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("EEE, MMM d")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Cart (${cartItems.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AiTextMain) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AiTextMain) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                FlipkartBottomBar(finalAmount, isOrdering) {
                    if (isOrdering) return@FlipkartBottomBar
                    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val token = prefs.getString("jwt_token", "")

                    if (token.isNullOrEmpty()) {
                        Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                        return@FlipkartBottomBar
                    }
                    isOrdering = true
                    // Note: Your backend placeOrder API might need updating to accept the new warrantyPrice if you want it saved in SQL.
                    placeOrdersRecursive(context, token, cartItems.toList(), 0, finalAmount, -1, onCheckoutSuccess)
                }
            }
        },
        containerColor = AiBackground
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            EmptyCartView(paddingValues)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems) { item ->
                    FlipkartCartItem(item, deliveryDate)
                }

                item { PriceDetailsView(cartItems.size, baseTotal + totalSavings, totalSavings, totalWarrantyCost, finalAmount) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Secured Payments. 100% Authentic.", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

fun placeOrdersRecursive(
    context: Context, token: String, items: List<CartItemModel>, index: Int, totalAmount: Int, validOrderId: Int, onSuccess: () -> Unit
) {
    if (index >= items.size) {
        OrderContext.currentTotalAmount = totalAmount
        OrderContext.currentOrderId = validOrderId
        onSuccess()
        return
    }
    val currentItem = items[index]
    RetrofitClient.instance.placeOrder("Bearer $token", OrderRequest(currentItem.id)).enqueue(object : Callback<OrderResponse> {
        override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
            val dbOrderId = response.body()?.order_id ?: validOrderId
            placeOrdersRecursive(context, token, items, index + 1, totalAmount, dbOrderId, onSuccess)
        }
        override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
            placeOrdersRecursive(context, token, items, index + 1, totalAmount, validOrderId, onSuccess)
        }
    })
}

@Composable
fun FlipkartCartItem(item: CartItemModel, deliveryDate: String) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(if (isPressed) 0.96f else 1f, tween(400, easing = FastOutSlowInEasing), label = "")
    val imageScale by animateFloatAsState(if (isVisible) 1f else 0.8f, tween(800, easing = FastOutSlowInEasing), label = "")
    val deliveryAlpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(800), label = "")

    val targetDiscount = if (item.originalPrice > 0) ((item.originalPrice - item.price).toFloat() / item.originalPrice * 100).toInt() else 0
    val animatedDiscount by animateIntAsState(if (isVisible) targetDiscount else 0, tween(800), label = "")
    val savingsColor by animateColorAsState(if (isVisible) AiSuccessGreen else Color.Gray, tween(800), label = "")

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeIn(tween(600))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().scale(cardScale).clickable(interactionSource = interactionSource, indication = null) {},
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(90.dp, 110.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE3F2FD)).padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item.imageUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().scale(imageScale)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, color = AiTextMain)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.specs, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "₹${item.originalPrice}", style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough), color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "₹${item.price}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AiPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$animatedDiscount% Off", color = savingsColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // --- NEW: WARRANTY BADGE IN CART ---
                        if (item.hasExtendedWarranty) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, "Warranty", tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Extended 2-Year Plan (₹${item.warrantyPrice})", color = Color(0xFF00C853), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Delivery by $deliveryDate | Free", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF424242), modifier = Modifier.alpha(deliveryAlpha))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = Color(0xFFF0F0F0))

                Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    val removeInteraction = remember { MutableInteractionSource() }
                    val isRemovePressed by removeInteraction.collectIsPressedAsState()
                    val removeBg by animateColorAsState(if (isRemovePressed) Color(0xFFFFEBEE) else Color.Transparent, label = "")

                    val saveInteraction = remember { MutableInteractionSource() }
                    val isSavePressed by saveInteraction.collectIsPressedAsState()
                    val saveBg by animateColorAsState(if (isSavePressed) Color(0xFFE3F2FD) else Color.Transparent, label = "")

                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(removeBg).clickable(interactionSource = removeInteraction, indication = null) { CartManager.items.remove(item) },
                        contentAlignment = Alignment.Center
                    ) { Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray) }

                    VerticalDivider(color = Color(0xFFF0F0F0))

                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(saveBg).clickable(interactionSource = saveInteraction, indication = null) { },
                        contentAlignment = Alignment.Center
                    ) { Text("Save for later", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AiPrimary) }
                }
            }
        }
    }
}

@Composable
fun PriceDetailsView(count: Int, totalMrp: Int, savings: Int, warrantyCost: Int, finalAmount: Int) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(visible = isVisible, enter = scaleIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeIn(tween(600))) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AiGlassWhite),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("PRICE DETAILS", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))

                PriceRow("Price ($count items)", "₹$totalMrp")
                PriceRow("Discount", "-₹$savings", AiSuccessGreen)
                if (warrantyCost > 0) {
                    PriceRow("Extended Warranty", "+₹$warrantyCost", AiPrimary)
                }
                PriceRow("Delivery Charges", "FREE", AiSuccessGreen)

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFE0E0E0))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Amount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AiTextMain)
                    Text("₹$finalAmount", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AiPrimary)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFE0E0E0))

                Text("You will save ₹$savings on this order", color = AiSuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PriceRow(label: String, value: String, color: Color = AiTextMain) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FlipkartBottomBar(total: Int, isOrdering: Boolean, onPlaceOrder: () -> Unit) {
    val animatedTotal by animateIntAsState(targetValue = total, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "")
    val btnInteraction = remember { MutableInteractionSource() }
    val isBtnPressed by btnInteraction.collectIsPressedAsState()
    val btnScale by animateFloatAsState(if (isBtnPressed) 0.96f else 1f, tween(400, easing = FastOutSlowInEasing), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("₹$animatedTotal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AiTextMain)
                Text("View Price Details", fontSize = 12.sp, color = AiAccent, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = onPlaceOrder, enabled = !isOrdering, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.width(160.dp).height(50.dp).scale(btnScale), interactionSource = btnInteraction
            ) {
                Box(modifier = Modifier.fillMaxSize().background(AiGradient, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    if (isOrdering) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Text("Place Order", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyCartView(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(120.dp).background(Color.White, CircleShape).padding(24.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.fillMaxSize(), tint = AiAccent.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your Smart Cart is empty", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AiTextMain)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Discover AI recommended devices.", color = Color.Gray, fontSize = 14.sp)
    }
}