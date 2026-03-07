package com.simats.smartelectroai.api

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.mutableStateListOf
import okhttp3.MultipartBody
import okhttp3.RequestBody

// ==========================================
// 🌟 CENTROID IP MANAGEMENT 🌟
// Change the IP address HERE, and it will automatically
// update across the ENTIRE Android application.
// ==========================================
object ApiConfig {
    const val BASE_URL = "http://172.23.51.199:5000/"
}

// --- DATA MODELS ---

// NEW: Admin Payment Model
data class AdminPaymentItem(
    val id: Int,
    val user_name: String,
    val order_id: String,
    val transaction_id: String,
    val amount: String,
    val method: String,
    val status: String,
    val date: String
)

data class AdminPaymentsResponse(val status: String, val payments: List<AdminPaymentItem>)

data class FcmTokenRequest(val fcm_token: String, val platform: String = "android")
data class OrderHistoryItem(
    val order_id: Int, val invoice_no: String?, val product_name: String, val price: String,
    val raw_price: Double?, val image_url: String?, val date: String, val status: String,
    val delivery_status: String?, val delivery_step: Int, val delivery_text: String?,
    val status_color: String, val delivery_name: String?, val delivery_address: String?,
    val delivery_phone: String?, val payment_method: String?
)
data class MyOrdersResponse(val status: String, val orders: List<OrderHistoryItem>)
data class ProfileData(val full_name: String, val email: String, val mobile: String, val total_orders: Int, val saved_addresses: Int, val active_warranties: Int, val ai_searches: Int)
data class ProfileResponse(val status: String, val profile: ProfileData?)
data class ProductItem(val id: Int?, val name: String?, val price: String?, val image_url: String?)
data class ProductResponse(val status: String, val products: List<ProductItem>)
data class RegisterRequest(val full_name: String, val email: String, val mobile: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val status: String?, val message: String?, val token: String?, val user_name: String?, val is_admin: Boolean?)
data class AiRequest(val budget: Float, val usage: String, val brand: String, val storage: String, val battery: String, val notes: String)
data class RecommendationData(val top_match: TopMatch?, val alternatives: List<Alternative>?, val analysis: String?)
data class TopMatch(val id: Int?, val name: String?, val price: String?, val match_percent: String?, val battery_spec: String?, val display_spec: String?, val processor_spec: String?, val camera_spec: String?, val image_url: String?, val image_urls: List<String>?)
data class Alternative(val name: String?, val price: String?, val match_percent: String?, val image_url: String?)
data class AiResponse(val status: String?, val message: String?, val data: RecommendationData?)
data class OrderRequest(val product_id: Int)
data class BaseResponse(val status: String, val message: String)
data class OrderResponse(val status: String, val message: String, val order_id: Int?)
data class PaymentRequest(val order_id: Int, val payment_method: String, val amount: String)
data class PaymentResponse(val status: String, val message: String, val transaction_id: String?)
data class AddressModel(val id: Int? = null, val full_name: String, val mobile: String, val pincode: String, val city: String, val address_line: String, val is_default: Boolean = false)
data class AddressListResponse(val status: String, val addresses: List<AddressModel>)
data class SearchDeviceResult(val id: Int, val name: String, val price: String, val match_percent: String, val specs: String, val category: String, val image_url: String?)
data class SearchDeviceResponse(val status: String, val results: List<SearchDeviceResult>?)
data class CartItemModel(val id: Int, val name: String, val price: Int, val originalPrice: Int, val imageUrl: String, val specs: String, var quantity: Int = 1)
data class CompareRequest(val device1: String, val device2: String)
data class ComparePerformance(val processor: String, val cores: String, val ram: String)
data class CompareDisplay(val type: String, val resolution: String, val refresh_rate: String, val size: String)
data class CompareCamera(val rear_main: String, val rear_secondary: String, val rear_tertiary: String, val front: String)
data class CompareBattery(val capacity: String, val charging: String)
data class CompareStorage(val internal: String, val type: String)
data class CompareDeviceDetail(val id: Int? = null,val name: String, val price: String, val spec_score: String, val release_date: String, val performance: ComparePerformance, val display: CompareDisplay, val camera: CompareCamera, val battery: CompareBattery, val storage: CompareStorage, val pros: List<String>, val cons: List<String>, val antutu_score: String, val battery_life: String, val expert_score: String, val image_url: String?)
data class CompareData(val device1: CompareDeviceDetail, val device2: CompareDeviceDetail, val ai_analysis: String)
data class CompareResponse(val status: String, val data: CompareData?)
data class AdminStats(val total_users: String, val total_orders: String, val active_warranties: String, val ai_searches: String)
data class AdminOrder(val id: Int, val name: String, val price: String, val status: String)
data class AdminDashboardResponse(val status: String, val stats: AdminStats?, val recent_orders: List<AdminOrder>?)
data class AdminDetailedOrder(val id: Int, val invoice_no: String, val customer_name: String, val product_name: String, val sku: String, val price: String, val status: String, val tracking_number: String?, val address: String, val image_url: String?, val payment_method: String?)
data class AdminOrdersResponse(val status: String, val orders: List<AdminDetailedOrder>?)
data class UpdateOrderRequest(val status: String, val tracking_number: String)
data class AiLogItem(val log_id: String, val preferences: String, val product: String, val match_percent: Int, val date: String)
data class AiSettingsData(val is_enabled: Boolean, val gaming: Int, val camera: Int, val battery: Int, val budget: Int, val engine_mode: String, val logs: List<AiLogItem>)
data class HistoryItem(val title: String, val date: String, val desc: String, val is_last: Boolean)
data class WarrantyDetailData(val id: Int, val device_name: String, val device_type: String, val purchase_date: String, val expiry_date: String, val status: String, val progress: Float, val months_left: String, val invoice_name: String, val history: List<HistoryItem>)
data class WarrantyDetailResponse(val status: String, val data: WarrantyDetailData?)
data class AiSettingsResponse(val status: String, val data: AiSettingsData?)
data class UpdateAiSettingsRequest(val is_enabled: Boolean, val gaming: Int, val camera: Int, val battery: Int, val budget: Int, val engine_mode: String)
data class AdminWarrantyItem(val id: Int, val user_name: String, val device_name: String, val device_type: String, val expiry_date: String, val status: String, val claim_reason: String?, val claim_invoice_url: String?, val claim_device_url: String?)
data class AdminWarrantyResponse(val status: String, val warranties: List<AdminWarrantyItem>)
data class ApproveWarrantyRequest(val action: String)
data class WarrantyDevice(val id: Int, val name: String, val status: String, val expiry: String)
data class AiRecommendation(val message: String)
data class MyWarrantiesResponse(val status: String, val devices: List<WarrantyDevice>?, val ai_recommendation: AiRecommendation?)
data class AlertStats(val total_registered: Int, val active: Int, val expiring_soon: Int, val expired: Int)
data class AlertDevice(val id: Int, val name: String, val type: String, val expiry: String, val status: String)
data class AlertResponse(val status: String, val stats: AlertStats?, val devices: List<AlertDevice>)
data class AddWarrantyReq(val device_name: String, val brand: String, val expiry_date: String)
data class AddWarrantyRes(val status: String, val message: String)
data class ExtendWarrantyReq(val plan_id: String, val duration_months: Int, val amount: Int, val payment_id: String)
data class ExtendWarrantyRes(val status: String, val message: String, val new_expiry: String?)
data class ForgotPasswordRequest(val email: String)
data class ResetPasswordRequest(val email: String, val otp: String, val new_password: String)
data class SimpleResponse(val status: String, val message: String)
data class UniqueNotifPrefsData(val order_updates: Boolean, val warranty_alerts: Boolean, val ai_updates: Boolean, val promotions: Boolean, val frequency: String)
data class UniqueNotifFetchResponse(val status: String, val preferences: UniqueNotifPrefsData?)
data class UniqueNotifUpdateResponse(val status: String, val message: String)
data class PaymentHistoryItem(val id: Int, val order_id: String, val transaction_id: String, val payment_method: String, val amount: String, val date: String, val status: String)
data class PaymentHistoryResponse(val status: String, val history: List<PaymentHistoryItem>?)

// --- API SERVICE ---
interface ApiService {
    @POST("/register") fun registerUser(@Body request: RegisterRequest): Call<AuthResponse>
    @POST("/login") fun loginUser(@Body request: LoginRequest): Call<AuthResponse>
    @POST("/recommend") fun getRecommendations(@Body request: AiRequest): Call<AiResponse>
    @POST("/update_fcm_token") fun updateFcmToken(@Header("Authorization") token: String, @Body request: FcmTokenRequest): Call<BaseResponse>
    @POST("/place_order") fun placeOrder(@Header("Authorization") token: String, @Body request: OrderRequest): Call<OrderResponse>
    @POST("/process_payment") fun processPayment(@Header("Authorization") token: String, @Body request: PaymentRequest): Call<PaymentResponse>
    @GET("/get_addresses") fun getAddresses(@Header("Authorization") token: String): Call<AddressListResponse>
    @POST("/add_address") fun addAddress(@Header("Authorization") token: String, @Body address: AddressModel): Call<BaseResponse>
    @PUT("/update_address/{id}") fun updateAddress(@Header("Authorization") token: String, @Path("id") id: Int, @Body address: AddressModel): Call<BaseResponse>
    @DELETE("/delete_address/{id}") fun deleteAddress(@Header("Authorization") token: String, @Path("id") id: Int): Call<BaseResponse>
    @GET("/products") fun getAllProducts(): Call<ProductResponse>
    @GET("/profile") fun getProfile(@Header("Authorization") token: String): Call<ProfileResponse>
    @GET("/my_orders") fun getMyOrders(@Header("Authorization") token: String): Call<MyOrdersResponse>
    @GET("/search_devices") fun searchDevices(@Query("q") query: String): Call<SearchDeviceResponse>
    @POST("/compare_devices") fun compareDevices(@Body request: CompareRequest): Call<CompareResponse>
    @POST("/forgot_password") fun forgotPassword(@Body request: ForgotPasswordRequest): Call<SimpleResponse>
    @POST("/reset_password") fun resetPassword(@Body request: ResetPasswordRequest): Call<SimpleResponse>
    @GET("/notifications/preferences") fun getPrefs(@Header("Authorization") token: String): Call<UniqueNotifFetchResponse>
    @PUT("/notifications/preferences") fun updatePrefs(@Header("Authorization") token: String, @Body prefs: UniqueNotifPrefsData): Call<UniqueNotifUpdateResponse>
    @GET("/payment_history") fun getPaymentHistory(@Header("Authorization") token: String): Call<PaymentHistoryResponse>

    // --- USER WARRANTY ROUTES ---
    @GET("/api/my_warranties") fun getMyWarranties(@Header("Authorization") token: String): Call<MyWarrantiesResponse>
    @GET("/api/alerts") fun getAlerts(@Header("Authorization") token: String): Call<AlertResponse>
    @GET("/api/warranties/{id}") fun getWarrantyDetail(@Header("Authorization") token: String, @Path("id") id: Int): Call<WarrantyDetailResponse>
    @POST("/api/warranties/add") fun addWarrantyDevice(@Header("Authorization") token: String, @Body req: AddWarrantyReq): Call<AddWarrantyRes>
    @POST("/api/warranties/{id}/extend") fun extendWarranty(@Header("Authorization") token: String, @Path("id") id: Int, @Body req: ExtendWarrantyReq): Call<ExtendWarrantyRes>
    @Multipart @POST("/api/warranties/{id}/claim") fun submitWarrantyClaim(@Header("Authorization") token: String, @Path("id") id: Int, @Part("issue_type") issueType: RequestBody, @Part("description") description: RequestBody, @Part("service_mode") serviceMode: RequestBody, @Part invoiceImage: MultipartBody.Part, @Part deviceImage: MultipartBody.Part): Call<BaseResponse>

    // --- ADMIN ROUTES (FIXED PREFIXES) ---
    @GET("/admin/dashboard") fun getAdminDashboard(@Header("Authorization") token: String): Call<AdminDashboardResponse>
    @GET("/admin/orders") fun getAdminOrders(@Header("Authorization") token: String): Call<AdminOrdersResponse>
    @PUT("/admin/orders/{id}") fun updateAdminOrder(@Header("Authorization") token: String, @Path("id") orderId: Int, @Body request: UpdateOrderRequest): Call<AuthResponse>
    @GET("/admin/ai_settings") fun getAiSettings(@Header("Authorization") token: String): Call<AiSettingsResponse>
    @PUT("/admin/ai_settings") fun updateAiSettings(@Header("Authorization") token: String, @Body request: UpdateAiSettingsRequest): Call<AuthResponse>
    @GET("/admin/warranties") fun getAdminWarranties(@Header("Authorization") token: String): Call<AdminWarrantyResponse>
    @PUT("/admin/warranties/{id}/approve") fun approveAdminWarranty(@Header("Authorization") token: String, @Path("id") warrantyId: Int, @Body request: ApproveWarrantyRequest): Call<AuthResponse>
    @GET("/admin/payments") fun getAdminPayments(@Header("Authorization") token: String): Call<AdminPaymentsResponse>
    @PUT("/admin/payments/{id}/refund") fun refundPayment(@Header("Authorization") token: String, @Path("id") id: Int): Call<SimpleResponse>
}

// --- SINGLETON CLIENT ---
object RetrofitClient {
    // USING CENTROID HERE
    private val okHttpClient = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// --- HELPERS ---
object RecommendationManager { var result: RecommendationData? = null }
object OrderContext { var currentOrderId: Int = -1; var currentTotalAmount: Int = 0 }
object CartManager {
    val items = mutableStateListOf<CartItemModel>()
    fun addItem(product: CartItemModel) { val existing = items.find { it.id == product.id }; if (existing != null) existing.quantity++ else items.add(product) }
    fun clear() { items.clear() }
    fun getTotal(): Int = items.sumOf { it.price * it.quantity }
    fun getSavings(): Int = items.sumOf { (it.originalPrice - it.price) * it.quantity }
}
object AppConfig { const val RAZORPAY_KEY_ID = "rzp_test_APuQCp0MiHoD9M" }
object OrderTrackingManager { var currentOrder: OrderHistoryItem? = null }