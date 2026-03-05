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
            val topMatch = aiData.top_match
            val matchString = topMatch.match_percent ?: "90%"
            val matchFloat = matchString.filter { it.isDigit() }.toFloatOrNull()?.div(100f) ?: 0.9f

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                HeroRecommendationCard(
                    name = topMatch.name ?: "Unknown Phone",
                    price = topMatch.price ?: "₹--",
                    imageUrl = topMatch.image_url ?: "",
                    matchFloat = matchFloat,
                    onClick = onProductClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                AiAnalysisSection(
                    analysisText = aiData.analysis ?: "No detailed analysis provided.",
                    baseMatchFloat = matchFloat
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!aiData.alternatives.isNullOrEmpty()) {
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
                        items(aiData.alternatives) { alt ->
                            AlternativeCard(alt)
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
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }

    val animatedMatch by animateFloatAsState(
        targetValue = if (played) matchFloat else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "HeroProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            // Replaced Purple with Light Blue (0xFF03A9F4)
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
                        color = Color(0xFF03A9F4), // Light Blue
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E1E2C))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(price, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2962FF)) // Deep Blue

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
                                color = Color(0xFF03A9F4), // Light Blue
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
                        // Replaced Purple with Light Blue (0xFF03A9F4)
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
fun AiAnalysisSection(analysisText: String, baseMatchFloat: Float) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF03A9F4), modifier = Modifier.size(24.dp)) // Light Blue
            Spacer(modifier = Modifier.width(8.dp))
            Text("Why AI Recommended This", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E2C))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(analysisText, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedFeatureBar("Performance", (baseMatchFloat * 1.02f).coerceAtMost(0.99f))
        AnimatedFeatureBar("Camera", (baseMatchFloat * 0.95f).coerceAtMost(0.98f))
        AnimatedFeatureBar("Battery", (baseMatchFloat * 0.98f).coerceAtMost(0.99f))
        AnimatedFeatureBar("Display", (baseMatchFloat * 1.0f).coerceAtMost(0.97f))
    }
}

@Composable
fun AnimatedFeatureBar(label: String, targetProgress: Float) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { played = true }

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
                    // Replaced Purple with Light Blue (0xFF03A9F4)
                    .background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF03A9F4))), CircleShape)
            )
        }
    }
}

@Composable
fun AlternativeCard(alt: com.simats.smartelectroai.api.Alternative) {
    Card(
        modifier = Modifier.width(160.dp),
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
                    text = alt.match_percent ?: "N/A",
                    color = Color(0xFF2E7D32),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AsyncImage(
                model = alt.image_url ?: "",
                contentDescription = null,
                modifier = Modifier.size(80.dp).padding(8.dp),
                contentScale = ContentScale.Fit,
                error = rememberVectorPainter(Icons.Default.Warning),
                placeholder = rememberVectorPainter(Icons.Default.Image)
            )

            Text(
                text = alt.name ?: "Unknown",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E1E2C),
                maxLines = 2,
                modifier = Modifier.height(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alt.price ?: "-",
                color = Color(0xFF2962FF), // Deep Blue
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}