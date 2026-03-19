package com.simats.smartelectroai.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.AiRequest
import com.simats.smartelectroai.api.AiResponse
import com.simats.smartelectroai.api.RecommendationManager
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiAssistantScreen(
    onBack: () -> Unit = {},
    onGenerate: () -> Unit = {},
) {
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    var budget by remember { mutableFloatStateOf(30000f) }
    var selectedUsage by remember { mutableStateOf("Gaming") }
    var selectedBrands by remember { mutableStateOf(setOf<String>()) }
    var selectedStorage by remember { mutableStateOf("128GB") }
    var batteryPreference by remember { mutableStateOf("Standard") }  // ✅ FIX 2: correct variable name
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text("Back", color = Color.Gray, fontSize = 16.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                isLoading = true

                                // ✅ FIX 1 & 3: Single brandString declaration, no space after comma
                                val brandString = if (selectedBrands.isEmpty() || selectedBrands.contains("Any")) {
                                    "Any"
                                } else {
                                    selectedBrands.joinToString(",") // ✅ "Samsung,Apple,OnePlus" — no space
                                }

                                val request = AiRequest(
                                    budget  = budget,
                                    usage   = selectedUsage,
                                    brand   = brandString,
                                    storage = selectedStorage,
                                    battery = batteryPreference, // ✅ FIX 2: was "selectedBattery" — now correct
                                    notes   = notes
                                )

                                RetrofitClient.instance.getRecommendations(request).enqueue(object : Callback<AiResponse> {
                                    override fun onResponse(call: Call<AiResponse>, response: Response<AiResponse>) {
                                        isLoading = false
                                        try {
                                            if (response.isSuccessful) {
                                                val body = response.body()
                                                if (body?.status == "success" && body.data != null) {
                                                    RecommendationManager.result = body.data
                                                    onGenerate()
                                                } else {
                                                    Toast.makeText(context, "API Error: ${body?.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                val errorString = response.errorBody()?.string() ?: "Unknown error"
                                                Toast.makeText(context, "Server Error ${response.code()}: $errorString", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "App Logic Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<AiResponse>, t: Throwable) {
                                        isLoading = false
                                        Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                                    }
                                })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Generate", modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = { (currentStep + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF1A73E8),
                trackColor = Color(0xFFE3F2FD),
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut())
                    }
                },
                label = "wizard_animation"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        0 -> StepOneBudget(budget) { budget = it }
                        1 -> StepTwoUsage(selectedUsage) { selectedUsage = it }
                        2 -> StepThreeBrand(
                            selectedBrands = selectedBrands,
                            onBrandToggle = { brand ->
                                val newSet = selectedBrands.toMutableSet()
                                if (brand == "Any") {
                                    newSet.clear()
                                } else {
                                    if (newSet.contains(brand)) newSet.remove(brand) else newSet.add(brand)
                                }
                                selectedBrands = newSet
                            }
                        )
                        3 -> StepFourDetails(
                            selectedStorage    = selectedStorage,
                            batteryPreference  = batteryPreference,
                            notes              = notes,
                            onStorageChange    = { selectedStorage = it },
                            onBatteryChange    = { batteryPreference = it },
                            onNotesChange      = { notes = it }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 1: BUDGET
// ==========================================
@Composable
fun StepOneBudget(currentBudget: Float, onBudgetSelect: (Float) -> Unit) {
    var customBudgetInput by remember {
        mutableStateOf(
            if (currentBudget > 0 && currentBudget !in listOf(15000f, 30000f, 60000f, 100000f))
                currentBudget.toInt().toString()
            else ""
        )
    }

    Text(
        "What is your budget range?",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Choose a category or enter a custom amount", color = Color.Gray, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(24.dp))

    val budgetOptions = listOf(
        BudgetOption("Budget Phone", "Under ₹15,000",         Icons.Outlined.AccountBalanceWallet, 15000f),
        BudgetOption("Mid-Range",    "₹15,000 – ₹30,000",    Icons.Outlined.Speed,                30000f),
        BudgetOption("Premium",      "₹30,000 – ₹60,000",    Icons.Outlined.StarBorder,           60000f),
        BudgetOption("Flagship",     "Above ₹60,000",         Icons.Outlined.WorkspacePremium,     100000f)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(280.dp)
    ) {
        items(budgetOptions) { option ->
            val isSelected = currentBudget == option.apiValue
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1A73E8) else Color(0xFFF8F9FA)
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF1A73E8) else Color(0xFFEEEEEE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clickable {
                        customBudgetInput = ""
                        onBudgetSelect(option.apiValue)
                    },
                elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = if (isSelected) Color.White else Color(0xFF1A73E8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = option.title,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.subtitle,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = customBudgetInput,
        onValueChange = {
            customBudgetInput = it
            val floatVal = it.toFloatOrNull()
            if (floatVal != null) {
                onBudgetSelect(floatVal)
            }
        },
        label = { Text("Or enter custom budget (₹)") },
        placeholder = { Text("e.g. 25000") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

data class BudgetOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val apiValue: Float
)

// ==========================================
// STEP 2: USAGE
// ==========================================
@Composable
fun StepTwoUsage(selectedUsage: String, onUsageSelect: (String) -> Unit) {
    val usages = listOf(
        Pair("Gaming",   Icons.Outlined.VideogameAsset),
        Pair("Camera",   Icons.Outlined.CameraAlt),
        Pair("Business", Icons.Outlined.WorkOutline),
        Pair("Media",    Icons.Outlined.Movie),
        Pair("Student",  Icons.Outlined.School),
        Pair("General",  Icons.Outlined.Smartphone)
    )

    Text(
        "How will you use this phone?",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Select your primary use case", color = Color.Gray, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(24.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(usages) { (name, icon) ->
            SelectionCard(
                title = name,
                icon = icon,
                isSelected = selectedUsage == name
            ) { onUsageSelect(name) }
        }
    }
}

// ==========================================
// STEP 3: BRAND (multi-select)
// ==========================================
@Composable
fun StepThreeBrand(selectedBrands: Set<String>, onBrandToggle: (String) -> Unit) {
    val brands = listOf(
        "Any", "Samsung", "Apple", "OnePlus",
        "Xiaomi", "Vivo", "Oppo", "Google",
        "Motorola", "Realme", "Poco", "iQOO"
    )

    Text(
        "Preferred Brands",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Select multiple brands, or choose 'Any'", color = Color.Gray, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(24.dp))

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(brands) { brand ->
            val isSelected = selectedBrands.contains(brand) ||
                    (brand == "Any" && selectedBrands.isEmpty())
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1A73E8) else Color(0xFFF5F5F5)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onBrandToggle(brand) },
                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brand,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 4: FINAL DETAILS
// ==========================================
@Composable
fun StepFourDetails(
    selectedStorage: String,
    batteryPreference: String,
    notes: String,
    onStorageChange: (String) -> Unit,
    onBatteryChange: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    Text(
        "Final Details",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text("Fine-tune your requirements", color = Color.Gray, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(32.dp))

    // --- Storage ---
    Text(
        "Minimum Storage",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("128GB", "256GB", "512GB+").forEach { storage ->
            val isSelected = selectedStorage == storage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) Color(0xFF1A73E8) else Color(0xFFF0F0F0))
                    .clickable { onStorageChange(storage) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = storage,
                    color = if (isSelected) Color.White else Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // --- Battery ---
    Text(
        "Battery Life",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val batteryOptions = listOf(
            Pair("Standard", Icons.Outlined.BatteryStd),
            Pair("Massive",  Icons.Outlined.BatteryChargingFull)
        )
        batteryOptions.forEach { (type, icon) ->
            val isSelected = batteryPreference == type
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF1A73E8) else Color.LightGray),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onBatteryChange(type) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = type,
                        tint = if (isSelected) Color(0xFF1A73E8) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = type,
                        color = if (isSelected) Color(0xFF1A73E8) else Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // --- Notes ---
    Text(
        "Specific Features (Optional)",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        placeholder = {
            Text(
                "E.g. Headphone jack, flat display, good for selfies...",
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.Gray)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor   = Color(0xFFFAFAFA),
            unfocusedContainerColor = Color(0xFFFAFAFA)
        )
    )
}

// ==========================================
// SHARED CARD COMPONENT
// ==========================================
@Composable
fun SelectionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1A73E8) else Color(0xFFF5F5F5)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.DarkGray
            )
        }
    }
}