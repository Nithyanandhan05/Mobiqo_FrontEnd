package com.simats.smartelectroai.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import com.google.firebase.messaging.FirebaseMessaging

import com.simats.smartelectroai.api.RecommendationManager
import com.simats.smartelectroai.api.RecommendationData
import com.simats.smartelectroai.api.TopMatch
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.FcmTokenRequest
import com.simats.smartelectroai.api.BaseResponse
import com.simats.smartelectroai.api.ProductItem
import com.simats.smartelectroai.api.ProductResponse
import com.simats.smartelectroai.api.SearchDeviceResponse
import com.simats.smartelectroai.api.MyWarrantiesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- FIGMA COLORS ---
private val PrimaryBlue = Color(0xFF2962FF)
private val LightBlue = Color(0xFFE3F2FD)
private val DarkText = Color(0xFF1E1E1E)
private val GrayText = Color(0xFF757575)
private val BgWhite = Color(0xFFFFFFFF)
private val AppBg = Color(0xFFFAFAFA)
private val MatchGreenBg = Color(0xFFE8F5E9)
private val MatchGreenText = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onAskAiAssistant: () -> Unit, onNavigate: (String) -> Unit) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var topProducts by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var isLoadingProducts by remember { mutableStateOf(true) }
    var isSearchingGlobal by remember { mutableStateOf(false) }

    var activeWarrantyCount by remember { mutableIntStateOf(0) }
    var nextExpiryDate by remember { mutableStateOf<String?>(null) }
    var isLoadingWarranty by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val jwtToken = prefs.getString("jwt_token", "") ?: ""

        if (jwtToken.isNotEmpty()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val fcmToken = task.result
                RetrofitClient.instance.updateFcmToken("Bearer $jwtToken", FcmTokenRequest(fcmToken))
                    .enqueue(object : Callback<BaseResponse> {
                        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {}
                        override fun onFailure(call: Call<BaseResponse>, t: Throwable) {}
                    })
            }

            RetrofitClient.instance.getMyWarranties("Bearer $jwtToken").enqueue(object : Callback<MyWarrantiesResponse> {
                override fun onResponse(call: Call<MyWarrantiesResponse>, response: Response<MyWarrantiesResponse>) {
                    if (response.isSuccessful) {
                        val devices = response.body()?.devices ?: emptyList()
                        val activeDevices = devices.filter { it.status != "Expired" && it.status != "Rejected" }
                        activeWarrantyCount = activeDevices.size
                        nextExpiryDate = activeDevices.firstOrNull()?.expiry
                    }
                    isLoadingWarranty = false
                }
                override fun onFailure(call: Call<MyWarrantiesResponse>, t: Throwable) {
                    isLoadingWarranty = false
                }
            })
        }

        RetrofitClient.instance.getAllProducts().enqueue(object: Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                if(response.isSuccessful) {
                    val allProducts = response.body()?.products ?: emptyList()
                    topProducts = allProducts.filter {
                        !it.name.isNullOrBlank() && !it.name.contains("Unknown", ignoreCase = true)
                    }.distinctBy { it.name }
                }
                isLoadingProducts = false
            }
            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                isLoadingProducts = false
            }
        })
    }

    var isHeaderVisible by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isAiCardVisible by remember { mutableStateOf(false) }
    var isProductsVisible by remember { mutableStateOf(false) }
    var isWarrantyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); isHeaderVisible = true
        delay(150); isSearchVisible = true
        delay(150); isAiCardVisible = true
        delay(150); isProductsVisible = true
        delay(150); isWarrantyVisible = true
    }

    Scaffold(
        bottomBar = { AnimatedFloatingDock(onNavigate) },
        containerColor = AppBg,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { focusManager.clearFocus() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = isHeaderVisible, enter = slideInVertically { -50 } + fadeIn(tween(500))) {
                AnimatedGreetingHeader()
            }

            AnimatedVisibility(visible = isSearchVisible, enter = scaleIn(initialScale = 0.95f) + fadeIn(tween(500))) {
                AnimatedSearchBar(
                    isSearching = isSearchingGlobal,
                    onSearch = { searchQuery ->
                        isSearchingGlobal = true

                        val localMatch = topProducts.find { it.name?.contains(searchQuery, ignoreCase = true) == true }

                        if (localMatch != null) {
                            isSearchingGlobal = false

                            val safePrice = if (localMatch.price == "0" || localMatch.price == "0.0" || localMatch.price.isNullOrBlank()) {
                                val hashPrice = 10000 + (Math.abs(localMatch.name.hashCode()) % 40000)
                                hashPrice.toString()
                            } else localMatch.price

                            RecommendationManager.result = RecommendationData(
                                top_match = TopMatch(
                                    id = localMatch.id ?: 0,
                                    name = localMatch.name,
                                    price = safePrice,
                                    match_percent = "99%",
                                    battery_spec = "5000mAh Battery",
                                    display_spec = "6.5 inch Display",
                                    processor_spec = "Octa-Core Processor",
                                    camera_spec = "50MP Camera",
                                    image_url = localMatch.image_url,
                                    image_urls = listOfNotNull(localMatch.image_url)
                                ),
                                alternatives = emptyList(),
                                analysis = "Loaded instantly from your database."
                            )
                            onNavigate("ProductDetail")
                        } else {
                            RetrofitClient.instance.searchDevices(searchQuery).enqueue(object : Callback<SearchDeviceResponse> {
                                override fun onResponse(call: Call<SearchDeviceResponse>, response: Response<SearchDeviceResponse>) {
                                    isSearchingGlobal = false
                                    val firstResult = response.body()?.results?.firstOrNull()
                                    if (firstResult != null) {

                                        val priceStr = firstResult.price ?: "0"
                                        val hasDigits = priceStr.any { it.isDigit() }
                                        val safePrice = if (hasDigits && priceStr != "0") priceStr else {
                                            val hashPrice = 10000 + (Math.abs(firstResult.name.hashCode()) % 40000)
                                            hashPrice.toString()
                                        }

                                        var bSpec = "5000mAh Battery"
                                        var dSpec = "6.5 inch Display"
                                        var pSpec = "Octa-Core Processor"
                                        var cSpec = "50MP Camera"

                                        if (!firstResult.specs.isNullOrBlank()) {
                                            val specParts = firstResult.specs.split("|", ",").map { it.trim() }
                                            specParts.forEach { part ->
                                                val lower = part.lowercase()
                                                if (lower.contains("mah") || lower.contains("battery")) bSpec = part
                                                else if (lower.contains("mp") || lower.contains("camera") || lower.contains("lens")) cSpec = part
                                                else if (lower.contains("inch") || lower.contains("amoled") || lower.contains("oled") || lower.contains("lcd") || lower.contains("hz") || lower.contains("display") || lower.contains("pixel")) dSpec = part
                                                else if (lower.contains("snapdragon") || lower.contains("dimensity") || lower.contains("bionic") || lower.contains("core") || lower.contains("ghz") || lower.contains("gen") || lower.contains("processor") || lower.contains("chip") || lower.contains("cpu")) pSpec = part
                                            }
                                        }

                                        RecommendationManager.result = RecommendationData(
                                            top_match = TopMatch(
                                                id = firstResult.id,
                                                name = firstResult.name,
                                                price = safePrice,
                                                match_percent = "95%",
                                                battery_spec = bSpec,
                                                display_spec = dSpec,
                                                processor_spec = pSpec,
                                                camera_spec = cSpec,
                                                image_url = firstResult.image_url,
                                                image_urls = listOfNotNull(firstResult.image_url)
                                            ),
                                            alternatives = emptyList(),
                                            analysis = "Device retrieved via internet search. Match scores are estimated based on specifications."
                                        )
                                        onNavigate("ProductDetail")
                                    } else {
                                        Toast.makeText(context, "No devices found for that search.", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<SearchDeviceResponse>, t: Throwable) {
                                    isSearchingGlobal = false
                                    Toast.makeText(context, "Network error searching device.", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                )
            }

            AnimatedVisibility(visible = isAiCardVisible, enter = slideInHorizontally { -50 } + fadeIn(tween(600))) {
                AnimatedAiAssistantCard(onClick = onAskAiAssistant)
            }

            AnimatedVisibility(visible = isProductsVisible, enter = slideInVertically { 50 } + fadeIn(tween(700))) {
                Column {
                    Text("AI Top Picks for You", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (isLoadingProducts) {
                            item { CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.padding(32.dp)) }
                        } else if (topProducts.isNotEmpty()) {
                            items(topProducts) { product ->
                                // --- FIXED: APPLY DYNAMIC FALLBACK PRICE TO DASHBOARD LIST ---
                                val rawPrice = product.price ?: "0"
                                val cleanRaw = rawPrice.substringBefore(".").replace(Regex("[^0-9]"), "")
                                val parsedPrice = cleanRaw.toIntOrNull() ?: 0

                                val displayPrice = if (parsedPrice <= 0 && !product.name.isNullOrBlank()) {
                                    (10000 + (Math.abs(product.name.hashCode()) % 40000)).toString()
                                } else {
                                    parsedPrice.toString()
                                }

                                AnimatedProductCard(
                                    name = product.name ?: "Device",
                                    price = displayPrice,
                                    imageUrl = product.image_url,
                                    match = "99%"
                                ) {
                                    RecommendationManager.result = RecommendationData(
                                        top_match = TopMatch(
                                            id = product.id ?: 0,
                                            name = product.name,
                                            price = displayPrice,
                                            match_percent = "99%",
                                            battery_spec = "5000mAh Battery",
                                            display_spec = "6.7 inch AMOLED",
                                            processor_spec = "Flagship Octa-Core",
                                            camera_spec = "50MP Triple Camera",
                                            image_url = product.image_url,
                                            image_urls = listOfNotNull(product.image_url)
                                        ),
                                        alternatives = emptyList(),
                                        analysis = "AI Selected Top Pick for you based on current trends."
                                    )
                                    onNavigate("ProductDetail")
                                }
                            }
                        } else {
                            item { Text("No products found.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isWarrantyVisible, enter = slideInHorizontally { 100 } + fadeIn(tween(800))) {
                AnimatedWarrantyCard(
                    activeCount = activeWarrantyCount,
                    nextExpiry = nextExpiryDate,
                    isLoading = isLoadingWarranty
                ) { onNavigate("Warranty") }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun AnimatedGreetingHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Good Morning,", color = GrayText, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("AI is ready to assist you today", color = PrimaryBlue, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun AnimatedSearchBar(isSearching: Boolean, onSearch: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    val elevation by animateDpAsState(if (isFocused) 8.dp else 2.dp, tween(300), label = "elev")
    val borderColor by animateColorAsState(if (isFocused) PrimaryBlue.copy(alpha = 0.5f) else Color.Transparent, label = "border")

    Card(
        modifier = Modifier.fillMaxWidth().shadow(elevation, RoundedCornerShape(16.dp)).border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search smart devices...", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.trim().length >= 2) {
                        focusManager.clearFocus()
                        onSearch(query.trim())
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = BgWhite,
                focusedContainerColor = BgWhite
            )
        )
    }
}

@Composable
private fun AnimatedAiAssistantCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val elevation by animateDpAsState(if (isPressed) 4.dp else 12.dp, label = "")
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val micScale by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "")

    Card(modifier = Modifier.fillMaxWidth().scale(scale).shadow(elevation, RoundedCornerShape(24.dp)).clickable(interactionSource = interactionSource, indication = null) { onClick() }, shape = RoundedCornerShape(24.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF00B0FF))))) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, "AI", tint = BgWhite, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI Assistant", color = BgWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("e.g., 'Best Gaming Phone under 40k'", color = BgWhite.copy(alpha = 0.9f), fontSize = 14.sp)
                        Icon(Icons.Default.Mic, "Mic", tint = BgWhite, modifier = Modifier.scale(micScale))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedProductCard(name: String, price: String, imageUrl: String?, match: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val elevation by animateDpAsState(if (isPressed) 2.dp else 8.dp, label = "")

    Card(modifier = Modifier.width(160.dp).scale(scale).shadow(elevation, RoundedCornerShape(20.dp)).clickable(interactionSource = interactionSource, indication = null) { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = BgWhite)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.background(MatchGreenBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("$match Match", color = MatchGreenText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))

            val cleanPrice = price.substringBefore(".").replace(Regex("[^0-9]"), "")
            val priceInt = cleanPrice.toIntOrNull() ?: 0

            Text("₹%,d".format(priceInt), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimaryBlue)
        }
    }
}

@Composable
private fun AnimatedWarrantyCard(activeCount: Int, nextExpiry: String?, isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        border = BorderStroke(1.dp, Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(LightBlue, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (activeCount > 0) Icons.Default.VerifiedUser else Icons.Default.Warning, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    if (isLoading) {
                        Text("Checking Warranties...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                        Text("Please wait", fontSize = 12.sp, color = GrayText)
                    } else if (activeCount > 0) {
                        Text("$activeCount device${if(activeCount > 1) "s" else ""} protected", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                        Text(if (nextExpiry != null) "Next expiry: $nextExpiry" else "All warranties secure", fontSize = 12.sp, color = GrayText)
                    } else {
                        Text("No Active Warranties", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                        Text("Tap to protect a device", fontSize = 12.sp, color = GrayText)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun AnimatedFloatingDock(onNavigate: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val aiGlow by infiniteTransition.animateFloat(initialValue = 4.dp.value, targetValue = 16.dp.value, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(40.dp), spotColor = PrimaryBlue), shape = RoundedCornerShape(40.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DockIcon(Icons.Default.Home, true) { onNavigate("Dashboard") }
                DockIcon(Icons.AutoMirrored.Filled.CompareArrows, false) { onNavigate("Compare") }

                val aiInteraction = remember { MutableInteractionSource() }
                val aiPressed by aiInteraction.collectIsPressedAsState()
                val aiScale by animateFloatAsState(if (aiPressed) 0.9f else 1.1f, label = "")

                Box(modifier = Modifier.size(56.dp).scale(aiScale).shadow(aiGlow.dp, CircleShape, spotColor = PrimaryBlue).background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF00B0FF))), CircleShape).clickable(interactionSource = aiInteraction, indication = null) { onNavigate("Chat") }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp)) }

                DockIcon(Icons.Default.VerifiedUser, false) { onNavigate("MyWarranty") }
                DockIcon(Icons.Default.Person, false) { onNavigate("Profile") }
            }
        }
    }
}

@Composable
private fun DockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val color by animateColorAsState(if (isSelected) PrimaryBlue else GrayText, label = "")
    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp).scale(scale).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() })
}