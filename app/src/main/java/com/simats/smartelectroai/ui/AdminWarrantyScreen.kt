package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.AdminWarrantyItem
import com.simats.smartelectroai.api.AdminWarrantyResponse
import com.simats.smartelectroai.api.ApproveWarrantyRequest
import com.simats.smartelectroai.api.AuthResponse
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.ApiConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.delay

private val AdminBlue = Color(0xFF1976D2)
private val TextDark = Color(0xFF1E1E1E)
private val TextGray = Color(0xFF757575)
private val BgGray = Color(0xFFF8F9FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWarrantyScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Items") }
    val filters = listOf("Expiring Soon", "All Items", "Active", "Expired")

    var warranties by remember { mutableStateOf<List<AdminWarrantyItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 🚀 NEW STATE: Controls the visibility of the details dialog
    var selectedWarranty by remember { mutableStateOf<AdminWarrantyItem?>(null) }

    val visibleState = remember { MutableTransitionState(false) }

    fun loadWarranties() {
        RetrofitClient.instance.getAdminWarranties("Bearer $token").enqueue(object : Callback<AdminWarrantyResponse> {
            override fun onResponse(call: Call<AdminWarrantyResponse>, response: Response<AdminWarrantyResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    warranties = response.body()?.warranties ?: emptyList()
                    visibleState.targetState = true
                } else {
                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<AdminWarrantyResponse>, t: Throwable) {
                isLoading = false
                visibleState.targetState = true
                Toast.makeText(context, "Network Error: Please check connection", Toast.LENGTH_SHORT).show()
            }
        })
    }

    LaunchedEffect(Unit) { loadWarranties() }

    val pendingClaims = warranties.filter { it.status.equals("Pending", ignoreCase = true) }

    val listItems = warranties.filter { item ->
        val stat = item.status.lowercase()
        val filter = selectedFilter.lowercase()

        stat != "pending" && (
                filter == "all items" ||
                        (filter == "expiring soon" && stat.contains("alert")) ||
                        (filter == "active" && (stat == "secure" || stat == "active")) ||
                        (filter == "expired" && stat == "expired")
                )
    }

    // 🚀 SHOW DIALOG IF A WARRANTY IS SELECTED
    if (selectedWarranty != null) {
        WarrantyDetailDialog(
            item = selectedWarranty!!,
            onDismiss = { selectedWarranty = null }
        )
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visibleState = visibleState, enter = slideInVertically(tween(500)) { -it } + fadeIn(tween(500))) {
                TopAppBar(
                    title = { Text("Warranty Management", fontWeight = FontWeight.Bold, color = TextDark) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextDark) } },
                    actions = { IconButton(onClick = { loadWarranties() }) { Icon(Icons.Default.MoreVert, "Refresh", tint = AdminBlue) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        bottomBar = { AdminBottomNavBar("AdminWarranty", onNavigate) },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AdminBlue) }
        } else {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { it / 10 }, animationSpec = spring(stiffness = Spring.StiffnessLow))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        var isFocused by remember { mutableStateOf(false) }
                        val borderColor by animateColorAsState(targetValue = if (isFocused) AdminBlue else Color.Transparent, animationSpec = tween(300), label = "search_border")

                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search by user or device...", color = TextGray, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray) }, modifier = Modifier.fillMaxWidth().height(50.dp).onFocusChanged { isFocused = it.isFocused },
                            shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = BgGray, focusedContainerColor = BgGray, unfocusedBorderColor = borderColor, focusedBorderColor = borderColor)
                        )
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filters) { filter ->
                                AnimatedFilterChip(text = filter, isSelected = filter == selectedFilter, onClick = { selectedFilter = filter })
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().animateContentSize()) {
                            Column {
                                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("USER /\nDEVICE", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.35f))
                                    Text("EXPIRY\nDATE", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.25f))
                                    Text("STATUS", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.40f))
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE))

                                if (listItems.isEmpty()) Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No warranties found.", color = Color.Gray, fontSize = 14.sp) }

                                listItems.forEachIndexed { index, item ->
                                    AnimatedWarrantyRow(
                                        item = item,
                                        index = index,
                                        visibleState = visibleState,
                                        onViewClick = { selectedWarranty = item } // 🚀 TRIGGERS THE DIALOG
                                    )
                                }
                            }
                        }
                    }

                    item { AnimatedReminderButton(context = context) }

                    if (pendingClaims.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("PENDING CLAIMS", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 13.sp, letterSpacing = 1.sp)
                                Box(modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("${pendingClaims.size} NEW", color = AdminBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }

                        itemsIndexed(pendingClaims) { index, claim ->
                            AnimatedPendingClaimCard(claim = claim, index = index, visibleState = visibleState, token = token, onActionComplete = { loadWarranties() })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ==========================================
// 🚀 NEW: WARRANTY DETAIL POPUP DIALOG
// ==========================================
@Composable
fun WarrantyDetailDialog(item: AdminWarrantyItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {

                // Title
                Text("Warranty Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AdminBlue)
                Spacer(Modifier.height(20.dp))

                // Product Image and Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).background(BgGray, RoundedCornerShape(12.dp))) {
                        AsyncImage(
                            model = item.product_image_url?.let { if (it.startsWith("http")) it else "${ApiConfig.BASE_URL.trimEnd('/')}$it" },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(item.device_name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(item.device_type, fontSize = 12.sp, color = TextGray)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color(0xFFEEEEEE))

                // User Details Section
                Text("USER INFORMATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                DialogDetailRow(icon = Icons.Outlined.Person, label = "Name", value = item.user_name)
                Spacer(modifier = Modifier.height(12.dp))
                DialogDetailRow(icon = Icons.Outlined.Email, label = "Email", value = item.user_email ?: "Data unavailable")
                Spacer(modifier = Modifier.height(12.dp))
                DialogDetailRow(icon = Icons.Outlined.Phone, label = "Phone", value = item.user_phone ?: "Data unavailable")

                HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color(0xFFEEEEEE))

                // Warranty Dates Section
                Text("WARRANTY TIMELINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                DialogDetailRow(icon = Icons.Outlined.CalendarToday, label = "Purchased", value = item.purchase_date ?: "Data unavailable")
                Spacer(modifier = Modifier.height(12.dp))
                DialogDetailRow(icon = Icons.Outlined.EventAvailable, label = "Expires", value = item.expiry_date)

                Spacer(Modifier.height(28.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
                ) {
                    Text("Close Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// Helper Row for the Dialog
@Composable
fun DialogDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = TextGray, modifier = Modifier.width(70.dp))
        Text(value, fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ==========================================
// REST OF THE EXISTING UI COMPONENTS
// ==========================================
@Composable
fun AnimatedFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rawScale = if (isPressed) 0.95f else 1f
    val scale by animateFloatAsState(targetValue = rawScale, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chip_scale")

    val bgColor by animateColorAsState(targetValue = if (isSelected) AdminBlue else BgGray, animationSpec = tween(300), label = "chip_bg")
    val textColor by animateColorAsState(targetValue = if (isSelected) Color.White else TextGray, animationSpec = tween(300), label = "chip_text")

    Box(modifier = Modifier.scale(if (scale.isNaN()) 1f else scale).clip(RoundedCornerShape(20.dp)).background(bgColor).clickable(interactionSource = interactionSource, indication = null, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (isSelected && text == "Expiring Soon") { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
fun AnimatedWarrantyRow(item: AdminWarrantyItem, index: Int, visibleState: MutableTransitionState<Boolean>, onViewClick: () -> Unit) {
    AnimatedVisibility(visibleState = visibleState, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(400, delayMillis = index * 50)) + fadeIn(tween(delayMillis = index * 50))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(0.35f)) {
                Text(item.user_name ?: "User", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.device_name ?: "Device", fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.expiry_date ?: "N/A", fontSize = 12.sp, color = TextGray, modifier = Modifier.weight(0.25f))

            Row(Modifier.weight(0.40f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AnimatedStatusBadge(item.status ?: "Secure")
                val viewInteractionSource = remember { MutableInteractionSource() }
                val isViewPressed by viewInteractionSource.collectIsPressedAsState()

                val rawScale = if (isViewPressed) 0.90f else 1f
                val viewScale by animateFloatAsState(targetValue = rawScale, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "view_scale")

                Text(text = "View", color = AdminBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.scale(if (viewScale.isNaN()) 1f else viewScale).clickable(interactionSource = viewInteractionSource, indication = null) { onViewClick() })
            }
        }
    }
}

@Composable
fun AnimatedStatusBadge(status: String) {
    val statLower = status.lowercase()
    val (statusText, targetColor, targetBg) = when {
        statLower == "secure" || statLower == "active" -> Triple("ACTIVE", Color(0xFF2E7D32), Color(0xFFE8F5E9))
        statLower.contains("alert") -> Triple(status.uppercase(), Color(0xFFC62828), Color(0xFFFFEBEE))
        else -> Triple("EXPIRED", Color(0xFFC62828), Color(0xFFFFEBEE))
    }
    val bgColor by animateColorAsState(targetValue = targetBg, animationSpec = tween(400), label = "badge_bg")
    val textColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(400), label = "badge_text")

    Box(modifier = Modifier.background(bgColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(statusText, color = textColor, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun AnimatedReminderButton(context: Context) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rawScale = if (isPressed) 0.95f else 1f
    val pressScale by animateFloatAsState(targetValue = rawScale, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btn_press")

    Button(
        onClick = { Toast.makeText(context, "Reminders Sent!", Toast.LENGTH_SHORT).show() },
        interactionSource = interactionSource, modifier = Modifier.fillMaxWidth().height(48.dp).scale(if (pressScale.isNaN()) 1f else pressScale),
        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AdminBlue)
    ) {
        Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Send Reminder for Expiring", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedPendingClaimCard(claim: AdminWarrantyItem, index: Int, visibleState: MutableTransitionState<Boolean>, token: String, onActionComplete: () -> Unit) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf<String?>(null) }

    if (showImageDialog != null) {
        AlertDialog(
            onDismissRequest = { showImageDialog = null },
            confirmButton = { TextButton(onClick = { showImageDialog = null }) { Text("Close", color = AdminBlue) } },
            title = { Text("Attachment Preview") },
            text = {
                val safeBaseUrl = ApiConfig.BASE_URL.trimEnd('/')

                AsyncImage(
                    model = "$safeBaseUrl${showImageDialog}",
                    contentDescription = "Attachment",
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
        )
    }

    fun processClaim(action: String) {
        isProcessing = true
        val req = ApproveWarrantyRequest(action)
        RetrofitClient.instance.approveAdminWarranty("Bearer $token", claim.id, req).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                isProcessing = false
                if (response.isSuccessful) {
                    Toast.makeText(context, response.body()?.message ?: "Action completed", Toast.LENGTH_SHORT).show()
                    onActionComplete()
                } else {
                    Toast.makeText(context, "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                isProcessing = false
                Toast.makeText(context, "Network error. Check connection.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    AnimatedVisibility(visibleState = visibleState, enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow), expandFrom = Alignment.Top) + fadeIn(tween(delayMillis = 100 + (index * 100)))) {
        Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CLAIM #CLM-0${claim.id}", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Just now", color = TextGray, fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(claim.device_name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Spacer(Modifier.height(8.dp))
                Text(claim.claim_reason ?: "No reason provided", fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)

                Spacer(Modifier.height(16.dp))
                Text("ATTACHMENTS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminAttachmentThumbnail(modifier = Modifier.weight(1f), icon = Icons.Default.Description, label = "View Invoice", onClick = {
                        if (claim.claim_invoice_url != null) showImageDialog = claim.claim_invoice_url
                        else Toast.makeText(context, "No Invoice Uploaded", Toast.LENGTH_SHORT).show()
                    })
                    AdminAttachmentThumbnail(modifier = Modifier.weight(1f), icon = Icons.Default.Image, label = "View Device", onClick = {
                        if (claim.claim_device_url != null) showImageDialog = claim.claim_device_url
                        else Toast.makeText(context, "No Device Image Uploaded", Toast.LENGTH_SHORT).show()
                    })
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { processClaim("Reject") }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFFCDD2)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)), enabled = !isProcessing) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { processClaim("Approve") }, modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = AdminBlue), enabled = !isProcessing) {
                        AnimatedContent(targetState = isProcessing, label = "button_loading") { loading ->
                            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Approve", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAttachmentThumbnail(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val rawScale = if (isPressed) 0.95f else 1f
    val scale = animateFloatAsState(targetValue = rawScale, label = "scale").value.takeIf { !it.isNaN() } ?: 1f

    Box(
        modifier = modifier.scale(if (scale.isNaN()) 1f else scale).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AdminBlue, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}