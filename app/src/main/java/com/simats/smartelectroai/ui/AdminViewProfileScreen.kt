package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import com.simats.smartelectroai.api.ApiConfig // <-- IMPORT THE CENTROID

// ==========================================
// 1. ISOLATED API MODELS
// ==========================================
internal data class UniqueProfileUserData(
    val id: Int, val full_name: String?, val email: String?, val mobile: String?, val reg_date: String?, val is_blocked: Boolean
)
internal data class UniqueProfileOrder(
    val id: Int, val product_name: String?, val price: String?, val status: String?, val date: String?
)
internal data class UniqueProfileWarranty(
    val id: Int, val device_name: String?, val status: String?, val expiry_date: String?
)
internal data class UniqueProfileFullResponse(
    val status: String?, val user: UniqueProfileUserData?, val orders: List<UniqueProfileOrder>?, val warranties: List<UniqueProfileWarranty>?
)

internal interface UniqueAdminProfileApi {
    @GET("/admin/users/{id}")
    fun getUserProfileDetails(@Header("Authorization") token: String, @Path("id") userId: Int): Call<UniqueProfileFullResponse>
}

// COLORS
private val BlueMain = Color(0xFF1976D2)
private val LightGrayBg = Color(0xFFF5F7FA)
private val TextGray = Color(0xFF757575)

// ==========================================
// 2. MAIN COMPOSABLE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminViewProfileScreen(userId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    val api = remember {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL) // <-- USING THE CENTROID HERE
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UniqueAdminProfileApi::class.java)
    }

    var isLoading by remember { mutableStateOf(true) }
    var profileData by remember { mutableStateOf<UniqueProfileFullResponse?>(null) }
    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(userId) {
        api.getUserProfileDetails("Bearer $token", userId).enqueue(object : Callback<UniqueProfileFullResponse> {
            override fun onResponse(call: Call<UniqueProfileFullResponse>, response: Response<UniqueProfileFullResponse>) {
                if (response.isSuccessful) {
                    profileData = response.body()
                } else {
                    Toast.makeText(context, "Error fetching profile", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
                visibleState.targetState = true
            }
            override fun onFailure(call: Call<UniqueProfileFullResponse>, t: Throwable) {
                isLoading = false
                visibleState.targetState = true
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueMain)
            }
        } else {
            val user = profileData?.user
            val orders = profileData?.orders ?: emptyList()
            val warranties = profileData?.warranties ?: emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                item {
                    AnimatedVisibility(visibleState = visibleState, enter = slideInVertically { -it } + fadeIn()) {
                        AdminProfileHeader(user)
                    }
                }

                // Analytics / Quick Stats
                item {
                    AnimatedVisibility(visibleState = visibleState, enter = fadeIn(tween(500))) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileStatBox(Modifier.weight(1f), "${orders.size}", "Total Orders", Icons.Default.ShoppingBag)
                            ProfileStatBox(Modifier.weight(1f), "${warranties.size}", "Warranties", Icons.Default.VerifiedUser)
                        }
                    }
                }

                // Recent Orders Section
                item {
                    Text("Order History", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (orders.isEmpty()) {
                    item { Text("No orders placed yet.", color = TextGray, fontSize = 14.sp) }
                } else {
                    items(orders) { order -> AdminProfileOrderCard(order) }
                }

                // Warranties Section
                item {
                    Text("Registered Devices", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (warranties.isEmpty()) {
                    item { Text("No active warranties.", color = TextGray, fontSize = 14.sp) }
                } else {
                    items(warranties) { warranty -> AdminProfileWarrantyCard(warranty) }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun AdminProfileHeader(user: UniqueProfileUserData?) {
    val safeName = user?.full_name ?: "Unknown"
    val initials = safeName.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LightGrayBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials.ifEmpty { "U" }, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = BlueMain)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(safeName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(user?.email ?: "N/A", color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = BlueMain, modifier = Modifier.size(20.dp))
                    Text(user?.mobile ?: "No Phone", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top=4.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BlueMain, modifier = Modifier.size(20.dp))
                    Text("Joined ${user?.reg_date ?: "N/A"}", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top=4.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBox(modifier: Modifier, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = BlueMain)
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(label, color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdminProfileOrderCard(order: UniqueProfileOrder) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(order.product_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Date: ${order.date}", color = TextGray, fontSize = 12.sp)
                Text(order.status ?: "Pending", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(order.price ?: "₹0", fontWeight = FontWeight.ExtraBold, color = BlueMain)
        }
    }
}

@Composable
private fun AdminProfileWarrantyCard(warranty: UniqueProfileWarranty) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
        border = BorderStroke(1.dp, Color(0xFFE6EE9C)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(warranty.device_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Expires: ${warranty.expiry_date}", color = TextGray, fontSize = 12.sp)
            }
            Icon(Icons.Default.Verified, null, tint = Color(0xFF8BC34A))
        }
    }
}