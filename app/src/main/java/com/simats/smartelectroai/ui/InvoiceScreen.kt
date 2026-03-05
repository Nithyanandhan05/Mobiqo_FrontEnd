package com.simats.smartelectroai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- Premium Design Colors ---
private val InvBlue = Color(0xFF1976D2)
private val InvGradient = Brush.verticalGradient(listOf(Color(0xFF1976D2), Color(0xFF1565C0)))
private val InvSuccessGreen = Color(0xFF2E7D32)
private val InvSuccessBg = Color(0xFFE8F5E9)
private val InvTextDark = Color(0xFF1E1E1E)
private val InvTextGray = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(onBack: () -> Unit) {
    var isLoaded by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoaded = true
        delay(400)
        showItems = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Receipt", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER SECTION ---
            AnimatedVisibility(
                visible = isLoaded,
                enter = slideInVertically() + fadeIn()
            ) {
                InvoiceHeaderSection()
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- MAIN DOCUMENT CARD ---
            AnimatedVisibility(
                visible = isLoaded,
                enter = expandVertically(animationSpec = spring(Spring.DampingRatioLowBouncy)) + fadeIn()
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("BILL TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                                Text("Nithiyanandhan R", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("SIMATS Engineering, Chennai", fontSize = 12.sp, color = InvTextGray)
                            }
                            // Rotating Logo Box
                            val rotation by animateFloatAsState(if(isLoaded) 360f else 0f, tween(1000))
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .rotate(rotation)
                                    .background(InvBlue, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(Modifier.padding(vertical = 20.dp), color = Color(0xFFF1F1F1))

                        // --- ITEM TABLE ---
                        Text("PURCHASED ITEMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(visible = showItems) {
                            Column {
                                InvoiceItemStaggered(
                                    name = "Google Pixel 8 Pro",
                                    desc = "Obsidian, 256GB Storage",
                                    price = "₹1,06,999",
                                    index = 1
                                )
                                InvoiceItemStaggered(
                                    name = "Premium Protection Plan",
                                    desc = "1-Year Extended Warranty",
                                    price = "₹0 (Included)",
                                    index = 2
                                )
                            }
                        }

                        Divider(Modifier.padding(vertical = 20.dp), color = Color(0xFFF1F1F1))

                        // --- TOTAL CALCULATION ---
                        InvoiceMathRow("Subtotal", "₹1,06,999")
                        InvoiceMathRow("GST (18%)", "₹19,259")

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF4F8FF), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Amount", fontWeight = FontWeight.Bold, color = InvTextDark)
                                Text("₹1,26,258", fontWeight = FontWeight.ExtraBold, color = InvBlue, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Audit Verified Footer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = InvSuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Digitally Verified for Warranty Ownership", fontSize = 10.sp, color = InvSuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- ACTION BUTTONS ---
            AnimatedVisibility(
                visible = showItems,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                Column {
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InvBlue)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Download PDF", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, InvBlue)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Share with Insurance", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun InvoiceHeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("ID: #INV-2026-8812", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Date: Feb 23, 2026", color = InvTextGray, fontSize = 12.sp)
        }
        Surface(
            color = InvSuccessBg,
            shape = RoundedCornerShape(50),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(InvSuccessGreen, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("PAID", color = InvSuccessGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun InvoiceItemStaggered(name: String, desc: String, price: String, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 150L)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = slideInHorizontally() + fadeIn()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = InvTextGray, fontSize = 11.sp)
            }
            Text(price, fontWeight = FontWeight.Bold, color = InvTextDark)
        }
    }
}

@Composable
fun InvoiceMathRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = InvTextGray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = InvTextDark)
    }
}