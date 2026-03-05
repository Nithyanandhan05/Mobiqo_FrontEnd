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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.AdminDetailedOrder
import com.simats.smartelectroai.api.AdminOrdersResponse
import com.simats.smartelectroai.api.AuthResponse
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.UpdateOrderRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- ENTERPRISE SAAS COLOR PALETTE ---
private val AdminBlue = Color(0xFF2962FF)
private val AdminBgGray = Color(0xFFF8F9FA)
private val AdminTextDark = Color(0xFF1D1D21)
private val AdminTextSub = Color(0xFF6E6E73)
private val AdminBorder = Color(0xFFE5E7EB)

// Status Colors
private val StatusGreenBg = Color(0xFFE8F5E9)
private val StatusGreenText = Color(0xFF2E7D32)
private val StatusOrangeBg = Color(0xFFFFF3E0)
private val StatusOrangeText = Color(0xFFEF6C00)
private val StatusRedBg = Color(0xFFFFEBEE)
private val StatusRedText = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderManagementScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Paid & Processing", "Shipped", "Delivered", "Cancelled")

    var orders by remember { mutableStateOf<List<AdminDetailedOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedOrderId by remember { mutableStateOf<Int?>(null) }

    val visibleState = remember { MutableTransitionState(false) }

    fun loadOrders() {
        RetrofitClient.instance.getAdminOrders("Bearer $token").enqueue(object : Callback<AdminOrdersResponse> {
            override fun onResponse(call: Call<AdminOrdersResponse>, response: Response<AdminOrdersResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    orders = response.body()?.orders ?: emptyList()
                    visibleState.targetState = true
                }
            }
            override fun onFailure(call: Call<AdminOrdersResponse>, t: Throwable) {
                isLoading = false
                visibleState.targetState = true
            }
        })
    }

    LaunchedEffect(Unit) { loadOrders() }

    val filteredOrders = if (selectedFilter == "All") orders else orders.filter { it.status == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Management", fontWeight = FontWeight.Bold, color = AdminTextDark) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate("AdminDashboard") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AdminTextDark) }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Search, "Search", tint = AdminTextDark) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { AdminBottomNavBar("AdminOrderManagement", onNavigate) },
        containerColor = Color.White
    ) { padding ->
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(600)) + slideInVertically(
                initialOffsetY = { it / 8 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                HorizontalDivider(color = AdminBorder)

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = filter == selectedFilter

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "")

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AdminTextDark else AdminBgGray)
                                .clickable(interactionSource = interactionSource, indication = null) { selectedFilter = filter }
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(filter, color = if (isSelected) Color.White else AdminTextSub, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AdminBlue) }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(filteredOrders, key = { _, it -> it.id }) { index, order ->
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = slideInHorizontally(
                                    initialOffsetX = { it / 2 },
                                    animationSpec = tween(durationMillis = 400, delayMillis = index * 80)
                                ) + fadeIn(tween(delayMillis = index * 80))
                            ) {
                                OrderTableRow(
                                    order = order,
                                    isExpanded = expandedOrderId == order.id,
                                    onExpandToggle = { expandedOrderId = if (expandedOrderId == order.id) null else order.id },
                                    token = token,
                                    onOrderUpdated = { loadOrders() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTableRow(
    order: AdminDetailedOrder,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    token: String,
    onOrderUpdated: () -> Unit
) {
    var trackingInput by remember(order.tracking_number) { mutableStateOf(order.tracking_number ?: "") }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var currentStatus by remember(order.status) { mutableStateOf(order.status) }
    val context = LocalContext.current

    val (statusBg, statusText) = when (currentStatus) {
        "Delivered" -> StatusGreenBg to StatusGreenText
        "Cancelled" -> StatusRedBg to StatusRedText
        else -> StatusOrangeBg to StatusOrangeText
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AdminBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 0.dp)
    ) {
        Column {
            // --- COLLAPSED VIEW WITH IMAGE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail with Fallback Safety
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AdminBgGray),
                    contentAlignment = Alignment.Center
                ) {
                    val safeImageUrl = order.image_url?.takeIf { it.isNotBlank() }
                        ?: "https://cdn-icons-png.flaticon.com/512/330/330714.png"

                    AsyncImage(
                        model = safeImageUrl,
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.customer_name, color = AdminTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val cleanId = order.invoice_no.split("-").lastOrNull() ?: order.invoice_no
                    Text("#$cleanId • ${order.price}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AdminTextSub)
                }

                // Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.background(statusBg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(currentStatus, fontSize = 10.sp, color = statusText, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(if (isExpanded) "Close" else "Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdminBlue)
                }
            }

            // --- EXPANDED DETAILS VIEW ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HorizontalDivider(color = AdminBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("PRODUCT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminTextSub)
                    Text(order.product_name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminTextDark)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("PAYMENT METHOD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminTextSub)
                            Text(order.payment_method ?: "Cash On Delivery", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AdminTextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL PAID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminTextSub)
                            Text(order.price, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AdminBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Dropdown
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Update Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminTextSub, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.background(AdminBgGray, RoundedCornerShape(6.dp)).clickable { statusDropdownExpanded = true }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentStatus, fontSize = 12.sp, color = AdminTextDark, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = AdminTextDark)
                            }
                            DropdownMenu(expanded = statusDropdownExpanded, onDismissRequest = { statusDropdownExpanded = false }, modifier = Modifier.background(Color.White)) {
                                listOf("Paid & Processing", "Shipped", "Out for Delivery", "Delivered", "Cancelled").forEach { statusOpt ->
                                    DropdownMenuItem(text = { Text(statusOpt, fontSize = 12.sp, fontWeight = FontWeight.Medium) }, onClick = { currentStatus = statusOpt; statusDropdownExpanded = false })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SHIPPING ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminTextSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(order.address, fontSize = 13.sp, color = AdminTextDark, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("TRACKING NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminTextSub)
                    Spacer(modifier = Modifier.height(8.dp))

                    var isFocused by remember { mutableStateOf(false) }
                    val borderColor by animateColorAsState(if (isFocused) AdminBlue else AdminBorder, label = "")

                    OutlinedTextField(
                        value = trackingInput,
                        onValueChange = { trackingInput = it },
                        placeholder = { Text("Enter carrier tracking ID", color = Color.LightGray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).onFocusChanged { isFocused = it.isFocused },
                        singleLine = true, shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(unfocusedContainerColor = AdminBgGray, focusedContainerColor = Color.White, unfocusedIndicatorColor = borderColor, focusedIndicatorColor = borderColor)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val req = UpdateOrderRequest(currentStatus, trackingInput)
                                RetrofitClient.instance.updateAdminOrder("Bearer $token", order.id, req).enqueue(object : Callback<AuthResponse> {
                                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                        Toast.makeText(context, "Details Saved!", Toast.LENGTH_SHORT).show()
                                        onExpandToggle()
                                        onOrderUpdated()
                                    }
                                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdminTextDark)
                        ) {
                            Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}