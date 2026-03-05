package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.smartelectroai.api.AddressListResponse
import com.simats.smartelectroai.api.AddressModel
import com.simats.smartelectroai.api.BaseResponse
import com.simats.smartelectroai.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- Thematic Colors ---
private val AiBlue = Color(0xFF2962FF)
private val AiLightBlue = Color(0xFF03A9F4)
private val AiGradient = Brush.linearGradient(listOf(AiBlue, AiLightBlue))
private val GlassBg = Color.White.copy(alpha = 0.85f)
private val BgLightBlue = Color(0xFFF4F8FF)
private val TextMain = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var isTopVisible by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<AddressModel?>(null) }
    var addressToDelete by remember { mutableStateOf<AddressModel?>(null) }

    var addresses by remember { mutableStateOf<List<AddressModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAddressId by remember { mutableIntStateOf(-1) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isTopVisible = true
        isLoading = true
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("jwt_token", "")

        if (!token.isNullOrEmpty()) {
            RetrofitClient.instance.getAddresses("Bearer $token").enqueue(object : Callback<AddressListResponse> {
                override fun onResponse(call: Call<AddressListResponse>, response: Response<AddressListResponse>) {
                    isLoading = false
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val fetchedAddresses = response.body()?.addresses ?: emptyList()
                        addresses = fetchedAddresses
                        val defaultAddr = fetchedAddresses.find { it.is_default } ?: fetchedAddresses.firstOrNull()
                        if (defaultAddr != null) selectedAddressId = defaultAddr.id ?: -1
                    }
                }
                override fun onFailure(call: Call<AddressListResponse>, t: Throwable) { isLoading = false }
            })
        } else isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Delivery Locations", fontWeight = FontWeight.Bold, color = TextMain) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLightBlue)
            )
        },
        floatingActionButton = {
            PrivateAnimatedFab(isFormOpen = showAddForm) {
                showAddForm = !showAddForm
                if (showAddForm) addressToEdit = null // Ensure form is blank if adding new
            }
        },
        bottomBar = { PrivateFloatingAiDock(onNavigate) },
        containerColor = BgLightBlue
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {

            AnimatedVisibility(visible = isTopVisible && !showAddForm, enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()) {
                Text("Optimized for fastest delivery", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            }

            // ADD / EDIT FORM
            AnimatedVisibility(visible = showAddForm, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                PrivateAddressEntryForm(
                    initialAddress = addressToEdit,
                    onSaved = { modifiedAddress ->
                        if (addressToEdit == null) {
                            privateSaveAddressToBackend(context, modifiedAddress) {
                                showAddForm = false
                                refreshTrigger++
                            }
                        } else {
                            privateUpdateAddressInBackend(context, addressToEdit!!.id!!, modifiedAddress) {
                                showAddForm = false
                                refreshTrigger++
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LIST OF ADDRESSES
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AiBlue) }
            } else if (addresses.isEmpty() && !showAddForm) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No saved locations found", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!showAddForm) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(addresses) { address ->
                        PrivateAnimatedAddressCard(
                            address = address,
                            isSelected = selectedAddressId == address.id,
                            onSelect = { selectedAddressId = address.id ?: -1 },
                            onEdit = {
                                addressToEdit = address
                                showAddForm = true
                            },
                            onDelete = { addressToDelete = address }
                        )
                    }
                }
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    if (addressToDelete != null) {
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("Delete Location", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this AI delivery location?") },
            confirmButton = {
                TextButton(onClick = {
                    privateDeleteAddressFromBackend(context, addressToDelete!!.id!!) {
                        addressToDelete = null
                        refreshTrigger++ // Refresh list
                    }
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }
}

// ==========================================
// PRIVATE HELPER FUNCTIONS
// ==========================================

@Composable
private fun PrivateAddressEntryForm(initialAddress: AddressModel?, onSaved: (AddressModel) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addrLine by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Pre-fill the form if we are editing
    LaunchedEffect(initialAddress) {
        name = initialAddress?.full_name ?: ""
        phone = initialAddress?.mobile ?: ""
        pin = initialAddress?.pincode ?: ""
        city = initialAddress?.city ?: ""
        addrLine = initialAddress?.address_line ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), spotColor = AiBlue),
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(if (initialAddress == null) "Add New Location" else "Update Location", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = AiBlue)
            Spacer(modifier = Modifier.height(16.dp))

            PrivateAiFormTextField(value = name, onValueChange = {name = it}, label = "Full Name", icon = Icons.Default.Person)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { PrivateAiFormTextField(value = phone, onValueChange = {phone = it}, label = "Mobile", icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone) }
                Box(Modifier.weight(1f)) { PrivateAiFormTextField(value = pin, onValueChange = {pin = it}, label = "Pincode", icon = Icons.Default.LocationOn, keyboardType = KeyboardType.Number) }
            }
            PrivateAiFormTextField(value = city, onValueChange = {city = it}, label = "City", icon = Icons.Default.Home)
            PrivateAiFormTextField(value = addrLine, onValueChange = {addrLine = it}, label = "House No / Building / Area", icon = Icons.Default.Map)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && pin.isNotBlank() && city.isNotBlank() && addrLine.isNotBlank()) {
                        onSaved(AddressModel(initialAddress?.id, name, phone, pin, city, addrLine, initialAddress?.is_default ?: false))
                    } else {
                        Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AiBlue)
            ) {
                Text(if (initialAddress == null) "Save AI Verified Address" else "Update Address", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PrivateAiFormTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = AiLightBlue, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AiLightBlue, focusedLabelColor = AiLightBlue, unfocusedContainerColor = BgLightBlue),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = true
    )
}

private fun privateSaveAddressToBackend(context: Context, address: AddressModel, onSuccess: () -> Unit) {
    val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
    if (token.isNullOrEmpty()) return

    RetrofitClient.instance.addAddress("Bearer $token", address).enqueue(object : Callback<BaseResponse> {
        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
            if (response.isSuccessful && response.body()?.status == "success") {
                Toast.makeText(context, "Location Saved!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
        override fun onFailure(call: Call<BaseResponse>, t: Throwable) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
    })
}

private fun privateUpdateAddressInBackend(context: Context, addressId: Int, address: AddressModel, onSuccess: () -> Unit) {
    val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
    if (token.isNullOrEmpty()) return

    RetrofitClient.instance.updateAddress("Bearer $token", addressId, address).enqueue(object : Callback<BaseResponse> {
        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
            if (response.isSuccessful && response.body()?.status == "success") {
                Toast.makeText(context, "Location Updated!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
        override fun onFailure(call: Call<BaseResponse>, t: Throwable) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
    })
}

private fun privateDeleteAddressFromBackend(context: Context, addressId: Int, onSuccess: () -> Unit) {
    val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "")
    if (token.isNullOrEmpty()) return

    RetrofitClient.instance.deleteAddress("Bearer $token", addressId).enqueue(object : Callback<BaseResponse> {
        override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
            if (response.isSuccessful && response.body()?.status == "success") {
                Toast.makeText(context, "Location Deleted", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
        override fun onFailure(call: Call<BaseResponse>, t: Throwable) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
    })
}

@Composable
private fun PrivateAnimatedAddressCard(address: AddressModel, isSelected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val bgColor by animateColorAsState(if (isSelected) Color(0xFFE1F5FE) else GlassBg, label = "bg")
    val elevation by animateDpAsState(if (isSelected) 8.dp else 2.dp, label = "elev")
    val cardScale by animateFloatAsState(if (isSelected) 1.03f else 1f, label = "scale")

    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.9f) + fadeIn()) {
        Card(
            modifier = Modifier.fillMaxWidth().scale(cardScale).clickable { onSelect() }
                .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) AiBlue else Color(0xFFEEEEEE), shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(elevation)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (isSelected || address.is_default) {
                    PrivatePulsingDefaultBadge()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(address.full_name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextMain)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(address.mobile, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${address.address_line}\n${address.city} - ${address.pincode}", fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrivateAnimatedActionIcon(Icons.Outlined.Edit, "Edit", onClick = onEdit)
                        PrivateAnimatedActionIcon(Icons.Outlined.Delete, "Delete", Color.Red.copy(alpha = 0.7f), onClick = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivatePulsingDefaultBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.background(AiLightBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Primary AI Delivery Location", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = AiBlue.copy(alpha = alphaAnim))
        }
    }
}

@Composable
private fun PrivateAnimatedActionIcon(icon: ImageVector, desc: String, tint: Color = Color.Gray, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "iconScale")

    Box(
        modifier = Modifier.size(36.dp).scale(scale).background(Color.White, CircleShape).border(1.dp, Color(0xFFEEEEEE), CircleShape)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PrivateAnimatedFab(isFormOpen: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "fabScale")

    val bgColor = if (isFormOpen) Color(0xFFFFEBEE) else AiBlue
    val contentColor = if (isFormOpen) Color.Red else Color.White

    Box(
        modifier = Modifier
            .padding(bottom = 60.dp)
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(50), spotColor = if (isFormOpen) Color.Red else AiBlue)
            .background(if (isFormOpen) Brush.linearGradient(listOf(bgColor, bgColor)) else AiGradient, RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isFormOpen) Icons.Default.Close else Icons.Default.Add, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isFormOpen) "Cancel" else "Add Location", fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
private fun PrivateFloatingAiDock(onNavigate: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(30.dp), spotColor = AiBlue),
            shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = GlassBg)
        ) {
            Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Dashboard") })
                Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("Compare") })
                Box(modifier = Modifier.size(56.dp).background(AiGradient, CircleShape).shadow(8.dp, CircleShape).clickable { onNavigate("Chat") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(28.dp).clickable { onNavigate("MyWarranty") })
                Icon(Icons.Default.Person, null, tint = AiBlue, modifier = Modifier.size(28.dp).clickable { onNavigate("Profile") })
            }
        }
    }
}