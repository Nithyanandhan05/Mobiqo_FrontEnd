package com.simats.smartelectroai.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

// --- IMPORT GLOBAL API CLIENT ---
import com.simats.smartelectroai.api.RetrofitClient
import com.simats.smartelectroai.api.AddWarrantyReq
import com.simats.smartelectroai.api.AddWarrantyRes

// --- DESIGN COLORS ---
private val BluePrimary = Color(0xFF1976D2)
private val TextMainDark = Color(0xFF1E1E1E)
private val TextHintGray = Color(0xFF9E9E9E)
private val SectionGray = Color(0xFF757575)
private val BorderLight = Color(0xFFE0E0E0)
private val CardBg = Color(0xFFFFFFFF)
private val AppBg = Color(0xFFFAFAFA)
private val SecureGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWarrantyScreen(onBack: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Form States
    var deviceName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Apple") }
    var durationMonths by remember { mutableIntStateOf(12) }
    var extendedWarranty by remember { mutableStateOf(false) }
    var alertsEnabled by remember { mutableStateOf(true) }

    // File Upload State
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileName = "Invoice_Document.pdf"
        }
    }

    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") }

    val expiryDateString = remember(durationMonths, extendedWarranty) {
        val calendar = Calendar.getInstance()
        val totalMonths = if (extendedWarranty) durationMonths + 12 else durationMonths
        calendar.add(Calendar.MONTH, totalMonths)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    val expiryDisplay = remember(expiryDateString) {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDateString)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(parsed!!)
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    fun handleSave() {
        if (deviceName.isBlank()) {
            Toast.makeText(context, "Device Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        isSaving = true

        // FIXED: Using centralized RetrofitClient
        RetrofitClient.instance.addWarrantyDevice(
            "Bearer $token",
            AddWarrantyReq(device_name = deviceName, brand = brand, expiry_date = expiryDateString)
        ).enqueue(object : Callback<AddWarrantyRes> {
            override fun onResponse(call: Call<AddWarrantyRes>, response: Response<AddWarrantyRes>) {
                isSaving = false
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Sent to Admin for Approval!", Toast.LENGTH_LONG).show()
                    onSave()
                } else {
                    Toast.makeText(context, "Failed to save: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<AddWarrantyRes>, t: Throwable) {
                isSaving = false
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                val backInteraction = remember { MutableInteractionSource() }
                val backPressed by backInteraction.collectIsPressedAsState()
                val backRotation by animateFloatAsState(if (backPressed) -15f else 0f, label = "rot")

                TopAppBar(
                    title = { Text("Add Warranty Device", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMainDark) },
                    navigationIcon = {
                        IconButton(onClick = onBack, interactionSource = backInteraction) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextMainDark, modifier = Modifier.rotate(backRotation))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg),
                    modifier = Modifier.shadow(2.dp)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()) {
                val btnInteraction = remember { MutableInteractionSource() }
                val btnPressed by btnInteraction.collectIsPressedAsState()
                val btnScale by animateFloatAsState(if (btnPressed) 0.96f else 1f, label = "scale")
                val btnElev by animateDpAsState(if (btnPressed) 2.dp else 8.dp, label = "elev")

                Box(modifier = Modifier.fillMaxWidth().background(CardBg).padding(16.dp)) {
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier.fillMaxWidth().height(55.dp).scale(btnScale).shadow(btnElev, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        interactionSource = btnInteraction
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Save Warranty", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = AppBg
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(400))) {
                Column {
                    AddSecHeader("PRODUCT INFO")
                    AddElevatedCard {
                        AddLabelledInput("Device Name", "e.g. Work Laptop", deviceName) { deviceName = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        AddLabelledDropdown("Brand/Type", brand, listOf("Apple", "Smartphone", "Laptop", "Headphones")) { brand = it }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(600))) {
                Column {
                    AddSecHeader("PURCHASE DETAILS")
                    AddElevatedCard {
                        AddLabelledInput("Purchase Date", "Today", "Today", isReadOnly = true)
                        Spacer(modifier = Modifier.height(16.dp))

                        AnimatedUploadBox(
                            fileName = selectedFileName,
                            onClick = { filePickerLauncher.launch("*/*") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 150 }) + fadeIn(tween(800))) {
                Column {
                    AddSecHeader("WARRANTY DETAILS")
                    AddElevatedCard {
                        AddLabelledDropdown("Duration", "$durationMonths Months", listOf("6 Months", "12 Months", "24 Months")) {
                            durationMonths = it.split(" ")[0].toInt()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Extended Warranty (+1 Yr)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextMainDark)
                                Text("Enable if purchased extra cover", fontSize = 11.sp, color = SectionGray)
                            }
                            Switch(
                                checked = extendedWarranty, onCheckedChange = { extendedWarranty = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = BluePrimary, uncheckedTrackColor = BorderLight, uncheckedBorderColor = Color.Transparent)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AddLabelledInput("Auto-calculated Expiry", expiryDisplay, expiryDisplay, isReadOnly = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = isVisible, enter = scaleIn(tween(1000)) + fadeIn()) {
                Column {
                    AddSecHeader("REMINDER SETTINGS")
                    AddElevatedCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable Expiry Alerts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextMainDark)
                            Switch(
                                checked = alertsEnabled, onCheckedChange = { alertsEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = BluePrimary, uncheckedTrackColor = BorderLight, uncheckedBorderColor = Color.Transparent)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun AddSecHeader(text: String) {
    Text(text, color = SectionGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
}

@Composable
private fun AddElevatedCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun AddLabelledInput(label: String, placeholder: String, value: String, isReadOnly: Boolean = false, onValueChange: (String) -> Unit = {}) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMainDark)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, readOnly = isReadOnly,
            placeholder = { Text(placeholder, color = TextHintGray, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderLight, focusedBorderColor = BluePrimary,
                unfocusedContainerColor = if (isReadOnly) AppBg else CardBg, focusedContainerColor = if (isReadOnly) AppBg else CardBg
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLabelledDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMainDark)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected, onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderLight, focusedBorderColor = BluePrimary, focusedContainerColor = CardBg, unfocusedContainerColor = CardBg)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardBg)) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun AnimatedUploadBox(fileName: String?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    val isSelected = fileName != null
    val borderColor by animateColorAsState(if (isPressed || isSelected) BluePrimary else Color(0xFFCFD8DC), label = "border")
    val boxBg by animateColorAsState(if (isSelected) Color(0xFFE3F2FD) else AppBg, label = "bg")

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).scale(scale).clip(RoundedCornerShape(12.dp))
            .background(boxBg).clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val pathEffect = if (isSelected) null else PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = if (isSelected) 4f else 3f, pathEffect = pathEffect),
                cornerRadius = CornerRadius(12.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Success", tint = SecureGreen, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(fileName!!, color = BluePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Description, "Upload", tint = if(isPressed) BluePrimary else SectionGray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Upload Invoice", color = if(isPressed) BluePrimary else TextMainDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("PDF, JPG or PNG (Max 5MB)", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}