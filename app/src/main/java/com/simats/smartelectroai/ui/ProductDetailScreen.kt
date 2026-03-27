package com.simats.smartelectroai.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// --- IMPORTS ---
import com.simats.smartelectroai.api.RecommendationManager
import com.simats.smartelectroai.api.CartManager
import com.simats.smartelectroai.api.CartItemModel
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.ProductSpecsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ==========================================
// 🌟 DYNAMIC SPEC SCORING ENGINE 🌟
// ==========================================
object SpecScorer {
    fun getBatteryScore(spec: String): Float {
        val s = spec.lowercase()
        val mah = Regex("(\\d{3,4})").find(s)?.value?.toIntOrNull() ?: 0
        return when {
            mah >= 6000 -> 0.99f
            mah >= 5000 -> 0.95f
            mah >= 4500 -> 0.88f
            mah >= 4000 -> 0.82f
            else -> 0.75f + (kotlin.math.abs(s.hashCode() % 100) / 1000f)
        }
    }

    fun getCameraScore(spec: String): Float {
        val s = spec.lowercase()
        val mp = Regex("(\\d{2,3})\\s*mp").find(s)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("(\\d{2,3})").find(s)?.value?.toIntOrNull() ?: 0
        return when {
            mp >= 200 -> 0.99f
            mp >= 100 -> 0.96f
            mp >= 64 -> 0.92f
            mp >= 48 -> 0.88f
            s.contains("quad") || s.contains("triple") -> 0.86f
            else -> 0.78f + (kotlin.math.abs(s.hashCode() % 100) / 1000f)
        }
    }

    fun getPerformanceScore(spec: String): Float {
        val s = spec.lowercase()
        return when {
            s.contains("gen 3") || s.contains("gen 2") || s.contains("a17") || s.contains("a16") || s.contains("bionic") -> 0.98f
            s.contains("gen 1") || s.contains("dimensity 9") || s.contains("a15") -> 0.94f
            s.contains("snapdragon 7") || s.contains("dimensity 8") || s.contains("tensor") -> 0.88f
            s.contains("octa") -> 0.84f
            else -> 0.75f + (kotlin.math.abs(s.hashCode() % 150) / 1000f)
        }
    }

    fun getDisplayScore(spec: String): Float {
        val s = spec.lowercase()
        var base = 0.80f
        if (s.contains("amoled") || s.contains("oled") || s.contains("ltpo")) base += 0.08f
        if (s.contains("144hz") || s.contains("120hz")) base += 0.06f
        if (s.contains("retina") || s.contains("qhd") || s.contains("4k")) base += 0.04f
        return base.coerceAtMost(0.99f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit = {},
    onAddToCart: () -> Unit = {},
    onBuyNow: () -> Unit = {},
) {
    val context = LocalContext.current
    val topMatch = RecommendationManager.result?.top_match

    // Extract Product Info
    val productId = topMatch?.id ?: -1
    val phoneName = topMatch?.name ?: "Unknown Device"
    val phonePrice = topMatch?.price ?: "30000"
    val imageList = topMatch?.image_urls ?: listOf(topMatch?.image_url ?: "")

    // --- STATE VARIABLES FOR SPECS ---
    var batterySpec by remember { mutableStateOf(topMatch?.battery_spec ?: "Standard Battery") }
    var displaySpec by remember { mutableStateOf(topMatch?.display_spec ?: "Standard Display") }
    var processorSpec by remember { mutableStateOf(topMatch?.processor_spec ?: "High Performance Processor") }
    var cameraSpec by remember { mutableStateOf(topMatch?.camera_spec ?: "Advanced Camera System") }

    // --- FETCH EXACT SPECS DIRECTLY FROM DATABASE ---
    LaunchedEffect(phoneName) {
        if (phoneName != "Unknown Device") {
            RetrofitClient.instance.getProductSpecs(phoneName).enqueue(object : Callback<ProductSpecsResponse> {
                override fun onResponse(call: Call<ProductSpecsResponse>, response: Response<ProductSpecsResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.status == "success") {
                            batterySpec = result.battery_spec ?: batterySpec
                            displaySpec = result.display_spec ?: displaySpec
                            processorSpec = result.processor_spec ?: processorSpec
                            cameraSpec = result.camera_spec ?: cameraSpec
                        }
                    }
                }
                override fun onFailure(call: Call<ProductSpecsResponse>, t: Throwable) {
                    // Fail silently, keeps initial fallback states
                }
            })
        }
    }

    // --- DYNAMIC AI SCORE CALCULATION ---
    val perfScore = remember(processorSpec) { SpecScorer.getPerformanceScore(processorSpec) }
    val camScore = remember(cameraSpec) { SpecScorer.getCameraScore(cameraSpec) }
    val batScore = remember(batterySpec) { SpecScorer.getBatteryScore(batterySpec) }
    val dispScore = remember(displaySpec) { SpecScorer.getDisplayScore(displaySpec) }
    val gamingScore = remember(perfScore, batScore) { ((perfScore + batScore) / 2f + 0.03f).coerceAtMost(0.99f) }

    // Calculate the overall top match average based on real specs
    val overallMatchFloat = remember(perfScore, camScore, batScore, dispScore, gamingScore) {
        ((perfScore + camScore + batScore + dispScore + gamingScore) / 5f).coerceAtMost(0.99f)
    }

    var isWarrantyAdded by remember { mutableStateOf(false) }
    val warrantyCost = 2999

    val cleanPrice = phonePrice.replace(Regex("[^0-9]"), "")
    val priceInt = cleanPrice.toIntOrNull() ?: 30000

    val createCartItem = {
        CartItemModel(
            id = productId,
            name = phoneName,
            price = priceInt,
            originalPrice = (priceInt * 1.15).toInt(),
            imageUrl = topMatch?.image_url ?: "",
            specs = "$processorSpec | $displaySpec",
            hasExtendedWarranty = isWarrantyAdded,
            warrantyPrice = if (isWarrantyAdded) warrantyCost else 0
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            TopAppBar(
                title = { Text(text = "AI Detail Report", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )

            HeroHeaderSection(phoneName, phonePrice, overallMatchFloat, imageList)

            Spacer(modifier = Modifier.height(32.dp))

            AiMatchAnalysisSection(
                perfScore = perfScore,
                camScore = camScore,
                batScore = batScore,
                dispScore = dispScore,
                gamingScore = gamingScore,
                processor = processorSpec,
                camera = cameraSpec,
                battery = batterySpec,
                display = displaySpec
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedStorageSelector()

            Spacer(modifier = Modifier.height(32.dp))

            FlipWarrantyCard(
                isWarrantyAdded = isWarrantyAdded,
                onWarrantyToggle = { isWarrantyAdded = it },
                warrantyCost = warrantyCost
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        FloatingAiActionBar(
            onAddToCart = {
                if (productId != -1) {
                    CartManager.addItem(createCartItem())
                    Toast.makeText(context, "Added to Cart!", Toast.LENGTH_SHORT).show()
                    onAddToCart()
                } else {
                    Toast.makeText(context, "Error: Product ID missing", Toast.LENGTH_SHORT).show()
                }
            },
            onBuyNow = {
                if (productId != -1) {
                    CartManager.addItem(createCartItem())
                    onBuyNow()
                } else {
                    Toast.makeText(context, "Error: Product ID missing", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            basePrice = priceInt,
            warrantyPrice = if (isWarrantyAdded) warrantyCost else 0
        )
    }
}

// ==========================================
// 1. HERO HEADER
// ==========================================
@Composable
fun HeroHeaderSection(name: String, price: String, matchFloat: Float, images: List<String>) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }

    val animatedMatch by animateFloatAsState(
        targetValue = if (played) matchFloat else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val pagerState = rememberPagerState { images.size }
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = images[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentScale = ContentScale.Fit,
                        error = painterResource(android.R.drawable.ic_menu_report_image)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E1E2C))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(price, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2962FF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("EMI from ₹2,686/mo", fontSize = 12.sp, color = Color.Gray)
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    CircularProgressIndicator(progress = { 1f }, color = Color(0xFFE0E0E0), strokeWidth = 6.dp, modifier = Modifier.fillMaxSize())
                    CircularProgressIndicator(progress = { animatedMatch }, color = Color(0xFF03A9F4), strokeWidth = 6.dp, modifier = Modifier.fillMaxSize())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(animatedMatch * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
                        Text("Match", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. AI MATCH ANALYSIS
// ==========================================
@Composable
fun AiMatchAnalysisSection(
    perfScore: Float, camScore: Float, batScore: Float, dispScore: Float, gamingScore: Float,
    processor: String, camera: String, battery: String, display: String
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF03A9F4), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Match Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedAnalysisBar("Performance", processor, perfScore)
        AnimatedAnalysisBar("Camera", camera, camScore)
        AnimatedAnalysisBar("Battery", battery, batScore)
        AnimatedAnalysisBar("Gaming", "Optimized Heat Management", gamingScore)
        AnimatedAnalysisBar("Display", display, dispScore)
    }
}

@Composable
fun AnimatedAnalysisBar(title: String, subtitle: String, targetProgress: Float) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }

    val progress by animateFloatAsState(targetValue = if (played) targetProgress else 0f, animationSpec = tween(1200, 300, FastOutSlowInEasing), label = "")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, color = Color(0xFF1E1E2C), fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
            Text("${(progress * 100).toInt()}%", fontSize = 14.sp, color = Color(0xFF2962FF), fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0xFFEEEEEE), CircleShape)) {
            Box(modifier = Modifier.fillMaxWidth(fraction = progress).height(6.dp).background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), CircleShape))
        }
    }
}

// ==========================================
// 3. STORAGE VARIANT
// ==========================================
@Composable
fun AnimatedStorageSelector() {
    var selected by remember { mutableStateOf("128 GB") }
    val options = listOf("128 GB", "256 GB", "512 GB")

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Storage Variant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isSelected = selected == option
                val bgColor by animateColorAsState(if (isSelected) Color(0xFF2962FF) else Color(0xFFF5F5F5), label = "")
                val textColor by animateColorAsState(if (isSelected) Color.White else Color.DarkGray, label = "")

                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bgColor).clickable { selected = option }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text(option, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

// ==========================================
// 4. WARRANTY CARD
// ==========================================
@Composable
fun FlipWarrantyCard(isWarrantyAdded: Boolean, onWarrantyToggle: (Boolean) -> Unit, warrantyCost: Int) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "")

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Warranty & Protection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp).graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }.clickable { flipped = !flipped },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isWarrantyAdded) Color(0xFF0D47A1) else if (rotation > 90f) Color(0xFF2962FF) else Color(0xFFF3F6FD))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (rotation <= 90f) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, "Warranty", tint = if(isWarrantyAdded) Color.White else Color(0xFF2962FF), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if(isWarrantyAdded) "Maximum 3-Year Coverage Active" else "1 Year Standard Warranty", fontWeight = FontWeight.Bold, color = if(isWarrantyAdded) Color.White else Color(0xFF1E1E2C))
                            Text(if(isWarrantyAdded) "Standard + 2 Year Extended" else "Tap to view extended protection", fontSize = 12.sp, color = if(isWarrantyAdded) Color.White.copy(0.8f) else Color.Gray)
                        }
                        if (isWarrantyAdded) {
                            Icon(Icons.Default.CheckCircle, "Added", tint = Color(0xFF00C853))
                        }
                    }
                } else {
                    Column(modifier = Modifier.graphicsLayer { rotationY = 180f }.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Extended 2-Year Plan", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Accidental & Liquid (+₹$warrantyCost)", fontSize = 12.sp, color = Color.White.copy(0.8f))
                                Text("(Max Limit: 3 Years Total)", fontSize = 10.sp, color = Color(0xFFFFD700))
                            }

                            Button(
                                onClick = {
                                    onWarrantyToggle(!isWarrantyAdded)
                                    flipped = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if(isWarrantyAdded) Color.Red else Color.White)
                            ) {
                                Text(if(isWarrantyAdded) "Remove" else "Add", color = if(isWarrantyAdded) Color.White else Color(0xFF2962FF), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FLOATING ACTION BAR
// ==========================================
@Composable
fun FloatingAiActionBar(onAddToCart: () -> Unit, onBuyNow: () -> Unit, modifier: Modifier = Modifier, basePrice: Int, warrantyPrice: Int) {
    val total = basePrice + warrantyPrice

    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (warrantyPrice > 0) {
                Text("Total: ₹$total (Includes ₹$warrantyPrice Warranty)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onAddToCart, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF2962FF))) {
                    Text("Add to Cart", color = Color(0xFF2962FF), fontWeight = FontWeight.Bold)
                }
                Button(onClick = onBuyNow, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues()) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("Buy Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}