package com.simats.smartelectroai.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.simats.smartelectroai.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

// --- THEME VARIABLES ---
private val AiBlue = Color(0xFF2962FF)
private val AiLightBlue = Color(0xFF03A9F4)
private val AiGradient = Brush.linearGradient(listOf(AiBlue, AiLightBlue))
private val BgWhite = Color(0xFFFFFFFF)
private val GrayText = Color(0xFF757575)
private val DarkText = Color(0xFF1E1E2C)
private val AppBg = Color(0xFFF8FAFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(onBack: () -> Unit = {}, onPayment: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen States
    var isVisible by remember { mutableStateOf(false) }
    var addresses by remember { mutableStateOf<List<AddressModel>>(emptyList()) }
    var selectedAddressId by remember { mutableIntStateOf(-1) }
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Form States
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addrLine by remember { mutableStateOf("") }
    var isFetchingLocation by remember { mutableStateOf(false) }

    // Fused Location Client
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // GPS Permission Launcher & Logic
    @SuppressLint("MissingPermission")
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isFetchingLocation = true
            showForm = true // Auto-open form to show fetched details

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        coroutineScope.launch {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addressesList = withContext(Dispatchers.IO) {
                                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                }
                                if (!addressesList.isNullOrEmpty()) {
                                    val geoAddr = addressesList[0]
                                    pin = geoAddr.postalCode ?: pin
                                    city = geoAddr.locality ?: geoAddr.subAdminArea ?: city
                                    val street = geoAddr.thoroughfare ?: geoAddr.subLocality ?: ""
                                    addrLine = if (street.isNotEmpty()) "$street, ${geoAddr.featureName ?: ""}".trim(',', ' ') else addrLine
                                    Toast.makeText(context, "Location autofilled!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not fetch address details.", Toast.LENGTH_SHORT).show()
                            }
                            isFetchingLocation = false
                        }
                    } else {
                        isFetchingLocation = false
                        Toast.makeText(context, "Please turn on device GPS", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    isFetchingLocation = false
                    Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerLocationFetch = {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Already have permission, just launch it again to trigger the success logic
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // Request permission
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        fetchAddresses(context) { fetchedAddresses ->
            addresses = fetchedAddresses
            isLoading = false
            if (fetchedAddresses.isNotEmpty()) {
                selectedAddressId = fetchedAddresses.first().id ?: -1
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { -50 }) + fadeIn()) {
                TopAppBar(
                    title = { Text("Select Delivery Address", fontWeight = FontWeight.Bold, color = DarkText, fontSize = 18.sp) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DarkText) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgWhite),
                    modifier = Modifier.shadow(elevation = 4.dp)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()) {
                StickyPaymentBar(enabled = selectedAddressId != -1, onContinue = onPayment)
            }
        },
        containerColor = AppBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DELIVER TO HEADER
            item {
                AnimatedVisibility(visible = isVisible, enter = fadeIn()) {
                    DeliverToHeader(addresses.find { it.id == selectedAddressId })
                }
            }

            // 2. TOP GPS LOCATION BUTTON
            item {
                AnimatedVisibility(visible = isVisible, enter = scaleIn() + fadeIn()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { triggerLocationFetch() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        border = BorderStroke(1.dp, AiLightBlue.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, "Location", tint = AiBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use my current location", fontWeight = FontWeight.Bold, color = AiBlue, fontSize = 15.sp)
                                Text("Auto-fill your exact address", fontSize = 12.sp, color = AiBlue.copy(alpha = 0.8f))
                            }
                            if (isFetchingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AiBlue, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ChevronRight, null, tint = AiBlue)
                            }
                        }
                    }
                }
            }

            // 3. SAVED ADDRESS LIST CARDS
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AiBlue)
                    }
                }
            } else {
                items(addresses) { addr ->
                    AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()) {
                        FlipkartAddressCard(
                            address = addr,
                            isSelected = selectedAddressId == addr.id,
                            onSelect = { selectedAddressId = addr.id ?: -1 }
                        )
                    }
                }
            }

            // 4. ADD NEW ADDRESS BUTTON & EXPANDABLE FORM
            item {
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgWhite),
                        border = BorderStroke(1.dp, if (showForm) AiBlue else Color(0xFFE0E0E0)),
                        elevation = CardDefaults.cardElevation(if (showForm) 4.dp else 0.dp)
                    ) {
                        Column {
                            // Add New Address Header
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showForm = !showForm }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if(showForm) Icons.Default.Remove else Icons.Default.Add, null, tint = AiBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Add a new address manually", color = AiBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            }

                            // Redesigned Address Form Section
                            AnimatedVisibility(visible = showForm, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                                AddressFormComposable(
                                    name = name, onNameChange = { name = it },
                                    phone = phone, onPhoneChange = { phone = it },
                                    pin = pin, onPinChange = { pin = it },
                                    city = city, onCityChange = { city = it },
                                    addrLine = addrLine, onAddrLineChange = { addrLine = it },
                                    isFetchingLocation = isFetchingLocation,
                                    onMapClick = { triggerLocationFetch() },
                                    onSave = {
                                        if (name.isNotBlank() && phone.isNotBlank() && pin.isNotBlank() && city.isNotBlank() && addrLine.isNotBlank()) {
                                            val newAddr = AddressModel(null, name, phone, pin, city, addrLine)
                                            saveAddressToBackend(context, newAddr) {
                                                showForm = false
                                                name = ""; phone = ""; pin = ""; city = ""; addrLine = ""
                                                fetchAddresses(context) { updatedList ->
                                                    addresses = updatedList
                                                    if (updatedList.isNotEmpty()) selectedAddressId = updatedList.last().id ?: -1
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please fill all details", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. DELIVERY SAFETY INFO
            item {
                AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(1000))) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), border = BorderStroke(1.dp, Color(0xFFC8E6C9))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Safe & Contactless Delivery", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 14.sp)
                                Text("Your location is verified for secure handling.", fontSize = 12.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

// ==========================================
// UI COMPONENTS
// ==========================================

@Composable
fun DeliverToHeader(selectedAddress: AddressModel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deliver to:", color = GrayText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                if (selectedAddress != null) {
                    Text(selectedAddress.full_name, fontWeight = FontWeight.Bold, color = DarkText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(selectedAddress.pincode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkText)
                    }
                }
            }
            if (selectedAddress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${selectedAddress.address_line}, ${selectedAddress.city}", color = GrayText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please select or add an address below.", color = AiBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FlipkartAddressCard(address: AddressModel, isSelected: Boolean, onSelect: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val elevation by animateDpAsState(if (isSelected) 6.dp else 1.dp, label = "")
    val borderColor by animateColorAsState(if (isSelected) AiBlue else Color(0xFFE0E0E0), label = "")
    val bgColor by animateColorAsState(if (isSelected) Color(0xFFF0F8FF) else BgWhite, label = "")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if(isSelected) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(elevation)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            RadioButton(
                selected = isSelected, onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = AiBlue, unselectedColor = Color.LightGray),
                modifier = Modifier.padding(top = 2.dp).size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(address.full_name, fontWeight = FontWeight.Bold, color = DarkText, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("HOME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GrayText)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("${address.address_line}, ${address.city} - ${address.pincode}", fontSize = 13.sp, color = GrayText, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(address.mobile, fontSize = 13.sp, color = DarkText, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// REDESIGNED FORM: Clean vertical layout, prevents squished text
@Composable
fun AddressFormComposable(
    name: String, onNameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    pin: String, onPinChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    addrLine: String, onAddrLineChange: (String) -> Unit,
    isFetchingLocation: Boolean,
    onMapClick: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 16.dp)) {

        // Location Button inside form
        OutlinedButton(
            onClick = onMapClick, modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, AiBlue),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AiBlue, containerColor = Color(0xFFF0F8FF))
        ) {
            if (isFetchingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AiBlue, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Locating...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use my current location", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stacked inputs for cleaner UI
        AiTextField(name, onNameChange, "Full Name", Icons.Default.Person)
        AiTextField(phone, onPhoneChange, "Phone Number", Icons.Default.Phone, KeyboardType.Phone)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { AiTextField(pin, onPinChange, "Pincode", Icons.Default.PinDrop, KeyboardType.Number) }
            Box(Modifier.weight(1f)) { AiTextField(city, onCityChange, "City", Icons.Default.LocationCity) }
        }

        AiTextField(addrLine, onAddrLineChange, "House No, Building, Street, Area", Icons.Default.Home)

        Button(
            onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = AiBlue)
        ) {
            Text("Save Address", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun AiTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = GrayText, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AiBlue, unfocusedBorderColor = Color(0xFFE0E0E0)),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun StickyPaymentBar(enabled: Boolean, onContinue: () -> Unit) {
    val elevation by animateDpAsState(if (enabled) 16.dp else 4.dp, label = "")
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.97f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")

    Surface(shadowElevation = elevation, color = BgWhite, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Ready to checkout?", fontSize = 12.sp, color = GrayText)
                Text("Select Address", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkText)
            }
            Box(
                modifier = Modifier
                    .width(160.dp).height(50.dp).scale(scale).clip(RoundedCornerShape(8.dp))
                    .background(if (enabled) AiGradient else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))))
                    .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onContinue() },
                contentAlignment = Alignment.Center
            ) {
                Text("Deliver Here", color = if (enabled) BgWhite else GrayText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// --- DB LOGIC ---
fun fetchAddresses(context: Context, onResult: (List<AddressModel>) -> Unit) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val savedToken = sharedPreferences.getString("jwt_token", "")

    if (savedToken.isNullOrEmpty()) {
        onResult(emptyList())
        return
    }

    RetrofitClient.instance.getAddresses("Bearer $savedToken").enqueue(object : Callback<AddressListResponse> {
        override fun onResponse(call: Call<AddressListResponse>, response: Response<AddressListResponse>) {
            if (response.isSuccessful && response.body()?.status == "success") {
                onResult(response.body()?.addresses ?: emptyList())
            } else {
                // If it fails, show a toast with the error code so we aren't guessing!
                Toast.makeText(context, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
            }
        }
        override fun onFailure(call: Call<AddressListResponse>, t: Throwable) {
            // If Android crashes trying to read the JSON, show the actual crash error
            Toast.makeText(context, "Network/Parsing Error: ${t.message}", Toast.LENGTH_LONG).show()
            onResult(emptyList())
        }
    })
}

fun saveAddressToBackend(context: Context, address: AddressModel, onFinished: () -> Unit) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val savedToken = sharedPreferences.getString("jwt_token", "")

    if (savedToken.isNullOrEmpty()) {
        Toast.makeText(context, "Cannot save: You are not logged in!", Toast.LENGTH_LONG).show()
        onFinished()
        return
    }

    RetrofitClient.instance.addAddress("Bearer $savedToken", address).enqueue(object : Callback<BaseResponse> {
        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
            if (response.isSuccessful && response.body()?.status == "success") {
                Toast.makeText(context, "Address successfully saved!", Toast.LENGTH_SHORT).show()
                onFinished()
            } else {
                Toast.makeText(context, "Server Error: Token Rejected.", Toast.LENGTH_LONG).show()
                onFinished()
            }
        }

        override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
            Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            onFinished()
        }
    })
}