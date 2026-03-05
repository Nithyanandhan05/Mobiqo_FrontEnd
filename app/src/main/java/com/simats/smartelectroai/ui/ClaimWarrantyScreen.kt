package com.simats.smartelectroai.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.BaseResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val PrimaryBlue = Color(0xFF2962FF)
private val LightBlue = Color(0xFFE3F2FD)
private val BgWhite = Color(0xFFFFFFFF)
private val GrayText = Color(0xFF757575)
private val DarkText = Color(0xFF1E1E1E)
private val AppBg = Color(0xFFFAFAFA)
private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessGreenBg = Color(0xFFE8F5E9)

private fun prepareFilePart(context: Context, uri: Uri, partName: String): MultipartBody.Part? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, tempFile.name, requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimWarrantyScreen(warrantyId: Int, onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    var selectedIssue by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var selectedServiceMode by remember { mutableStateOf("Service Center Visit") }
    var isSubmitting by remember { mutableStateOf(false) }

    var invoiceUri by remember { mutableStateOf<Uri?>(null) }
    var deviceImageUri by remember { mutableStateOf<Uri?>(null) }

    val invoicePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> invoiceUri = uri }
    val deviceImagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> deviceImageUri = uri }

    var isHeaderVisible by remember { mutableStateOf(false) }
    var isCardVisible by remember { mutableStateOf(false) }
    var isFormVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isHeaderVisible = true
        delay(150); isCardVisible = true
        delay(150); isFormVisible = true
    }

    fun submitClaimToBackend() {
        if (selectedIssue.isEmpty() || issueDescription.isEmpty() || invoiceUri == null || deviceImageUri == null) {
            Toast.makeText(context, "Please fill all fields and upload both images", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true

        coroutineScope.launch(Dispatchers.IO) {
            // FIXED: Using "text/plain" so Python can read the text and not save NULL to DB
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val issueTypeBody = selectedIssue.toRequestBody(textMediaType)
            val descBody = issueDescription.toRequestBody(textMediaType)
            val serviceBody = selectedServiceMode.toRequestBody(textMediaType)

            val invoicePart = prepareFilePart(context, invoiceUri!!, "invoice_image")
            val devicePart = prepareFilePart(context, deviceImageUri!!, "device_image")

            withContext(Dispatchers.Main) {
                if (invoicePart == null || devicePart == null) {
                    Toast.makeText(context, "Error processing images. Try again.", Toast.LENGTH_SHORT).show()
                    isSubmitting = false
                    return@withContext
                }

                RetrofitClient.instance.submitWarrantyClaim(
                    "Bearer $token", warrantyId,
                    issueTypeBody, descBody, serviceBody,
                    invoicePart, devicePart
                ).enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        isSubmitting = false
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Claim Submitted Successfully!", Toast.LENGTH_LONG).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Failed to submit claim", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        isSubmitting = false
                        Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isHeaderVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Claim Warranty", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkText) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DarkText) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        containerColor = AppBg,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(visible = isCardVisible, enter = scaleIn(initialScale = 0.95f) + fadeIn(tween(500))) { ProductWarrantyCard() }

            AnimatedVisibility(visible = isFormVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(600))) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    IssueTypeSelector(selectedIssue = selectedIssue, onSelect = { selectedIssue = it })
                    IssueDescriptionField(value = issueDescription, onValueChange = { issueDescription = it })
                    UploadProofSection(
                        invoiceUri = invoiceUri, deviceUri = deviceImageUri,
                        onInvoiceClick = { invoicePicker.launch("image/*") }, onDeviceClick = { deviceImagePicker.launch("image/*") }
                    )
                    ServiceModeSelector(selectedMode = selectedServiceMode, onSelect = { selectedServiceMode = it })
                    PickupAddressExpandableCard(isVisible = selectedServiceMode == "Doorstep Service")
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedSubmitClaimButton(isSubmitting = isSubmitting) { submitClaimToBackend() }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ProductWarrantyCard() {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(70.dp).background(LightBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Smartphone, contentDescription = "Device", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Submitting Claim", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.background(SuccessGreenBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Warranty", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IssueTypeSelector(selectedIssue: String, onSelect: (String) -> Unit) {
    val issues = listOf("Screen Issue", "Battery Issue", "Charging Problem", "Water Damage", "Software Issue")
    Column {
        SectionTitle("SELECT ISSUE TYPE")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(issues) { issue ->
                val isSelected = selectedIssue == issue
                val interaction = remember { MutableInteractionSource() }
                val isPressed by interaction.collectIsPressedAsState()
                val bgColor by animateColorAsState(if (isSelected) PrimaryBlue else BgWhite, label = "bg")
                val txtColor by animateColorAsState(if (isSelected) BgWhite else GrayText, label = "txt")
                val borderColor by animateColorAsState(if (isSelected) Color.Transparent else Color(0xFFE0E0E0), label = "border")
                val scale by animateFloatAsState(if (isPressed) 0.9f else if (isSelected) 1.05f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

                Box(
                    modifier = Modifier.scale(scale).clip(RoundedCornerShape(50)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(50))
                        .clickable(interactionSource = interaction, indication = null) { onSelect(issue) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(issue, color = txtColor, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun IssueDescriptionField(value: String, onValueChange: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val elevation by animateDpAsState(if (isFocused) 6.dp else 0.dp, tween(300), label = "elev")
    Column {
        SectionTitle("DESCRIBE THE PROBLEM")
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).shadow(elevation, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite), border = BorderStroke(1.dp, if (isFocused) PrimaryBlue else Color(0xFFEEEEEE))
        ) {
            OutlinedTextField(
                value = value, onValueChange = onValueChange, placeholder = { Text("Please explain the issue in detail...", color = Color.LightGray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(120.dp), interactionSource = interactionSource,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), maxLines = 5
            )
        }
    }
}

@Composable
fun UploadProofSection(invoiceUri: Uri?, deviceUri: Uri?, onInvoiceClick: () -> Unit, onDeviceClick: () -> Unit) {
    Column {
        SectionTitle("UPLOAD PROOF")
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UploadBox(modifier = Modifier.weight(1f), icon = Icons.Default.Description, label = "Upload Invoice", uri = invoiceUri, onClick = onInvoiceClick)
            UploadBox(modifier = Modifier.weight(1f), icon = Icons.Default.AddPhotoAlternate, label = "Device Image", uri = deviceUri, onClick = onDeviceClick)
        }
    }
}

@Composable
fun UploadBox(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, uri: Uri?, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "icon_pulse")
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val boxScale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "box_scale")

    Box(
        modifier = modifier.scale(boxScale).height(100.dp).clickable(interactionSource = interaction, indication = null) { onClick() }
            .drawBehind {
                if (uri == null) {
                    val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                    drawRoundRect(color = Color(0xFFBDBDBD), style = stroke, cornerRadius = CornerRadius(12.dp.toPx()))
                }
            }.background(if (uri == null) Color(0xFFFAFAFA) else Color.Transparent, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(SuccessGreen, CircleShape).padding(2.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(40.dp).background(LightBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp).scale(pulseScale))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GrayText)
            }
        }
    }
}

@Composable
fun ServiceModeSelector(selectedMode: String, onSelect: (String) -> Unit) {
    Column {
        SectionTitle("SERVICE MODE")
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BgWhite), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
            Column {
                ServiceRadioRow("Service Center Visit", selectedMode, onSelect)
                HorizontalDivider(color = Color(0xFFEEEEEE))
                ServiceRadioRow("Doorstep Service", selectedMode, onSelect)
            }
        }
    }
}

@Composable
fun ServiceRadioRow(mode: String, selectedMode: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(mode) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedMode == mode, onClick = { onSelect(mode) }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue, unselectedColor = Color.Gray))
        Spacer(modifier = Modifier.width(8.dp))
        Text(mode, fontSize = 14.sp, fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal, color = DarkText)
    }
}

@Composable
fun PickupAddressExpandableCard(isVisible: Boolean) {
    AnimatedVisibility(visible = isVisible, enter = expandVertically(animationSpec = tween(400)) + fadeIn(), exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = LightBlue.copy(alpha = 0.3f)), border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pickup Address", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = "123, Main Street, Chennai, 600001", onValueChange = {}, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = BgWhite, focusedContainerColor = BgWhite), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp))
                }
            }
        }
    }
}

@Composable
fun AnimatedSubmitClaimButton(isSubmitting: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "btn_scale")

    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp).scale(scale).shadow(if (isPressed) 2.dp else 12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryBlue).background(Brush.linearGradient(listOf(Color(0xFF2962FF), Color(0xFF00B0FF))), RoundedCornerShape(16.dp)).clickable(interactionSource = interaction, indication = null, enabled = !isSubmitting) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSubmitting) CircularProgressIndicator(color = BgWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        else Text("Submit Claim", color = BgWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SectionTitle(title: String) { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrayText, letterSpacing = 1.sp) }