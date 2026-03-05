package com.simats.smartelectroai.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

// Lightweight isolated API model just for the background task
internal data class BgUrgentNotification(val title: String, val body: String)
internal data class BgWarrantyResponse(val status: String?, val urgent_notifications: List<BgUrgentNotification>?)

internal interface BackgroundWarrantyApi {
    @GET("/api/warranties")
    suspend fun getWarranties(@Header("Authorization") token: String): Response<BgWarrantyResponse>
}

class WarrantyCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Get the login token
        val token = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("jwt_token", "")

        // If the user is logged out, stop the background work immediately
        if (token.isNullOrEmpty()) {
            return Result.success()
        }

        try {
            // 2. Call your backend securely in the background
            val api = Retrofit.Builder()
                .baseUrl("http://10.156.35.203:5000/") // YOUR BACKEND IP
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BackgroundWarrantyApi::class.java)

            val response = api.getWarranties("Bearer $token")

            if (response.isSuccessful) {
                val alerts = response.body()?.urgent_notifications ?: emptyList()

                // 3. If there are alerts, trigger the Android Push Notification
                if (alerts.isNotEmpty()) {
                    showNotification(alerts[0].title, alerts[0].body)
                }
            }
        } catch (e: Exception) {
            return Result.retry() // Try again later if no internet
        }

        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0+ requires a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bg_warranty_alerts",
                "Warranty Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, "bg_warranty_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Fire the notification!
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}