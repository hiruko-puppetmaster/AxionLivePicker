package com.x6882.livepicker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import java.io.File

class VideoWallpaperService : WallpaperService() {

    companion object {
        const val ACTION_RELOAD_VIDEO = "com.x6882.livepicker.ACTION_RELOAD_VIDEO"
        const val PREFS_NAME = "axion_video_prefs"
        const val KEY_MUTE_AUDIO = "mute_audio"
        const val KEY_SCALE_CROP = "scale_crop"
        const val VIDEO_FILE_NAME = "active_wallpaper.mp4"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isSurfaceReady = false

        private val reloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_RELOAD_VIDEO) {
                    initOrReloadPlayer()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)

            val filter = IntentFilter(ACTION_RELOAD_VIDEO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(reloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(reloadReceiver, filter)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            isSurfaceReady = true
            request120HzSurface(holder)
            initOrReloadPlayer()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            request120HzSurface(holder)
        }

        private fun request120HzSurface(holder: SurfaceHolder) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    holder.surface.setFrameRate(
                        120.0f,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                        Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                    )
                } catch (_: Exception) {
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    holder.surface.setFrameRate(
                        120.0f,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                    )
                } catch (_: Exception) {
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            val player = mediaPlayer ?: return
            try {
                if (visible) {
                    applyPreferences(player)
                    if (!player.isPlaying) {
                        player.start()
                    }
                } else {
                    if (player.isPlaying) {
                        player.pause()
                    }
                }
            } catch (_: Exception) {
            }
        }

        private fun initOrReloadPlayer() {
            if (!isSurfaceReady) return
            releasePlayer()

            val videoFile = File(filesDir, VIDEO_FILE_NAME)
            if (!videoFile.exists() || videoFile.length() == 0L) {
                drawFallbackCanvas()
                return
            }

            try {
                mediaPlayer = MediaPlayer().apply {
                    setSurface(surfaceHolder.surface)
                    setDataSource(videoFile.absolutePath)
                    isLooping = true
                    applyPreferences(this)

                    setOnPreparedListener { mp ->
                        applyPreferences(mp)
                        if (isVisible) {
                            mp.start()
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        drawFallbackCanvas()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                drawFallbackCanvas()
            }
        }

        private fun applyPreferences(player: MediaPlayer) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isMuted = prefs.getBoolean(KEY_MUTE_AUDIO, true)
            val isCrop = prefs.getBoolean(KEY_SCALE_CROP, true)

            val volume = if (isMuted) 0f else 1f
            player.setVolume(volume, volume)

            val scalingMode = if (isCrop) {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            } else {
                MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
            try {
                player.setVideoScalingMode(scalingMode)
            } catch (_: Exception) {
            }
        }

        private fun drawFallbackCanvas() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.BLACK)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FF0033")
                        textSize = 42f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(
                        "No MP4 Selected in Axion Live Picker",
                        canvas.width / 2f,
                        canvas.height / 2f,
                        paint
                    )
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        private fun releasePlayer() {
            try {
                mediaPlayer?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isSurfaceReady = false
            releasePlayer()
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(reloadReceiver)
            } catch (_: Exception) {
            }
            releasePlayer()
        }
    }
}
