package com.simats.smartelectroai.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.OrderHistoryItem
import com.simats.smartelectroai.api.OrderTrackingManager
import kotlinx.coroutines.delay
import java.util.Locale

// --- AMAZON UI COLOR PALETTE ---
private val AmzTeal = Color(0xFF007185)
private val AmzDarkText = Color(0xFF0F1111)
private val AmzGrayText = Color(0xFF565959)
private val AmzBorder = Color(0xFFD5D9D9)
private val AmzBackground = Color(0xFFF2F4F8)
private val AmzGreen = Color(0xFF067D62)
private val AmzRed = Color(0xFFB12704)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(onBack: () -> Unit) {
    val order = OrderTrackingManager.currentOrder
    val context = LocalContext.current
    if (order == null) return

    val safePriceStr = order.price.replace(Regex("[^0-9.]"), "")
    val safePrice = order.raw_price ?: safePriceStr.toDoubleOrNull() ?: 0.0
    val listingPrice = safePrice * 1.15
    val discount = listingPrice - safePrice
    val fees = if (safePrice > 0) 39.0 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Order", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = AmzDarkText) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AmzDarkText) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AmzBackground
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // 🚀 FIXED: Using Spacers instead of Modifier.padding to bypass the compiler bug completely!
            Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))

            // --- 1. PRODUCT & STATUS OVERVIEW ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 16.dp)) {
                val currentStatus = order.status.lowercase(Locale.getDefault())
                val isDelivered = currentStatus.contains("delivered")
                val isShipped = currentStatus.contains("shipped") || currentStatus.contains("out for") || isDelivered

                // Header Status Text
                Text(
                    text = if (isDelivered) "Delivered" else if (isShipped) "Arriving soon" else "Ordered",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDelivered) AmzGreen else AmzDarkText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = if (isDelivered) "Your package was handed directly to resident." else "Your package is being processed.",
                    fontSize = 14.sp,
                    color = AmzGrayText,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                )

                // Product Snapshot
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).background(Color.White).border(1.dp, AmzBorder, RoundedCornerShape(8.dp)).padding(6.dp)) {
                        AsyncImage(model = order.image_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(order.product_name, fontSize = 14.sp, color = AmzDarkText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Qty: 1 • ₹${safePrice.toInt()}", fontSize = 13.sp, color = AmzGrayText)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = AmzBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // 🚀 VERTICAL ANIMATED TRACKER
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    VerticalShipmentTracker(status = order.status, orderDate = order.date)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 2. DELIVERY DETAILS ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text("Delivery details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmzDarkText, modifier = Modifier.padding(bottom = 12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Home, null, tint = AmzGrayText, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(order.delivery_name ?: "Customer Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmzDarkText)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(order.delivery_address ?: "Address securely captured", fontSize = 14.sp, color = AmzDarkText, lineHeight = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null, tint = AmzGrayText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(order.delivery_phone ?: "Phone unavailable", fontSize = 14.sp, color = AmzDarkText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 3. PRICE DETAILS ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text("Payment information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmzDarkText, modifier = Modifier.padding(bottom = 12.dp))

                InvoicePriceRow("Listing price", "₹${listingPrice.toInt()}")
                InvoicePriceRow("Special price", "-₹${discount.toInt()}", isDiscount = true)
                InvoicePriceRow("Shipping fees", "₹${fees.toInt()}")

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AmzBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmzDarkText)
                    Text("₹${safePrice.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AmzDarkText)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Paid via ${order.payment_method ?: "Online"}", fontSize = 13.sp, color = AmzGrayText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 4. ORDER ID & INVOICE BUTTON ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", fontSize = 14.sp, color = AmzGrayText)
                    Text(order.invoice_no ?: "N/A", fontSize = 14.sp, color = AmzDarkText, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { generateInvoicePdf(context, order) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmzTeal)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Invoice", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Push bottom content above navigation bar safely
            Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding() + 24.dp))
        }
    }
}

// 🚀 MODERN VERTICAL SHIPMENT TRACKER ANIMATION
@Composable
fun VerticalShipmentTracker(status: String, orderDate: String) {
    val currentStatus = status.lowercase(Locale.getDefault())
    val activeStep = when {
        currentStatus.contains("delivered") -> 3
        currentStatus.contains("out for") -> 2
        currentStatus.contains("shipped") -> 1
        else -> 0
    }

    val steps = listOf(
        Triple("Order Confirmed", "Your order has been placed successfully.", orderDate),
        Triple("Shipped", "Package has left the SmartElectro facility.", if (activeStep >= 1) "Updated tracking" else "Pending"),
        Triple("Out for Delivery", "Our courier is on the way to your address.", if (activeStep >= 2) "Expected today" else "Pending"),
        Triple("Delivered", "Package was handed to resident.", if (activeStep >= 3) "Delivered successfully" else "Pending")
    )

    val progress = remember { Animatable(0f) }

    LaunchedEffect(activeStep) {
        progress.snapTo(0f)
        delay(200)
        progress.animateTo(
            targetValue = activeStep.toFloat(),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, stepData ->
            val isLast = index == steps.size - 1
            val isReached = progress.value >= index

            val lineFillFraction = (progress.value - index).coerceIn(0f, 1f)

            val nodeScale by animateFloatAsState(
                targetValue = if (isReached) 1f else 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "nodeScale"
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {

                // --- LEFT: TIMELINE GRAPHICS ---
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(30.dp)) {

                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(24.dp)
                            .scale(nodeScale)
                            .background(if (isReached) AmzGreen else Color.White, CircleShape)
                            .border(2.dp, if (isReached) AmzGreen else AmzBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReached) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .width(2.dp)
                                .height(50.dp)
                                .background(AmzBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(lineFillFraction)
                                    .background(AmzGreen)
                            )
                        }
                    }
                }

                // --- RIGHT: TEXT CONTENT ---
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = if (isLast) 0.dp else 24.dp)
                        .alpha(if (isReached) 1f else 0.4f)
                ) {
                    Text(
                        text = stepData.first,
                        fontWeight = if (isReached) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isReached) AmzDarkText else AmzGrayText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stepData.second,
                        fontSize = 13.sp,
                        color = AmzGrayText,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stepData.third,
                        fontSize = 12.sp,
                        color = if (isReached) AmzTeal else Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoicePriceRow(label: String, value: String, isDiscount: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = AmzGrayText)
        Text(value, fontSize = 14.sp, color = if (isDiscount) AmzRed else AmzDarkText, fontWeight = if (isDiscount) FontWeight.Bold else FontWeight.Medium)
    }
}

// ==========================================
// NATIVE PDF GENERATION LOGIC (UNCHANGED)
// ==========================================
fun generateInvoicePdf(context: Context, order: OrderHistoryItem) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 28f; isFakeBoldText = true }
        val boldPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val normalPaint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 14f }
        val bluePaint = Paint().apply { color = android.graphics.Color.parseColor("#007185"); textSize = 18f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 2f }

        canvas.drawText("TAX INVOICE", 40f, 60f, titlePaint)

        canvas.drawText("Order ID: ${order.invoice_no}", 40f, 110f, normalPaint)
        canvas.drawText("Date: ${order.date}", 40f, 135f, normalPaint)

        canvas.drawLine(40f, 160f, 555f, 160f, linePaint)

        canvas.drawText("Delivery To:", 40f, 200f, boldPaint)
        canvas.drawText(order.delivery_name ?: "User", 40f, 230f, normalPaint)
        canvas.drawText(order.delivery_phone ?: "", 40f, 255f, normalPaint)

        val addressStr = order.delivery_address ?: ""
        if (addressStr.length > 50) {
            canvas.drawText(addressStr.substring(0, 50), 40f, 280f, normalPaint)
            canvas.drawText(addressStr.substring(50), 40f, 305f, normalPaint)
        } else {
            canvas.drawText(addressStr, 40f, 280f, normalPaint)
        }

        canvas.drawLine(40f, 340f, 555f, 340f, linePaint)

        canvas.drawText("Item Details:", 40f, 380f, boldPaint)

        val productStr = order.product_name
        if (productStr.length > 60) {
            canvas.drawText(productStr.substring(0, 60), 40f, 410f, normalPaint)
            canvas.drawText(productStr.substring(60), 40f, 435f, normalPaint)
        } else {
            canvas.drawText(productStr, 40f, 410f, normalPaint)
        }

        canvas.drawText(order.price, 450f, 410f, bluePaint)

        canvas.drawLine(40f, 480f, 555f, 480f, linePaint)

        canvas.drawText("Total Paid Amount:", 40f, 530f, boldPaint)
        canvas.drawText(order.price, 450f, 530f, titlePaint)

        canvas.drawText("Thank you for shopping with SmartElectro!", 40f, 780f, normalPaint)

        pdfDocument.finishPage(page)

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Invoice_${order.invoice_no}.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(context, "Invoice saved to Downloads!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to save Invoice.", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
