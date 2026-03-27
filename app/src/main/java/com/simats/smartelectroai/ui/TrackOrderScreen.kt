package com.simats.smartelectroai.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.simats.smartelectroai.api.BaseResponse
import com.simats.smartelectroai.api.OrderHistoryItem
import com.simats.smartelectroai.api.OrderTrackingManager
import com.simats.smartelectroai.api.RetrofitClient
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
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

    // 🚀 State to instantly update UI when cancelled
    var localStatus by remember { mutableStateOf(order.status) }
    var isCancelling by remember { mutableStateOf(false) }

    val currentStatus = localStatus.lowercase(Locale.getDefault())
    val isCancelled = currentStatus.contains("cancel")
    val isDelivered = currentStatus.contains("delivered")
    val isShipped = currentStatus.contains("shipped") || currentStatus.contains("out for") || isDelivered

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
            Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))

            // --- 1. PRODUCT & STATUS OVERVIEW ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 16.dp)) {

                // Header Status Text
                Text(
                    text = if (isCancelled) "Cancelled" else if (isDelivered) "Delivered" else if (isShipped) "Arriving soon" else "Ordered",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) AmzRed else if (isDelivered) AmzGreen else AmzDarkText,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = if (isCancelled) "This order was cancelled and will not be shipped." else if (isDelivered) "Your package was handed directly to resident." else "Your package is being processed.",
                    fontSize = 14.sp,
                    color = AmzGrayText,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                )

                // Product Snapshot
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).background(Color.White).border(1.dp, AmzBorder, RoundedCornerShape(8.dp)).padding(6.dp)) {
                        AsyncImage(
                            model = order.image_url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            // Turn image gray if cancelled
                            colorFilter = if (isCancelled) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.product_name,
                            fontSize = 14.sp,
                            color = if (isCancelled) AmzGrayText else AmzDarkText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val formatter = NumberFormat.getIntegerInstance(Locale("en", "IN"))
                        Text("Qty: 1 • ₹${formatter.format(safePrice.toInt())}", fontSize = 13.sp, color = AmzGrayText)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = AmzBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                // 🚀 HIDE TRACKER IF CANCELLED
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (isCancelled) {
                        Box(modifier = Modifier.fillMaxWidth().background(AmzRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, null, tint = AmzRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Order has been cancelled.", color = AmzRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        VerticalShipmentTracker(status = localStatus, orderDate = order.date)
                    }
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
                val formatter = NumberFormat.getIntegerInstance(Locale("en", "IN"))
                Text("Payment information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmzDarkText, modifier = Modifier.padding(bottom = 12.dp))

                InvoicePriceRow("Listing price", "₹${formatter.format(listingPrice.toInt())}")
                InvoicePriceRow("Special price", "-₹${formatter.format(discount.toInt())}", isDiscount = true)
                InvoicePriceRow("Shipping fees", "₹${formatter.format(fees.toInt())}")

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AmzBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmzDarkText)
                    Text("₹${formatter.format(safePrice.toInt())}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AmzDarkText)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Paid via ${order.payment_method ?: "Online"}", fontSize = 13.sp, color = AmzGrayText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 4. ACTIONS (INVOICE & CANCEL) ---
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", fontSize = 14.sp, color = AmzGrayText)
                    Text(order.invoice_no ?: "N/A", fontSize = 14.sp, color = AmzDarkText, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { downloadInvoiceAsPdf(context, order) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmzTeal)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Invoice", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }

                // 🚀 CANCEL BUTTON (Only if not shipped/delivered/cancelled)
                if (!isShipped && !isCancelled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            isCancelling = true
                            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            val token = prefs.getString("jwt_token", "") ?: ""

                            RetrofitClient.instance.cancelOrder("Bearer $token", order.order_id).enqueue(object : Callback<BaseResponse> {
                                override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                                    isCancelling = false
                                    if (response.isSuccessful && response.body()?.status == "success") {
                                        Toast.makeText(context, "Order Cancelled Successfully", Toast.LENGTH_SHORT).show()
                                        localStatus = "Cancelled"
                                        order.status = "Cancelled"
                                    } else {
                                        Toast.makeText(context, "Failed to cancel order.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                                    isCancelling = false
                                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, AmzRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmzRed)
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AmzRed, strokeWidth = 2.dp)
                        } else {
                            Text("Cancel Order", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

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

// =========================================================
// 🚀 NATIVE ANDROID SHARING & PDF GENERATION FUNCTIONS
// =========================================================

fun generateInvoicePdf(context: Context, order: OrderHistoryItem): File? {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // --- CALCULATION LOGIC ---
        val safePriceStr = order.price.replace(Regex("[^0-9.]"), "")
        val safePrice = order.raw_price ?: safePriceStr.toDoubleOrNull() ?: 0.0
        val rate = safePrice / 1.18
        val gst = safePrice - rate

        val formatter = NumberFormat.getIntegerInstance(Locale("en", "IN"))
        val rateStr = formatter.format(rate.toInt())
        val gstStr = formatter.format(gst.toInt())
        val totalStr = formatter.format(safePrice.toInt())

        // --- PAINTS ---
        val titlePaint = Paint().apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(25, 118, 210) // InvBlue
        }
        val boldPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
        }
        val normalPaint = Paint().apply {
            textSize = 11f
            color = android.graphics.Color.rgb(60, 60, 60)
        }
        val smallBoldGray = Paint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.GRAY
        }
        val rightAlignNormal = Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightAlignBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        val bgGrayPaint = Paint().apply { color = android.graphics.Color.rgb(245, 247, 250) }
        val bgGreenPaint = Paint().apply { color = android.graphics.Color.rgb(232, 245, 233) }
        val greenTextPaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.rgb(46, 125, 50)
        }

        val margin = 50f
        val rightMargin = 545f

        // 1. Header
        canvas.drawText("Mobiqo", margin, 70f, titlePaint)
        canvas.drawText("ELECTRONICS PLATFORM", margin, 85f, smallBoldGray)

        // 2. Order Info Background Box
        canvas.drawRoundRect(RectF(margin, 110f, rightMargin, 160f), 8f, 8f, bgGrayPaint)

        canvas.drawText("ORDER STATUS", 65f, 130f, smallBoldGray)
        canvas.drawText(order.status, 65f, 145f, if(order.status.lowercase(Locale.getDefault()).contains("delivered")) greenTextPaint else Paint(boldPaint))

        canvas.drawText("ORDER DATE", 250f, 130f, smallBoldGray)
        canvas.drawText(order.date, 250f, 145f, boldPaint)

        canvas.drawText("PAYMENT", 400f, 130f, smallBoldGray)
        canvas.drawText(order.payment_method ?: "Online", 400f, 145f, boldPaint)

        // 3. Invoice Title
        val largeBold = Paint(boldPaint).apply { textSize = 16f }
        canvas.drawText("TAX INVOICE", margin, 210f, largeBold)
        canvas.drawText("${order.invoice_no}", margin, 228f, normalPaint)
        canvas.drawText("Generated: ${order.date}", margin, 243f, normalPaint)

        // 4. Billed To / Sold By
        canvas.drawText("BILLED TO", margin, 280f, smallBoldGray)
        canvas.drawText(order.delivery_name ?: "Customer", margin, 295f, boldPaint)
        canvas.drawText(order.delivery_phone ?: "", margin, 310f, normalPaint)

        // Chunk long address
        val address = order.delivery_address ?: ""
        if (address.length > 40) {
            canvas.drawText(address.substring(0, 40), margin, 325f, normalPaint)
            canvas.drawText(address.substring(40), margin, 340f, normalPaint)
        } else {
            canvas.drawText(address, margin, 325f, normalPaint)
        }

        canvas.drawText("SOLD BY", 300f, 280f, smallBoldGray)
        canvas.drawText("Mobiqo Electronics", 300f, 295f, boldPaint)
        canvas.drawText("support@mobiqo.com", 300f, 310f, normalPaint)
        canvas.drawText("www.mobiqo.com", 300f, 325f, normalPaint)
        canvas.drawText("Tamil Nadu, India 600001", 300f, 340f, normalPaint)

        canvas.drawLine(margin, 360f, rightMargin, 360f, dividerPaint)

        // 5. Table Header Box
        canvas.drawRect(margin, 370f, rightMargin, 395f, bgGrayPaint)

        canvas.drawText("DESCRIPTION", 60f, 387f, smallBoldGray)

        val colHsn = 320f
        val colRate = 390f
        val colGst = 460f
        val colAmt = 535f

        val rightAlignSmallGray = Paint(smallBoldGray).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("HSN CODE", colHsn, 387f, rightAlignSmallGray)
        canvas.drawText("RATE", colRate, 387f, rightAlignSmallGray)
        canvas.drawText("GST @ 18%", colGst, 387f, rightAlignSmallGray)
        canvas.drawText("AMOUNT", colAmt, 387f, rightAlignSmallGray)

        // 6. Table Rows
        var currentY = 415f

        // Product Row (Dynamic shortening)
        val prodName = if(order.product_name.length > 35) order.product_name.substring(0, 35) + "..." else order.product_name
        canvas.drawText(prodName, 60f, currentY, boldPaint)
        canvas.drawText("8517", colHsn, currentY, rightAlignNormal)
        canvas.drawText(rateStr, colRate, currentY, rightAlignNormal)
        canvas.drawText("₹$gstStr", colGst, currentY, rightAlignNormal)
        canvas.drawText(totalStr, colAmt, currentY, rightAlignBold)

        currentY += 15f
        canvas.drawText("Qty: 1", 60f, currentY, normalPaint)

        // Delivery Row
        currentY += 30f
        canvas.drawText("Delivery Charges", 60f, currentY, normalPaint)
        canvas.drawText("-", colHsn, currentY, rightAlignNormal)
        canvas.drawText("-", colRate, currentY, rightAlignNormal)
        canvas.drawText("-", colGst, currentY, rightAlignNormal)
        canvas.drawText("FREE", colAmt, currentY, Paint(greenTextPaint).apply { textAlign = Paint.Align.RIGHT })

        canvas.drawLine(margin, currentY + 15f, rightMargin, currentY + 15f, dividerPaint)

        // 7. Totals Box
        currentY += 25f
        canvas.drawRect(margin, currentY, rightMargin, currentY + 30f, bgGrayPaint)

        canvas.drawText("Total Amount", 60f, currentY + 20f, boldPaint)
        canvas.drawText("₹$gstStr", colGst, currentY + 20f, rightAlignBold)

        val totalPaintBlue = Paint(rightAlignBold).apply { color = android.graphics.Color.rgb(25, 118, 210); textSize = 14f }
        canvas.drawText("₹$totalStr", colAmt, currentY + 20f, totalPaintBlue)

        // 8. Warranty Box
        currentY += 60f
        canvas.drawRoundRect(RectF(margin, currentY, rightMargin, currentY + 65f), 8f, 8f, bgGreenPaint)
        canvas.drawText("1-Year Manufacturer Warranty Included", 60f, currentY + 25f, greenTextPaint)

        val greenNormal = Paint(normalPaint).apply { color = android.graphics.Color.rgb(46, 125, 50) }
        canvas.drawText("Warranty Valid Until: 24 Mar 2027", 60f, currentY + 40f, greenNormal)
        canvas.drawText("Warranty ID: WRN-${order.invoice_no}", 60f, currentY + 55f, greenNormal)

        // 9. Footer
        val centerGray = Paint(normalPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f }
        canvas.drawText("This is a computer-generated invoice and does not require a signature.", 297f, 780f, centerGray)
        canvas.drawText("Enjoy your Mobiqo purchase!", 297f, 795f, centerGray)

        pdfDocument.finishPage(page)

        // Save PDF to Cache for FileProvider
        val file = File(context.cacheDir, "Mobiqo_Invoice_${order.invoice_no}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun downloadInvoiceAsPdf(context: Context, order: OrderHistoryItem) {
    val file = generateInvoicePdf(context, order)
    if (file != null) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening PDF...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF Viewer installed.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Failed to generate PDF.", Toast.LENGTH_SHORT).show()
    }
}