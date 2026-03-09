package com.simats.smartelectroai.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke // 🚀 FIXED: Added BorderStroke import
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.simats.smartelectroai.api.ApiConfig

// ==========================================
// 1. ISOLATED API MODELS
// ==========================================
internal data class UniqueAdminUser(
    val id: Int = 0,
    val full_name: String? = null,
    val email: String? = null,
    val reg_date: String? = null,
    var is_blocked: Boolean = false,
    val total_orders: Int = 0,
    val total_warranties: Int = 0
)

internal data class UniqueUserMgmtResponse(
    val status: String? = null,
    val total_users: Int = 0,
    val users: List<UniqueAdminUser>? = null
)

internal data class UniqueToggleBlockResponse(
    val status: String? = null,
    val message: String? = null,
    val is_blocked: Boolean = false
)

internal data class UniqueForgotPasswordRequest(val email: String)
internal data class UniqueSimpleResponse(val status: String?, val message: String?)

internal interface UniqueUserMgmtApi {
    @GET("/admin/users")
    fun getAllUsers(@Header("Authorization") token: String): Call<UniqueUserMgmtResponse>

    @PUT("/admin/users/{id}/toggle_block")
    fun toggleUserBlock(@Header("Authorization") token: String, @Path("id") userId: Int): Call<UniqueToggleBlockResponse>
    @POST("/admin/users/send_reset_link")
    fun sendResetLink(
        @Header("Authorization") token: String,
        @Body request: UniqueForgotPasswordRequest
    ): Call<UniqueSimpleResponse>
    @POST("/forgot_password")
    fun sendResetLink(@Body request: UniqueForgotPasswordRequest): Call<UniqueSimpleResponse>
}

// ==========================================
// 2. COLORS & THEMING
// ==========================================
private val BlueMain = Color(0xFF1976D2)
private val LightBlueBg = Color(0xFFE3F2FD)
private val TextGray = Color(0xFF757575)
private val LightGrayBg = Color(0xFFF5F7FA)
private val ActiveGreen = Color(0xFF4CAF50)
private val ActiveGreenBg = Color(0xFFE8F5E9)
private val BlockedRed = Color(0xFFD32F2F)
private val BlockedRedBg = Color(0xFFFFEBEE)

// ==========================================
// 3. MAIN UI COMPOSABLE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val token = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: "" }

    val api = remember {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UniqueUserMgmtApi::class.java)
    }

    var isLoading by remember { mutableStateOf(true) }
    var usersList by remember { mutableStateOf<List<UniqueAdminUser>>(emptyList()) }
    var totalUsers by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val visibleState = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        api.getAllUsers("Bearer $token").enqueue(object : Callback<UniqueUserMgmtResponse> {
            override fun onResponse(call: Call<UniqueUserMgmtResponse>, response: Response<UniqueUserMgmtResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    usersList = response.body()?.users ?: emptyList()
                    totalUsers = response.body()?.total_users ?: 0
                } else {
                    Toast.makeText(context, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
                visibleState.targetState = true
            }
            override fun onFailure(call: Call<UniqueUserMgmtResponse>, t: Throwable) {
                isLoading = false
                visibleState.targetState = true
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    Scaffold(
        topBar = { UserManagementTopBar(onNavigate) },
        bottomBar = { AdminBottomNavBar("AdminUsers", onNavigate) },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            AnimatedVisibility(visibleState = visibleState, enter = fadeIn() + slideInVertically { -it / 2 }) {
                SearchBarInput(searchQuery) { searchQuery = it }
            }

            AnimatedVisibility(visibleState = visibleState, enter = fadeIn() + slideInVertically { -it / 4 }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("REGISTERED USERS ($totalUsers)", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Sort by: Date", color = BlueMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BlueMain)
                }
            } else {
                val filteredUsers = usersList.filter { user ->
                    val nameMatch = user.full_name?.contains(searchQuery, ignoreCase = true) == true
                    val emailMatch = user.email?.contains(searchQuery, ignoreCase = true) == true
                    nameMatch || emailMatch
                }

                if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found.", color = TextGray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(filteredUsers, key = { _, user -> user.id }) { index, user ->
                            AnimatedVisibility(
                                visible = visibleState.targetState,
                                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300 + index * 50)) + fadeIn()
                            ) {
                                UserManagementCard(user, api, token, context, onNavigate)
                            }
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserManagementCard(
    user: UniqueAdminUser,
    api: UniqueUserMgmtApi,
    token: String,
    context: Context,
    onNavigate: (String) -> Unit
) {
    var isBlocked by remember { mutableStateOf(user.is_blocked) }
    var isProcessingBlock by remember { mutableStateOf(false) }
    var isSendingReset by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(if (isBlocked) BlockedRed else ActiveGreen, label = "color")
    val statusBgColor by animateColorAsState(if (isBlocked) BlockedRedBg else ActiveGreenBg, label = "bgcolor")

    val safeName = user.full_name ?: "Unknown User"
    val safeEmail = user.email ?: "No Email"
    val safeDate = user.reg_date ?: "N/A"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LightBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = safeName.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
                    Text(initials.ifEmpty { "U" }, color = BlueMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(safeName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusBgColor)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBlocked) "BLOCKED" else "ACTIVE",
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text("ID: USR-92${user.id} • Reg: $safeDate", color = TextGray, fontSize = 12.sp)
                    Text(safeEmail, color = TextGray, fontSize = 12.sp)
                }

                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextGray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(Icons.Default.ShoppingCart, "${user.total_orders} Orders")
                StatChip(Icons.Default.VerifiedUser, "${user.total_warranties} Warranties")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedButton(
                    onClick = {
                        if (isSendingReset) return@OutlinedButton

                        if (user.email.isNullOrEmpty()) {
                            Toast.makeText(context, "User has no email address", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }

                        isSendingReset = true
                        val req = UniqueForgotPasswordRequest(user.email)
                        api.sendResetLink("Bearer $token", req).enqueue(object : Callback<UniqueSimpleResponse> {
                            override fun onResponse(call: Call<UniqueSimpleResponse>, response: Response<UniqueSimpleResponse>) {
                                isSendingReset = false
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "Recovery email sent to ${user.email}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to send reset link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onFailure(call: Call<UniqueSimpleResponse>, t: Throwable) {
                                isSendingReset = false
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueMain),
                    border = BorderStroke(1.dp, BlueMain)
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlueMain, strokeWidth = 2.dp)
                    } else {
                        Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                val actionContainer = if (isBlocked) BlockedRed else Color.White
                val actionContent = if (isBlocked) Color.White else BlockedRed

                Button(
                    onClick = {
                        if (isProcessingBlock) return@Button
                        isProcessingBlock = true
                        api.toggleUserBlock("Bearer $token", user.id).enqueue(object : Callback<UniqueToggleBlockResponse> {
                            override fun onResponse(call: Call<UniqueToggleBlockResponse>, response: Response<UniqueToggleBlockResponse>) {
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    isBlocked = response.body()?.is_blocked == true
                                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                                }
                                isProcessingBlock = false
                            }
                            override fun onFailure(call: Call<UniqueToggleBlockResponse>, t: Throwable) {
                                isProcessingBlock = false
                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionContainer, contentColor = actionContent),
                    border = if (!isBlocked) BorderStroke(1.dp, BlockedRed) else null
                ) {
                    if (isProcessingBlock) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = actionContent, strokeWidth = 2.dp)
                    } else {
                        Text(if (isBlocked) "Unblock User" else "Block User", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, animationSpec = tween(100), label = "")

            Button(
                onClick = {
                    pressed = true
                    onNavigate("AdminViewProfile/${user.id}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .scale(scale),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightGrayBg, contentColor = Color(0xFF424242))
            ) {
                Text("View Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            LaunchedEffect(pressed) {
                if (pressed) {
                    delay(100)
                    pressed = false
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .background(LightGrayBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = Color(0xFF424242), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchBarInput(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search by name, ID or email...", color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextGray) },
        trailingIcon = {
            IconButton(onClick = { /* Implement Filters */ }) {
                Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.Black)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = LightGrayBg,
            unfocusedContainerColor = LightGrayBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserManagementTopBar(onNavigate: (String) -> Unit) {
    TopAppBar(
        title = { Text("User Management", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = { onNavigate("AdminDashboard") }) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.Black)
            }
        },
        actions = {
            IconButton(onClick = { /* Add User Logic */ }) {
                Icon(Icons.Default.PersonAddAlt1, contentDescription = "Add User", tint = BlueMain)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}