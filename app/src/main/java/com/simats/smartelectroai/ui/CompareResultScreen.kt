package com.simats.smartelectroai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel

import com.simats.smartelectroai.api.CompareDeviceDetail
import com.simats.smartelectroai.api.RecommendationData
import com.simats.smartelectroai.api.RecommendationManager
import com.simats.smartelectroai.api.TopMatch

// --- Colors ---
private val ResBlue = Color(0xFF2962FF)
private val ResTextMain = Color(0xFF1E1E1E)
private val ResTextSub = Color(0xFF757575)
private val ResBgGray = Color(0xFFF8F9FA)
private val ResMatchBg = Color(0xFFE3F2FD)
private val ResLightBlueHeader = Color(0xFFE3F2FD)
private val ResRed = Color(0xFFD32F2F)
private val ResGreenBg = Color(0xFFE8F5E9)
private val ResRedBg = Color(0xFFFFEBEE)

// FIXED: Removed the nested API call. Instantly loads data using the ID from the comparison!
private fun sendToProductDetailsAndNavigate(device: CompareDeviceDetail, onNavigate: (String) -> Unit) {
    RecommendationManager.result = RecommendationData(
        top_match = TopMatch(
            id = device.id ?: -1, // Reads the ID directly from the updated Python backend
            name = device.name,
            price = device.price,
            match_percent = device.spec_score,
            battery_spec = device.battery.capacity,
            display_spec = "${device.display.size} ${device.display.type}",
            processor_spec = device.performance.processor,
            camera_spec = device.camera.rear_main,
            image_url = device.image_url,
            image_urls = if (device.image_url != null) listOf(device.image_url) else emptyList()
        ),
        alternatives = emptyList(),
        analysis = "Transferred from your comparison analysis."
    )
    onNavigate("ProductDetail")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareResultScreen(
    deviceNames: List<String>,
    viewModel: CompareViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    LaunchedEffect(deviceNames) {
        viewModel.fetchCompareData(deviceNames)
    }

    val state by viewModel.compareState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Devices", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ResTextMain) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ResTextMain) }
                },
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Add, "Add", tint = ResBlue, modifier = Modifier.background(ResMatchBg, CircleShape).padding(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            AnimatedFloatingDock(onNavigate)
        },
        containerColor = Color.White
    ) { paddingValues ->

        when (state) {
            is CompareUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ResBlue)
                }
            }
            is CompareUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text((state as CompareUiState.Error).message, color = ResRed)
                }
            }
            is CompareUiState.Success -> {
                val successState = state as CompareUiState.Success
                val device1 = successState.data.device1
                val device2 = successState.data.device2

                CompareContent(
                    device1 = device1,
                    device2 = device2,
                    aiAnalysisText = successState.data.ai_analysis,
                    paddingValues = paddingValues,
                    onBack = onBack,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
fun CompareContent(
    device1: CompareDeviceDetail,
    device2: CompareDeviceDetail,
    aiAnalysisText: String,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompareProductHeader(modifier = Modifier.weight(1f), name = device1.name, price = device1.price, match = device1.spec_score, imageUrl = device1.image_url)
            CompareProductHeader(modifier = Modifier.weight(1f), name = device2.name, price = device2.price, match = device2.spec_score, imageUrl = device2.image_url)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ResLightBlueHeader)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = ResBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = aiAnalysisText, color = ResTextMain, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SpecsSectionTitle("SUMMARY")
        SpecsRow("Spec Score", device1.spec_score, device2.spec_score, highlight = true, isBold = true)
        SpecsRow("Release Date", device1.release_date, device2.release_date)

        Spacer(modifier = Modifier.height(16.dp))

        SpecsSectionTitle("PERFORMANCE")
        SpecsRow("Processor", device1.performance.processor, device2.performance.processor, highlight = true)
        SpecsRow("Cores (Max Freq)", device1.performance.cores, device2.performance.cores)
        SpecsRow("RAM", device1.performance.ram, device2.performance.ram, isBold = true)

        Spacer(modifier = Modifier.height(16.dp))
        SpecsSectionTitle("DISPLAY")
        SpecsRow("Screen Size", device1.display.size, device2.display.size, highlight = true)
        SpecsRow("Type", device1.display.type, device2.display.type)
        SpecsRow("Resolution", device1.display.resolution, device2.display.resolution)
        SpecsRow("Refresh Rate", device1.display.refresh_rate, device2.display.refresh_rate)

        Spacer(modifier = Modifier.height(16.dp))
        SpecsSectionTitle("CAMERA")
        SpecsRow("Main", device1.camera.rear_main, device2.camera.rear_main, highlight = true)
        SpecsRow("Secondary", device1.camera.rear_secondary, device2.camera.rear_secondary)
        SpecsRow("Tertiary", device1.camera.rear_tertiary, device2.camera.rear_tertiary)
        SpecsRow("Front Camera", device1.camera.front, device2.camera.front, highlight = true)

        Spacer(modifier = Modifier.height(16.dp))
        SpecsSectionTitle("BATTERY")
        SpecsRow("Capacity", device1.battery.capacity, device2.battery.capacity, highlight = true)
        SpecsRow("Charging Speed", device1.battery.charging, device2.battery.charging)

        Spacer(modifier = Modifier.height(16.dp))
        SpecsSectionTitle("STORAGE")
        SpecsRow("Internal", device1.storage.internal, device2.storage.internal, highlight = true)
        SpecsRow("Type", device1.storage.type, device2.storage.type)

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth().background(ResGreenBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp)) {
                    Text("Pros", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                }
                Box(modifier = Modifier.fillMaxWidth().background(ResGreenBg.copy(alpha=0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).padding(12.dp)) {
                    Column { device1.pros.forEach { pro -> Text("• $pro", fontSize = 11.sp, color = ResTextMain, modifier = Modifier.padding(bottom = 4.dp)) } }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth().background(ResGreenBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp)) {
                    Text("Pros", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                }
                Box(modifier = Modifier.fillMaxWidth().background(ResGreenBg.copy(alpha=0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).padding(12.dp)) {
                    Column { device2.pros.forEach { pro -> Text("• $pro", fontSize = 11.sp, color = ResTextMain, modifier = Modifier.padding(bottom = 4.dp)) } }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth().background(ResRedBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp)) {
                    Text("Cons", fontWeight = FontWeight.Bold, color = ResRed, fontSize = 13.sp)
                }
                Box(modifier = Modifier.fillMaxWidth().background(ResRedBg.copy(alpha=0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).padding(12.dp)) {
                    Column { device1.cons.forEach { con -> Text("• $con", fontSize = 11.sp, color = ResTextMain, modifier = Modifier.padding(bottom = 4.dp)) } }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth().background(ResRedBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(12.dp)) {
                    Text("Cons", fontWeight = FontWeight.Bold, color = ResRed, fontSize = 13.sp)
                }
                Box(modifier = Modifier.fillMaxWidth().background(ResRedBg.copy(alpha=0.5f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)).padding(12.dp)) {
                    Column { device2.cons.forEach { con -> Text("• $con", fontSize = 11.sp, color = ResTextMain, modifier = Modifier.padding(bottom = 4.dp)) } }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { sendToProductDetailsAndNavigate(device1, onNavigate) }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, ResBlue)) { Text("View Details", fontSize = 11.sp, color = ResBlue, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { sendToProductDetailsAndNavigate(device1, onNavigate) }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = ResBlue)) { Text("Buy Now", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { sendToProductDetailsAndNavigate(device2, onNavigate) }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, ResBlue)) { Text("View Details", fontSize = 11.sp, color = ResBlue, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { sendToProductDetailsAndNavigate(device2, onNavigate) }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = ResBlue)) { Text("Buy Now", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onBack() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Delete, null, tint = ResRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Remove All", color = ResRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun CompareProductHeader(modifier: Modifier = Modifier, name: String, price: String, match: String, imageUrl: String?) {
    Card(modifier = modifier.heightIn(min = 200.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Box {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.background(ResMatchBg, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = match, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ResBlue)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ResTextMain, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = price, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ResBlue)
            }
        }
    }
}

@Composable
fun SpecsSectionTitle(title: String) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ResTextSub, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp)) }

@Composable
fun SpecsRow(label: String, val1: String, val2: String, highlight: Boolean = false, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (highlight) ResLightBlueHeader else Color.Transparent).padding(vertical = 12.dp, horizontal = 8.dp)) {
        Text(label, fontSize = 12.sp, color = ResTextSub, modifier = Modifier.weight(0.8f))
        Text(val1, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, color = if (highlight) ResBlue else ResTextMain, modifier = Modifier.weight(1f))
        Text(val2, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, color = if (highlight && !isBold) ResTextMain else if (isBold) ResBlue else ResTextMain, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AnimatedFloatingDock(onNavigate: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val aiGlow by infiniteTransition.animateFloat(initialValue = 4.dp.value, targetValue = 16.dp.value, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    Box(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 16.dp, vertical = 16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(40.dp), spotColor = ResBlue), shape = RoundedCornerShape(40.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DockIcon(Icons.Default.Home, false) { onNavigate("Dashboard") }
                DockIcon(Icons.AutoMirrored.Filled.CompareArrows, true) { /* Already here */ }

                val aiInteraction = remember { MutableInteractionSource() }
                val aiPressed by aiInteraction.collectIsPressedAsState()
                val aiScale by animateFloatAsState(if (aiPressed) 0.9f else 1.1f, label = "")

                Box(modifier = Modifier.size(56.dp).scale(aiScale).shadow(aiGlow.dp, CircleShape, spotColor = ResBlue).background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF00B0FF))), CircleShape).clickable(interactionSource = aiInteraction, indication = null) { onNavigate("Chat") }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp)) }

                DockIcon(Icons.Default.VerifiedUser, false) { onNavigate("MyWarranty") }
                DockIcon(Icons.Default.Person, false) { onNavigate("Profile") }
            }
        }
    }
}

@Composable
private fun DockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val color by animateColorAsState(if (isSelected) ResBlue else ResTextSub, label = "")
    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp).scale(scale).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() })
}