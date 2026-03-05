package com.simats.smartelectroai.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.SearchDeviceResult

// --- Colors ---
private val CompBlue = Color(0xFF2962FF)
private val CompTextMain = Color(0xFF1E1E1E)
private val CompTextSub = Color(0xFF757575)
private val CompBgGray = Color(0xFFF8F9FA)
private val CompMatchBg = Color(0xFFE3F2FD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val selectedDevices = remember { mutableStateListOf<SearchDeviceResult>() }

    val searchResults by viewModel.searchResults.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredDevices = searchResults.filter { device ->
        (selectedCategory == "All" || device.category == selectedCategory)
    }

    LaunchedEffect(Unit) {
        viewModel.searchDevice("a") // Load default trending list initially
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Devices", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CompTextMain) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = CompTextMain) }
                },
                actions = {
                    if (selectedDevices.isNotEmpty()) {
                        IconButton(onClick = { selectedDevices.clear() }) { Icon(Icons.Default.Close, "Clear", tint = CompTextMain) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFFFAFAFA))) {
                if (selectedDevices.isNotEmpty()) {
                    CompareBottomBarSelection(
                        selectedDevices = selectedDevices,
                        onRemove = { device -> selectedDevices.remove(device) },
                        onCompareNow = {
                            val namesString = selectedDevices.joinToString(",") { it.name }
                            onNavigate("CompareResult/$namesString")
                        }
                    )
                }
                AnimatedFloatingDock(onNavigate)
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp).background(CompBgGray, RoundedCornerShape(25.dp)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            // FIXED: Only load trending if search is cleared. NO LIVE SEARCHING.
                            if (it.isEmpty()) {
                                viewModel.searchDevice("a")
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = CompTextMain, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                // FIXED: API is called ONLY when the keyboard Search button is clicked
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.searchDevice(searchQuery)
                                }
                                keyboardController?.hide()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("Search smartphones...", color = Color.Gray, fontSize = 14.sp)
                            innerTextField()
                        }
                    )
                }
            }

            // Category Chips
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val categories = listOf("All", "Budget", "Gaming", "Camera", "5G")
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .border(width = if (isSelected) 0.dp else 1.dp, color = if (isSelected) Color.Transparent else Color(0xFFE0E0E0), shape = RoundedCornerShape(50))
                            .background(color = if (isSelected) CompBlue else Color.White, shape = RoundedCornerShape(50))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(category, color = if (isSelected) Color.White else CompTextSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isEmpty()) "Trending Devices" else "Search Results",
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CompTextMain
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Empty State Handling
            if (filteredDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(48.dp), tint = Color(0xFFE0E0E0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No devices found", color = CompTextSub)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDevices) { device ->
                        val isAdded = selectedDevices.contains(device)
                        CompareDeviceCard(
                            device = device,
                            isAdded = isAdded,
                            onToggle = {
                                if (isAdded) selectedDevices.remove(device)
                                else if (selectedDevices.size < 2) selectedDevices.add(device)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// REMAINDER OF UI COMPONENTS (UNCHANGED)
// ==========================================
@Composable
fun CompareDeviceCard(device: SearchDeviceResult, isAdded: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = device.image_url, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(CompMatchBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(text = device.match_percent, fontSize = 10.sp, color = CompBlue, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "4.8", fontSize = 12.sp, color = CompTextMain, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = device.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CompTextMain)
                Text(text = device.price, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CompBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = device.specs, fontSize = 11.sp, color = CompTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Spacer(modifier = Modifier.height(8.dp))
                if (isAdded) {
                    Button(onClick = onToggle, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = CompBlue), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(32.dp).fillMaxWidth()) { Text("Added", fontSize = 11.sp) }
                } else {
                    OutlinedButton(onClick = onToggle, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, CompBlue), colors = ButtonDefaults.outlinedButtonColors(contentColor = CompBlue), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp), modifier = Modifier.height(32.dp).fillMaxWidth()) { Text("+ Add to Compare", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
fun CompareBottomBarSelection(selectedDevices: List<SearchDeviceResult>, onRemove: (SearchDeviceResult) -> Unit, onCompareNow: () -> Unit) {
    Surface(shadowElevation = 24.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CompareSlotBox(device = selectedDevices.getOrNull(0), onRemove = onRemove)
                Box(modifier = Modifier.size(40.dp).background(CompBgGray, CircleShape).border(1.dp, Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) {
                    Text("VS", color = CompBlue, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                CompareSlotBox(device = selectedDevices.getOrNull(1), onRemove = onRemove)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onCompareNow, enabled = selectedDevices.size == 2, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = CompBlue, disabledContainerColor = Color(0xFFE0E0E0), disabledContentColor = Color.Gray)) {
                Text(text = if (selectedDevices.size == 2) "Compare Specifications" else "Add one more device", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CompareSlotBox(device: SearchDeviceResult?, onRemove: (SearchDeviceResult) -> Unit) {
    if (device != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
            Box(modifier = Modifier.size(70.dp)) {
                AsyncImage(model = device.image_url, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(1.dp, CompBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(8.dp), contentScale = ContentScale.Fit)
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(22.dp).background(Color(0xFFE53935), CircleShape).clickable { onRemove(device) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(device.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CompTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
            Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFAFAFA)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color(0xFFBDBDBD)) }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select Device", fontSize = 11.sp, color = CompTextSub)
        }
    }
}

@Composable
private fun AnimatedFloatingDock(onNavigate: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val aiGlow by infiniteTransition.animateFloat(initialValue = 4.dp.value, targetValue = 16.dp.value, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    Box(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 16.dp, vertical = 16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(40.dp), spotColor = CompBlue), shape = RoundedCornerShape(40.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DockIcon(Icons.Default.Home, false) { onNavigate("Dashboard") }
                DockIcon(Icons.AutoMirrored.Filled.CompareArrows, true) { /* Already here */ }

                val aiInteraction = remember { MutableInteractionSource() }
                val aiPressed by aiInteraction.collectIsPressedAsState()
                val aiScale by animateFloatAsState(if (aiPressed) 0.9f else 1.1f, label = "")

                Box(modifier = Modifier.size(56.dp).scale(aiScale).shadow(aiGlow.dp, CircleShape, spotColor = CompBlue).background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF00B0FF))), CircleShape).clickable(interactionSource = aiInteraction, indication = null) { onNavigate("Chat") }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp)) }

                DockIcon(Icons.Default.VerifiedUser, false) { onNavigate("MyWarranty") }
                DockIcon(Icons.Default.Person, false) { onNavigate("Profile") }
            }
        }
    }
}

@Composable
private fun DockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val color by animateColorAsState(if (isSelected) CompBlue else CompTextSub, label = "")
    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp).scale(scale).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() })
}