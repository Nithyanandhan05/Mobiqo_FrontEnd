package com.simats.smartelectroai.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.simats.smartelectroai.R

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(onVideoFinished: () -> Unit) {
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Point it to your video in the res/raw folder
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
            val mediaItem = MediaItem.fromUri(videoUri)

            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true // Autoplay

            // Listen for when the video finishes playing
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onVideoFinished() // Trigger the navigation!
                    }
                }
            })
        }
    }

    // Clean up the player when the splash screen is destroyed to free up memory
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Render the video player
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Hide play/pause/timeline controls
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Crop to fill screen perfectly
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        // Background color behind the video
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    // Provide an empty lambda so the preview doesn't crash
    SplashScreen(onVideoFinished = {})
}