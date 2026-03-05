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

// --- ADDED CART MANAGER IMPORTS ---
import com.simats.smartelectroai.api.RecommendationManager
import com.simats.smartelectroai.api.CartManager
import com.simats.smartelectroai.api.CartItemModel

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

    // Extract float from match percentage for animations
    val matchString = topMatch?.match_percent ?: "90%"
    val matchFloat = matchString.filter { it.isDigit() }.toFloatOrNull()?.div(100f) ?: 0.9f

    // Specs
    val batterySpec = topMatch?.battery_spec ?: "Standard Battery"
    val displaySpec = topMatch?.display_spec ?: "Standard Display"
    val processorSpec = topMatch?.processor_spec ?: "High Performance Processor"
    val cameraSpec = topMatch?.camera_spec ?: "Advanced Camera System"

    val imageList = topMatch?.image_urls ?: listOf(topMatch?.image_url ?: "")

    // --- CART ITEM PACKAGING LOGIC ---
    // Clean the price string to safely convert it to an integer for calculations
    val cleanPrice = phonePrice.replace(Regex("[^0-9]"), "")
    val priceInt = cleanPrice.toIntOrNull() ?: 30000

    val currentCartItem = CartItemModel(
        id = productId,
        name = phoneName,
        price = priceInt,
        originalPrice = (priceInt * 1.15).toInt(), // Adds a fake 15% original price to show Flipkart discount
        imageUrl = topMatch?.image_url ?: "",
        specs = "$processorSpec | $displaySpec"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp) // Padding so content isn't hidden by Floating Bar
        ) {
            // TOP BAR
            TopAppBar(
                title = { Text(text = "AI Detail Report", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )

            // SECTION 1: HERO HEADER
            HeroHeaderSection(phoneName, phonePrice, matchFloat, imageList)

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 2: AI MATCH ANALYSIS
            AiMatchAnalysisSection(matchFloat, processorSpec, cameraSpec, batterySpec, displaySpec)

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 3: STORAGE (Animated)
            AnimatedStorageSelector()

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 4: COLOR (Animated)
            AnimatedColorSelector()

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 5: WARRANTY (3D Flip)
            FlipWarrantyCard()

            Spacer(modifier = Modifier.height(32.dp))
        }

        // FLOATING ACTION BAR WITH DATABASE INTEGRATION
        FloatingAiActionBar(
            onAddToCart = {
                if (productId != -1) {
                    CartManager.addItem(currentCartItem)
                    Toast.makeText(context, "Added to Cart!", Toast.LENGTH_SHORT).show()
                    onAddToCart() // Triggers the badge update in MainActivity
                } else {
                    Toast.makeText(context, "Error: Product ID missing", Toast.LENGTH_SHORT).show()
                }
            },
            onBuyNow = {
                if (productId != -1) {
                    // Automatically add to cart and proceed to checkout
                    CartManager.addItem(currentCartItem)
                    onBuyNow() // Triggers the navigation in MainActivity
                } else {
                    Toast.makeText(context, "Error: Product ID missing", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            productId = productId
        )
    }
}

// ==========================================
// 1. HERO HEADER (Glassmorphism & Ring)
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
// 2. AI MATCH ANALYSIS (Animated Bars)
// ==========================================
@Composable
fun AiMatchAnalysisSection(
    baseMatch: Float, processor: String, camera: String, battery: String, display: String
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF03A9F4), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AI Match Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedAnalysisBar("Performance", processor, (baseMatch * 1.02f).coerceAtMost(0.99f))
        AnimatedAnalysisBar("Camera", camera, (baseMatch * 0.95f).coerceAtMost(0.98f))
        AnimatedAnalysisBar("Battery", battery, (baseMatch * 0.98f).coerceAtMost(0.99f))
        AnimatedAnalysisBar("Gaming", "Optimized Heat Management", (baseMatch * 0.94f).coerceAtMost(0.97f))
        AnimatedAnalysisBar("Display", display, (baseMatch * 1.0f).coerceAtMost(0.97f))
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
// 3. STORAGE VARIANT (Color Expansion)
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
// 4. COLOR SELECTOR (Scale & Glow Animation)
// ==========================================
@Composable
fun AnimatedColorSelector() {
    var selectedColor by remember { mutableStateOf(Color(0xFF212121)) }
    val colors = listOf(Color(0xFF212121), Color(0xFFE0E0E0), Color(0xFF00695C))

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Device Color", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            colors.forEach { color ->
                val isSelected = selectedColor == color
                val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, label = "")
                val borderWidth by animateFloatAsState(if (isSelected) 3f else 0f, label = "")

                Box(modifier = Modifier.size(40.dp).scale(scale).clip(CircleShape).background(color).border(borderWidth.dp, if (isSelected) Color(0xFF03A9F4) else Color.Transparent, CircleShape).clickable { selectedColor = color })
            }
        }
    }
}

// ==========================================
// 5. WARRANTY CARD (3D Flip Animation)
// ==========================================
@Composable
fun FlipWarrantyCard() {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "")

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Warranty & Protection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp).graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }.clickable { flipped = !flipped },
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (rotation > 90f) Color(0xFF2962FF) else Color(0xFFF3F6FD))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (rotation <= 90f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, "Warranty", tint = Color(0xFF2962FF), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("1 Year Standard Warranty", fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
                            Text("Tap to view extended protection", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Column(modifier = Modifier.graphicsLayer { rotationY = 180f }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Extended 2-Year Plan", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Covers accidental & liquid damage (+₹2,999)", fontSize = 12.sp, color = Color.White.copy(0.8f))
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. FLOATING ACTION BAR & DATABASE API CALL
// ==========================================
@Composable
fun FloatingAiActionBar(onAddToCart: () -> Unit, onBuyNow: () -> Unit, modifier: Modifier = Modifier, productId: Int) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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