package com.simats.smartelectroai

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.simats.smartelectroai.worker.WarrantyCheckWorker

import com.simats.smartelectroai.ui.*
import com.simats.smartelectroai.ui.theme.SmartElectroAITheme
import com.simats.smartelectroai.api.CartManager
import kotlinx.coroutines.delay

// --- RAZORPAY IMPORTS ---
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

// Global callback to pass Razorpay results to Compose Screens
object PaymentCallbackHandler {
    var onSuccess: ((String, String, String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
}

private val MainActivityBlue = Color(0xFF1976D2)

class MainActivity : FragmentActivity(), PaymentResultWithDataListener {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Checkout.preload(applicationContext)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WarrantyCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WarrantyCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            SmartElectroAITheme {
                // ==========================================
                // 🚀 NAVIGATION BACK STACK
                // ==========================================
                var backStack by remember { mutableStateOf(listOf("Splash")) }
                val currentScreen = backStack.lastOrNull() ?: "Splash"

                val navigateTo: (String) -> Unit = { screen ->
                    val rootScreens = setOf("Dashboard", "Login", "AdminDashboard", "Welcome")
                    if (screen in rootScreens) {
                        backStack = listOf(screen)
                    } else if (backStack.lastOrNull() != screen) {
                        backStack = backStack + screen
                    }
                }

                val navigateBack: () -> Unit = {
                    if (backStack.size > 1) {
                        backStack = backStack.dropLast(1)
                    } else {
                        finish()
                    }
                }

                BackHandler(enabled = backStack.size > 1) {
                    navigateBack()
                }

                val realCartCount by remember { derivedStateOf { CartManager.items.sumOf { it.quantity } } }
                var selectedWarrantyId by remember { mutableIntStateOf(0) }
                var compareDeviceNames by remember { mutableStateOf(emptyList<String>()) }
                var selectedAdminUserId by remember { mutableIntStateOf(0) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                        when (currentScreen) {
                            "Splash" -> {
                                SplashScreen(
                                    onVideoFinished = {
                                        navigateTo("Welcome")
                                    }
                                )
                            }

                            "Welcome" -> WelcomeScreen(
                                modifier = Modifier.padding(innerPadding),
                                onContinueClicked = { navigateTo("Onboarding") }
                            )

                            "Onboarding" -> OnboardingScreen(
                                modifier = Modifier.padding(innerPadding),
                                onOnboardingFinished = { navigateTo("Login") }
                            )

                            "Login" -> SignInScreen(
                                modifier = Modifier.padding(innerPadding),
                                onSignIn = { isAdmin ->
                                    if (isAdmin) navigateTo("AdminDashboard")
                                    else navigateTo("Dashboard")
                                },
                                onSignUp = { navigateTo("Register") },
                                onForgotPassword = { navigateTo("ForgotPassword") } // <--- ADD THIS
                            )

                            "Register" -> RegisterScreen(
                                modifier = Modifier.padding(innerPadding),
                                onRegister = { navigateTo("Dashboard") },
                                onLogin = { navigateBack() },
                                onTermsAndConditions = {},
                                onPrivacyPolicy = {}
                            )
                            "ForgotPassword" -> ForgotPasswordScreen(
                                onBack = { navigateBack() },
                                onResetSuccess = { navigateTo("Login") }
                            )
                            "TrackOrder" -> TrackOrderScreen(onBack = { navigateBack() }, onInvoiceClick = { navigateTo("OrderInvoice") })
                            "OrderInvoice" -> OrderInvoiceScreen(onBack = { navigateBack() })

                            "AdminDashboard" -> AdminDashboardScreen(onNavigate = { navigateTo(it) })
                            "AdminUsers" -> AdminUserManagementScreen(
                                onNavigate = { screen ->
                                    if(screen.startsWith("AdminViewProfile/")) {
                                        selectedAdminUserId = screen.substringAfter("/").toIntOrNull() ?: 0
                                        navigateTo("AdminViewProfileDetail")
                                    } else {
                                        navigateTo(screen)
                                    }
                                }
                            )
                            "AdminViewProfileDetail" -> AdminViewProfileScreen(userId = selectedAdminUserId, onBack = { navigateBack() })
                            "AdminOrderManagement" -> OrderManagementScreen(onNavigate = { navigateTo(it) })
                            "AdminAiSettings" -> AiSettingsScreen(onBack = { navigateBack() }, onNavigate = { navigateTo(it) })
                            "AdminWarranty" -> AdminWarrantyScreen(onBack = { navigateBack() }, onNavigate = { navigateTo(it) })

                            "Dashboard" -> DashboardScreen(
                                onAskAiAssistant = { navigateTo("AskAiAssistant") },
                                onNavigate = { screen ->
                                    val target = when (screen) {
                                        "Chat" -> "AskAiAssistant"
                                        "Warranty" -> "MyWarranty"
                                        "Profile" -> "Profile"
                                        "Compare" -> "Compare"
                                        else -> screen
                                    }
                                    navigateTo(target)
                                }
                            )

                            "AskAiAssistant" -> AskAiAssistantScreen(onBack = { navigateBack() }, onGenerate = { navigateTo("AiRecommendation") })
                            "AiRecommendation" -> AiRecommendationScreen(
                                onBack = { navigateBack() },
                                onProductClick = { navigateTo("ProductDetail") },
                                onNavigate = { screen -> navigateTo(screen) }
                            )

                            "ProductDetail" -> ProductDetailScreen(
                                onBack = { navigateBack() },
                                onAddToCart = { },
                                onBuyNow = { navigateTo("MyCart") }
                            )

                            "MyCart" -> MyCartScreen(onBack = { navigateBack() }, onCheckoutSuccess = { navigateTo("Address") })
                            "Address" -> AddressScreen(onBack = { navigateBack() }, onPayment = { navigateTo("Payment") })
                            "Payment" -> PaymentScreen(onBack = { navigateBack() }, onPaymentSuccess = { navigateTo("OrderSuccess") })

                            "OrderSuccess" -> OrderSuccessScreen(
                                onContinueShopping = {
                                    CartManager.clear()
                                    navigateTo("Dashboard")
                                }
                            )

                            "MyWarranty" -> MyWarrantyScreen(
                                onBack = { navigateBack() },
                                onNavigate = { screen ->
                                    if(screen.startsWith("WarrantyDetail/")) {
                                        selectedWarrantyId = screen.substringAfter("/").toIntOrNull() ?: 0
                                        navigateTo("WarrantyDetail")
                                    } else if (screen == "Chat") {
                                        navigateTo("AskAiAssistant") // Handle AI Chat from Warranty
                                    } else {
                                        navigateTo(screen)
                                    }
                                }
                            )

                            "AddWarranty" -> AddWarrantyScreen(onBack = { navigateBack() }, onSave = { navigateBack() })
                            "WarrantyDetail" -> WarrantyDetailsScreen(warrantyId = selectedWarrantyId, onBack = { navigateBack() }, onNavigate = { screen -> navigateTo(screen) })

                            "ClaimWarranty" -> ClaimWarrantyScreen(
                                warrantyId = selectedWarrantyId,
                                onBack = { navigateBack() },
                                onNavigate = { navigateTo(it) }
                            )
                            "ExtendWarranty" -> ExtendedWarrantyScreen(
                                warrantyId = selectedWarrantyId,
                                onBack = { navigateBack() },
                                onNavigate = { navigateTo(it) }
                            )

                            "Invoice" -> InvoiceScreen(onBack = { navigateBack() })
                            "WarrantyAlerts" -> WarrantyAlertsScreen(
                                onBack = { navigateBack() },
                                onNavigate = { screen ->
                                    if(screen.startsWith("WarrantyDetail/")) {
                                        selectedWarrantyId = screen.substringAfter("/").toIntOrNull() ?: 0
                                        navigateTo("WarrantyDetail")
                                    } else navigateTo(screen)
                                }
                            )

                            "Profile" -> ProfileScreen(
                                onNavigate = { screen ->
                                    val target = when (screen) {
                                        "Chat" -> "AskAiAssistant"
                                        "MyWarranty" -> "MyWarranty"
                                        "Dashboard" -> "Dashboard"
                                        else -> screen
                                    }
                                    navigateTo(target)
                                }
                            )

                            "EditProfile" -> EditProfileScreen(onBack = { navigateBack() })
                            "PrivacySecurity" -> PrivacySecurityScreen(onBack = { navigateBack() }, onNavigate = { screen -> navigateTo(screen) })
                            "ChangePassword" -> ChangePasswordScreen(onBack = { navigateBack() })
                            "Notifications" -> NotificationsScreen(onBack = { navigateBack() }, onNavigate = { navigateTo(it) })
                            "MyOrders" -> MyOrdersScreen(onBack = { navigateBack() }, onNavigate = { navigateTo(it) })
                            "SavedAddresses" -> SavedAddressesScreen(onBack = { navigateBack() }, onNavigate = { navigateTo(it) })
                            "PaymentMethods" -> PaymentHistoryScreen(
                                onBack = { navigateBack() },
                                onNavigate = { route -> navigateTo(route) }
                            )

                            "Compare" -> CompareScreen(
                                onBack = { navigateBack() },
                                onNavigate = { screen ->
                                    if(screen.startsWith("CompareResult/")) {
                                        val namesString = screen.substringAfter("/")
                                        compareDeviceNames = namesString.split(",").filter { it.isNotBlank() }
                                        navigateTo("CompareResult")
                                    } else if (screen == "Chat") {
                                        navigateTo("AskAiAssistant") // Fix for AI button in Compare Dock
                                    } else {
                                        navigateTo(screen)
                                    }
                                }
                            )

                            "CompareResult" -> CompareResultScreen(
                                deviceNames = compareDeviceNames,
                                onBack = { navigateBack() },
                                onNavigate = { screen -> navigateTo(screen) }
                            )
                        }
                    }

                    // --- Floating Cart Logic ---
                    val hideCartScreens = setOf(
                        "Splash", "Welcome", "Onboarding", "Login", "Register","ForgetPassword",
                        "MyCart", "Address", "Payment", "OrderSuccess",
                        "AddWarranty", "WarrantyDetail", "Invoice", "WarrantyAlerts",
                        "Profile", "EditProfile", "PrivacySecurity", "Notifications",
                        "MyOrders", "SavedAddresses", "PaymentMethods", "Compare",
                        "CompareResult", "ChangePassword", "AdminDashboard", "AdminOrderManagement",
                        "AdminAiSettings", "AdminWarranty", "AdminUsers", "AdminViewProfileDetail",
                        "ClaimWarranty", "ExtendWarranty"
                    )

                    if (realCartCount > 0 && currentScreen !in hideCartScreens) {
                        BadgedBox(
                            badge = { Badge { Text("$realCartCount") } },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 90.dp, end = 20.dp)
                                .clickable { navigateTo("MyCart") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MainActivityBlue)
                                    .padding(14.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(paymentId: String?, paymentData: PaymentData?) {
        val rzpPaymentId = paymentData?.paymentId ?: paymentId ?: ""
        val rzpOrderId = paymentData?.orderId ?: ""
        val rzpSignature = paymentData?.signature ?: ""
        PaymentCallbackHandler.onSuccess?.invoke(rzpPaymentId, rzpOrderId, rzpSignature)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        PaymentCallbackHandler.onError?.invoke(response ?: "Payment Failed")
    }
}