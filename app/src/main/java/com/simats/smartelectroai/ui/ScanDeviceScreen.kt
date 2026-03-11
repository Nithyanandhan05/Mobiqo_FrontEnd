package com.simats.smartelectroai.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.simats.smartelectroai.api.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ScanDeviceScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isAnalyzing by remember { mutableStateOf(false) }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreviewView(
                context = context,
                lifecycleOwner = lifecycleOwner,
                isAnalyzing = isAnalyzing,
                onImageCaptured = { file ->
                    isAnalyzing = true
                    uploadAndAnalyzeImage(context, file, onNavigate) { isAnalyzing = false }
                }
            )

            // Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("AI Smart Lens", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.size(48.dp)) // Balance
            }

            // Scanning Overlay
            if (isAnalyzing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val iconScale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "scale_anim"
                        )

                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF2962FF), modifier = Modifier.size(64.dp).scale(iconScale))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Mobiqo is analyzing device...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan devices.")
        }
    }
}

@Composable
fun CameraPreviewView(context: Context, lifecycleOwner: androidx.lifecycle.LifecycleOwner, isAnalyzing: Boolean, onImageCaptured: (File) -> Unit) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    } catch (e: Exception) { Log.e("Camera", "Binding failed", e) }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // --- NEW: UI Hint for better accuracy ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = "Tip: For best accuracy, scan the back camera module or the 'About Phone' screen.",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // Shutter Button
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)).border(4.dp, Color.White, CircleShape)
                    .clickable(enabled = !isAnalyzing) {
                        val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture?.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onImageCaptured(file)
                            }
                            override fun onError(exception: ImageCaptureException) { Log.e("Camera", "Capture failed", exception) }
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

private fun uploadAndAnalyzeImage(context: Context, file: File, onNavigate: (String) -> Unit, onComplete: () -> Unit) {
    val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("jwt_token", "") ?: ""

    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

    RetrofitClient.instance.scanDevice("Bearer $token", body).enqueue(object : Callback<ScanResponse> {
        override fun onResponse(call: Call<ScanResponse>, response: Response<ScanResponse>) {
            onComplete()
            if (response.isSuccessful && response.body()?.status == "success") {
                val device = response.body()?.device
                if (device != null) {
                    val safePrice = if (device.price.contains(Regex("[0-9]"))) device.price else "45000"

                    // LOAD REAL SPECS FROM BACKEND
                    RecommendationManager.result = RecommendationData(
                        top_match = TopMatch(
                            id = device.id,
                            name = device.name,
                            price = safePrice,
                            match_percent = device.match_percent,
                            battery_spec = device.battery_spec,
                            display_spec = device.display_spec,
                            processor_spec = device.processor_spec,
                            camera_spec = device.camera_spec,
                            image_url = device.image_url,
                            image_urls = listOfNotNull(device.image_url)
                        ),
                        alternatives = emptyList(),
                        analysis = "Mobiqo successfully identified this device from your photo!"
                    )
                    onNavigate("ProductDetail")
                }
            } else {
                Toast.makeText(context, "Could not identify device.", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onFailure(call: Call<ScanResponse>, t: Throwable) {
            onComplete()
            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
        }
    })
}