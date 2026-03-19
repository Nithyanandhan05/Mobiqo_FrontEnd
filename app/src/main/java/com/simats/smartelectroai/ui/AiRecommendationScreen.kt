package com.simats.smartelectroai.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.RecommendationManager

// Simple UI Model to handle swapping smoothly
data class PhoneUiModel(
    val name: String,
    val price: String,
    val imageUrl: String,
    val matchPercent: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendationScreen(
    onBack: () -> Unit = {},
    onProductClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val aiData = RecommendationManager.result

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Decision Report", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E1E2C)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (aiData == null || aiData.top_match == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No recommendation data found.\nPlease try again.")
            }
        } else {
            // 1. Gather ALL phones (Top + Alts) into one unified, clean list
            val allUniquePhones = remember(aiData) {
                val list = mutableListOf<PhoneUiModel>()

                // Add Top Match safely
                aiData.top_match.let { top ->
                    list.add(
                        PhoneUiModel(
                            name = top.name ?: "Unknown Phone",
                            price = top.price ?: "₹--",
                            imageUrl = top.image_url ?: "",
                            matchPercent = top.match_percent ?: "90%"
                        )
                    )
                }

                // Add Alternatives safely
                aiData.alternatives?.forEach { alt ->
                    list.add(
                        PhoneUiModel(
                            name = alt.name ?: "Unknown Phone",
                            price = alt.price ?: "₹--",
                            imageUrl = alt.image_url ?: "",
                            matchPercent = alt.match_percent ?: "85%"
                        )
                    )
                }

                // ULTIMATE FILTER: Remove duplicates by checking the base name (ignores storage variants)
                // Example: "OnePlus 12 (8GB)" and "OnePlus 12 (12GB)" become just "OnePlus 12", blocking the duplicate!
                list.distinctBy { it.name.substringBefore("(").trim() }
            }

            // 2. Track which phone is currently featured as the "Hero"
            var selectedPhone by remember(allUniquePhones) {
                mutableStateOf(allUniquePhones.first())
            }

            // 3. The alternatives list is just ALL phones minus the one currently selected
            val displayAlts = allUniquePhones.filter { it.name != selectedPhone.name }

            val matchFloat = selectedPhone.matchPercent.filter { it.isDigit() }.toFloatOrNull()?.div(100f) ?: 0.9f

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // HERO CARD
                HeroRecommendationCard(
                    name = selectedPhone.name,
                    price = selectedPhone.price,
                    imageUrl = selectedPhone.imageUrl,
                    matchFloat = matchFloat,
                    onClick = onProductClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ANALYSIS SECTION
                AiAnalysisSection(
                    analysisText = aiData.analysis ?: "No detailed analysis provided.",
                    baseMatchFloat = matchFloat,
                    animationKey = selectedPhone.name // Pass name to restart animations on swap
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ALTERNATIVES ROW
                if (displayAlts.isNotEmpty()) {
                    Text(
                        text = "Strong Alternatives",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E2C),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayAlts) { altPhone ->
                            AlternativeCard(
                                phone = altPhone,
                                onClick = {
                                    // SWAP LOGIC: When clicked, update the top selected phone!
                                    selectedPhone = altPhone
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun HeroRecommendationCard(
    name: String, price: String, imageUrl: String, matchFloat: Float, onClick: () -> Unit
) {
    var played by remember(name) { mutableStateOf(false) } // Keyed by name so progress restarts on swap
    LaunchedEffect(name) { played = true }

    val animatedMatch by animateFloatAsState(
        targetValue = if (played) matchFloat else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "HeroProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                    error = rememberVectorPainter(Icons.Default.Warning),
                    placeholder = rememberVectorPainter(Icons.Default.Image)
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Top Match",
                        color = Color(0xFF03A9F4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E1E2C))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(price, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2962FF))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                color = Color(0xFFE0E0E0),
                                strokeWidth = 5.dp
                            )
                            CircularProgressIndicator(
                                progress = { animatedMatch },
                                color = Color(0xFF03A9F4),
                                strokeWidth = 5.dp
                            )
                            Text(
                                text = "${(animatedMatch * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E2C)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Confidence", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("View Full Details", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AiAnalysisSection(analysisText: String, baseMatchFloat: Float, animationKey: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF03A9F4), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Why AI Recommended This", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(analysisText, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedFeatureBar("Performance", (baseMatchFloat * 1.02f).coerceAtMost(0.99f), animationKey)
        AnimatedFeatureBar("Camera", (baseMatchFloat * 0.95f).coerceAtMost(0.98f), animationKey)
        AnimatedFeatureBar("Battery", (baseMatchFloat * 0.98f).coerceAtMost(0.99f), animationKey)
        AnimatedFeatureBar("Display", (baseMatchFloat * 1.0f).coerceAtMost(0.97f), animationKey)
    }
}

@Composable
fun AnimatedFeatureBar(label: String, targetProgress: Float, animationKey: String) {
    var played by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) { played = true }

    val progress by animateFloatAsState(
        targetValue = if (played) targetProgress else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "FeatureBar"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text("${(progress * 100).toInt()}%", fontSize = 13.sp, color = Color(0xFF1E1E2C), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFEEEEEE), CircleShape)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .height(8.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), CircleShape)
            )
        }
    }
}

@Composable
fun AlternativeCard(phone: PhoneUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }, // Triggers the swap when clicked!
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    text = phone.matchPercent,
                    color = Color(0xFF2E7D32),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AsyncImage(
                model = phone.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).padding(8.dp),
                contentScale = ContentScale.Fit,
                error = rememberVectorPainter(Icons.Default.Warning),
                placeholder = rememberVectorPainter(Icons.Default.Image)
            )

            Text(
                text = phone.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E1E2C),
                maxLines = 2,
                modifier = Modifier.height(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = phone.price,
                color = Color(0xFF2962FF),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}