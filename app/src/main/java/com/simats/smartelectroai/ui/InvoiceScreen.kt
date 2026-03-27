package com.simats.smartelectroai.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

// --- Colors ---
private val InvBlue = Color(0xFF1976D2)
private val InvTextDark = Color(0xFF1E1E1E)
private val InvTextGray = Color(0xFF757575)
private val InvBorderColor = Color(0xFFE0E0E0)
private val InvSuccessGreen = Color(0xFF2E7D32)
private val InvSuccessBg = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Invoice", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isLoaded,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
            ) {
                // --- THE "PAPER" INVOICE CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // 1. Mobiqo Brand Header
                        Text("Mobiqo", fontWeight = FontWeight.Black, fontSize = 28.sp, color = InvBlue, letterSpacing = 1.sp)
                        Text("ELECTRONICS PLATFORM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray, letterSpacing = 1.5.sp)

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. Order Info Table (Top)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF5F7FA))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InvoiceTopInfoItem("ORDER STATUS", "Delivered", InvSuccessGreen)
                            InvoiceTopInfoItem("ORDER DATE", "24 Mar 2026", InvTextDark)
                            InvoiceTopInfoItem("PAYMENT", "Cash On Delivery", InvTextDark)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. Invoice Title & ID
                        Text("TAX INVOICE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = InvTextDark)
                        Text("#INV-30R4TPLM\nGenerated: 24/03/2026", fontSize = 12.sp, color = InvTextGray, lineHeight = 18.sp)

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Billed To & Sold By
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("BILLED TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Nithyanandhan R", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InvTextDark)
                                Text("9363441126\nSaveetha Hospital, Saveetha Nagar, Thandalam\n(SIMATS Engineering)", fontSize = 11.sp, color = InvTextGray, lineHeight = 16.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SOLD BY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Mobiqo Electronics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = InvTextDark)
                                Text("support@mobiqo.com\nwww.mobiqo.com\nTamil Nadu, India 600001", fontSize = 11.sp, color = InvTextGray, lineHeight = 16.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = InvBorderColor)

                        // 5. Item Details & Price Breakdown Table
                        Text("ITEM DETAILS & PRICE BREAKDOWN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("DESCRIPTION", modifier = Modifier.weight(2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
                            Text("HSN", modifier = Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray, textAlign = TextAlign.Center)
                            Text("RATE", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray, textAlign = TextAlign.End)
                            Text("GST", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray, textAlign = TextAlign.End)
                            Text("AMOUNT", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InvTextGray, textAlign = TextAlign.End)
                        }

                        // Main Phone Item
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text("Nothing Phone 2a Plus 5G (12GB RAM, 512GB)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InvTextDark)
                                Text("Model: MBQ-34 | Qty: 1", fontSize = 10.sp, color = InvTextGray)
                            }
                            Text("8517", modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.Center)
                            Text("24,797", modifier = Modifier.weight(1f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.End)
                            Text("₹5,443", modifier = Modifier.weight(1f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.End)
                            Text("30,240", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InvTextDark, textAlign = TextAlign.End)
                        }

                        // Delivery Item
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Text("Delivery Charges", modifier = Modifier.weight(2f), fontSize = 11.sp, color = InvTextDark)
                            Text("-", modifier = Modifier.weight(0.7f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.Center)
                            Text("-", modifier = Modifier.weight(1f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.End)
                            Text("-", modifier = Modifier.weight(1f), fontSize = 11.sp, color = InvTextDark, textAlign = TextAlign.End)
                            Text("FREE", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InvSuccessGreen, textAlign = TextAlign.End)
                        }

                        HorizontalDivider(color = InvBorderColor)

                        // 6. Total Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Amount", modifier = Modifier.weight(2.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InvTextDark)
                            Text("₹5,443", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InvTextDark, textAlign = TextAlign.End)
                            Text("₹30,240", modifier = Modifier.weight(1.2f), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = InvBlue, textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 7. Warranty Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, InvSuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(InvSuccessBg, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, null, tint = InvSuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("1-Year Manufacturer Warranty Included", fontSize = 12.sp, color = InvSuccessGreen, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Warranty valid until: 24 Mar 2027", fontSize = 11.sp, color = InvSuccessGreen, modifier = Modifier.padding(start = 24.dp))
                                Text("Warranty ID: WRN-INV-30R4TPLM", fontSize = 11.sp, color = InvSuccessGreen, modifier = Modifier.padding(start = 24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // 8. Footer
                        Text(
                            text = "This is a computer-generated invoice and does not require a signature.\nEnjoy your Mobiqo purchase!",
                            fontSize = 10.sp,
                            color = InvTextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ACTION BUTTONS ---
            AnimatedVisibility(
                visible = isLoaded,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { downloadInvoiceAsPdf(context) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InvBlue)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { shareInvoice(context) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, InvBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InvBlue)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun RowScope.InvoiceTopInfoItem(label: String, value: String, valueColor: Color) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = InvTextGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// =========================================================
// 🚀 NATIVE ANDROID SHARING & PDF GENERATION FUNCTIONS
// =========================================================

fun generateInvoicePdf(context: Context): File? {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

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

        // Background Box Paints
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
        canvas.drawText("Delivered", 65f, 145f, greenTextPaint)

        canvas.drawText("ORDER DATE", 250f, 130f, smallBoldGray)
        canvas.drawText("24 Mar 2026", 250f, 145f, boldPaint)

        canvas.drawText("PAYMENT", 400f, 130f, smallBoldGray)
        canvas.drawText("Cash On Delivery", 400f, 145f, boldPaint)

        // 3. Invoice Title
        val largeBold = Paint(boldPaint).apply { textSize = 16f }
        canvas.drawText("TAX INVOICE", margin, 210f, largeBold)
        canvas.drawText("#INV-30R4TPLM", margin, 228f, normalPaint)
        canvas.drawText("Generated: 24/03/2026", margin, 243f, normalPaint)

        // 4. Billed To / Sold By
        canvas.drawText("BILLED TO", margin, 280f, smallBoldGray)
        canvas.drawText("Nithyanandhan R", margin, 295f, boldPaint)
        canvas.drawText("9363441126", margin, 310f, normalPaint)
        canvas.drawText("Saveetha Hospital, Saveetha Nagar, Thandalam", margin, 325f, normalPaint)
        canvas.drawText("(SIMATS Engineering)", margin, 340f, normalPaint)

        canvas.drawText("SOLD BY", 300f, 280f, smallBoldGray)
        canvas.drawText("Mobiqo Electronics", 300f, 295f, boldPaint)
        canvas.drawText("support@mobiqo.com", 300f, 310f, normalPaint)
        canvas.drawText("www.mobiqo.com", 300f, 325f, normalPaint)
        canvas.drawText("Tamil Nadu, India 600001", 300f, 340f, normalPaint)

        canvas.drawLine(margin, 360f, rightMargin, 360f, dividerPaint)

        // 5. Table Header Box
        canvas.drawRect(margin, 370f, rightMargin, 395f, bgGrayPaint)

        canvas.drawText("DESCRIPTION", 60f, 387f, smallBoldGray)

        // Right Aligned Header positions
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

        // Phone Row
        canvas.drawText("Nothing Phone 2a Plus 5G (12GB RAM, 512GB)", 60f, currentY, boldPaint)
        canvas.drawText("8517", colHsn, currentY, rightAlignNormal)
        canvas.drawText("24,797", colRate, currentY, rightAlignNormal)
        canvas.drawText("₹5,443", colGst, currentY, rightAlignNormal)
        canvas.drawText("30,240", colAmt, currentY, rightAlignBold)

        currentY += 15f
        canvas.drawText("Model: MBQ-34 | Qty: 1", 60f, currentY, normalPaint)

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
        canvas.drawText("₹5,443", colGst, currentY + 20f, rightAlignBold)

        val totalPaintBlue = Paint(rightAlignBold).apply { color = android.graphics.Color.rgb(25, 118, 210); textSize = 14f }
        canvas.drawText("₹30,240", colAmt, currentY + 20f, totalPaintBlue)

        // 8. Warranty Box
        currentY += 60f
        canvas.drawRoundRect(RectF(margin, currentY, rightMargin, currentY + 65f), 8f, 8f, bgGreenPaint)
        canvas.drawText("1-Year Manufacturer Warranty Included", 60f, currentY + 25f, greenTextPaint)

        val greenNormal = Paint(normalPaint).apply { color = android.graphics.Color.rgb(46, 125, 50) }
        canvas.drawText("Warranty Valid Until: 24 Mar 2027", 60f, currentY + 40f, greenNormal)
        canvas.drawText("Warranty ID: WRN-INV-30R4TPLM", 60f, currentY + 55f, greenNormal)

        // 9. Footer
        val centerGray = Paint(normalPaint).apply { textAlign = Paint.Align.CENTER; textSize = 10f }
        canvas.drawText("This is a computer-generated invoice and does not require a signature.", 297f, 780f, centerGray)
        canvas.drawText("Enjoy your Mobiqo purchase!", 297f, 795f, centerGray)

        pdfDocument.finishPage(page)

        // Save PDF to Cache
        val file = File(context.cacheDir, "Mobiqo_Invoice_INV-30R4TPLM.pdf")
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

fun downloadInvoiceAsPdf(context: Context) {
    val file = generateInvoicePdf(context)
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

fun shareInvoice(context: Context) {
    val file = generateInvoicePdf(context)
    if (file != null) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Mobiqo Invoice #INV-30R4TPLM")
                putExtra(Intent.EXTRA_TEXT, "Here is your invoice for the Nothing Phone 2a Plus.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice via..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share PDF.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Failed to generate PDF.", Toast.LENGTH_SHORT).show()
    }
}