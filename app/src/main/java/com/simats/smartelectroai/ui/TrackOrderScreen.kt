package com.simats.smartelectroai.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// E-commerce Colors
private val EcomGreen = Color(0xFF26A541)
private val EcomBlue = Color(0xFF2874F0)
private val EcomGray = Color(0xFFE0E0E0)
private val EcomBg = Color(0xFFF1F3F6)
private val EcomTextDark = Color(0xFF212121)
private val EcomTextGray = Color(0xFF878787)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(onBack: () -> Unit, onInvoiceClick: () -> Unit = {}) {
    val order = OrderTrackingManager.currentOrder
    val context = LocalContext.current
    if (order == null) return

    // Fake calculations for visual detail matching your screenshot
    val listingPrice = (order.raw_price ?: 0.0) * 1.15
    val discount = listingPrice - (order.raw_price ?: 0.0)
    val fees = 39.0

    // Generate subtitle dynamically based on the current step
    val deliveryText = when (order.delivery_step) {
        1 -> "Your order is being processed"
        2 -> "Your item has been shipped"
        3 -> "Courier is on the way"
        4 -> "Delivered successfully"
        else -> "Processing"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = EcomBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {

            // 1. PRODUCT & HORIZONTAL TRACKING SECTION
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Text
                    Text(order.status, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if(order.delivery_step >= 4) EcomGreen else EcomTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(deliveryText, fontSize = 13.sp, color = EcomTextGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Horizontal Progress Bar (Flipkart Style)
                    HorizontalTrackingBar(currentStep = order.delivery_step)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFF1F3F6))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Product Details
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).background(Color.White).padding(4.dp)) {
                            AsyncImage(model = order.image_url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(order.product_name, fontSize = 14.sp, color = EcomTextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(order.price, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // 2. DELIVERY DETAILS SECTION
            Text("Delivery details", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = EcomTextDark)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Home, null, tint = EcomTextDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(order.delivery_address ?: "No address provided", fontSize = 14.sp, color = EcomTextDark)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, null, tint = EcomTextDark, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${order.delivery_name ?: "User"}  ${order.delivery_phone ?: ""}", fontSize = 14.sp, color = EcomTextDark)
                    }
                }
            }

            // 3. PRICE DETAILS SECTION
            Text("Price details", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = EcomTextDark)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InvoicePriceRow("Listing price", "₹${listingPrice.toInt()}")
                    InvoicePriceRow("Special price", "-₹${discount.toInt()}", EcomGreen)
                    InvoicePriceRow("Total fees", "₹${fees.toInt()}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = Color(0xFFEEEEEE))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total amount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EcomTextDark)
                        Text(order.price, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EcomTextDark)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // FIXED: Payment Method now reads from the database!
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Payment method", fontSize = 14.sp, color = EcomTextDark)
                        }
                        Text(
                            text = order.payment_method ?: "Cash On Delivery",
                            fontSize = 14.sp,
                            color = EcomTextDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. ORDER ID & INVOICE DOWNLOAD
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Order ID", fontSize = 14.sp, color = EcomTextDark, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(order.invoice_no ?: "N/A", fontSize = 13.sp, color = EcomTextGray)

                    Spacer(modifier = Modifier.height(24.dp))

                    // NATIVE PDF DOWNLOAD BUTTON
                    Button(
                        onClick = { generateInvoicePdf(context, order) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcomBlue)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Invoice PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- Flipkart Style Horizontal Tracker ---
@Composable
fun HorizontalTrackingBar(currentStep: Int) {
    val steps = listOf("Confirmed", "Shipped", "Out for\nDelivery", "Delivered")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, title ->
            val stepNumber = index + 1
            val isCompleted = currentStep >= stepNumber
            val isCurrent = currentStep == stepNumber

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    // Left Line (Hide for first item)
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(if (index == 0) Color.Transparent else if (isCompleted) EcomGreen else EcomGray))

                    // Circle Node
                    Box(
                        modifier = Modifier.size(20.dp).clip(CircleShape).background(if (isCompleted) EcomGreen else if (isCurrent) EcomBlue else EcomGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        } else if (isCurrent) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        }
                    }

                    // Right Line (Hide for last item)
                    Box(modifier = Modifier.weight(1f).height(3.dp).background(if (index == steps.size - 1) Color.Transparent else if (currentStep > stepNumber) EcomGreen else EcomGray))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(title, fontSize = 11.sp, color = if (isCompleted || isCurrent) EcomTextDark else EcomTextGray, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun InvoicePriceRow(label: String, value: String, color: Color = EcomTextDark) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = EcomTextDark)
        Text(value, fontSize = 14.sp, color = color)
    }
}

// ==========================================
// NATIVE PDF GENERATION LOGIC
// ==========================================
fun generateInvoicePdf(context: Context, order: OrderHistoryItem) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 28f; isFakeBoldText = true }
        val boldPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val normalPaint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 14f }
        val bluePaint = Paint().apply { color = android.graphics.Color.parseColor("#2874F0"); textSize = 18f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 2f }

        canvas.drawText("TAX INVOICE", 40f, 60f, titlePaint)

        canvas.drawText("Order ID: ${order.invoice_no}", 40f, 110f, normalPaint)
        canvas.drawText("Date: ${order.date}", 40f, 135f, normalPaint)

        canvas.drawLine(40f, 160f, 555f, 160f, linePaint)

        canvas.drawText("Delivery To:", 40f, 200f, boldPaint)
        canvas.drawText("${order.delivery_name ?: "User"}", 40f, 230f, normalPaint)
        canvas.drawText("${order.delivery_phone ?: ""}", 40f, 255f, normalPaint)

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

        canvas.drawText("Thank you for shopping with Mobiqo!", 40f, 780f, normalPaint)

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