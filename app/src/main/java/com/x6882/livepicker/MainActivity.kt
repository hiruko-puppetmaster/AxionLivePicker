package com.x6882.livepicker

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : Activity() {

    companion object {
        private const val REQ_PICK_MP4 = 1001
    }

    private lateinit var listView: ListView
    private lateinit var statusText: TextView
    private lateinit var videoStatusText: TextView
    private val wallpaperList = mutableListOf<WallpaperInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.preferredDisplayModeId = 0
        }
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val prefs = getSharedPreferences(VideoWallpaperService.PREFS_NAME, Context.MODE_PRIVATE)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(dp(16), dp(20), dp(16), dp(16))
        }

        val titleView = TextView(this).apply {
            text = "AXION LIVE WALLPAPER ENGINE"
            setTextColor(Color.parseColor("#FF0033"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            letterSpacing = 0.08f
        }

        statusText = TextView(this).apply {
            text = "Infinix X6882 • 120Hz Hardware Accelerated"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(2), 0, dp(12))
        }

        // 120FPS MP4 VIDEO ENGINE CARD
        val videoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#110005"))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.parseColor("#FF0033"))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }

        val videoHeader = TextView(this).apply {
            text = "120 FPS LOCAL MP4 ENGINE"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
        }

        videoStatusText = TextView(this).apply {
            setTextColor(Color.parseColor("#AAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(4), 0, dp(10))
        }
        updateVideoStatusLabel()

        val togglesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        val muteCheck = CheckBox(this).apply {
            text = "Mute Audio"
            setTextColor(Color.WHITE)
            textSize = 12f
            isChecked = prefs.getBoolean(VideoWallpaperService.KEY_MUTE_AUDIO, true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(VideoWallpaperService.KEY_MUTE_AUDIO, checked).apply()
                sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD_VIDEO).setPackage(packageName))
            }
        }

        val cropCheck = CheckBox(this).apply {
            text = "Fullscreen Crop (1080x2460)"
            setTextColor(Color.WHITE)
            textSize = 12f
            isChecked = prefs.getBoolean(VideoWallpaperService.KEY_SCALE_CROP, true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(VideoWallpaperService.KEY_SCALE_CROP, checked).apply()
                sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD_VIDEO).setPackage(packageName))
            }
        }

        togglesRow.addView(muteCheck)
        togglesRow.addView(cropCheck)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val pickMp4Btn = Button(this).apply {
            text = "SELECT .MP4 FILE"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF0033"))
                cornerRadius = dp(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                rightMargin = dp(8)
            }
            setOnClickListener { openMp4FilePicker() }
        }

        val applyVideoBtn = Button(this).apply {
            text = "ACTIVATE"
            setTextColor(Color.parseColor("#FF0033"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), Color.parseColor("#FF0033"))
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 0.7f)
            setOnClickListener {
                val component = ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                launchComponentPreview(component)
            }
        }

        btnRow.addView(pickMp4Btn)
        btnRow.addView(applyVideoBtn)

        videoPanel.addView(videoHeader)
        videoPanel.addView(videoStatusText)
        videoPanel.addView(togglesRow)
        videoPanel.addView(btnRow)

        val listHeader = TextView(this).apply {
            text = "INSTALLED LIVE WALLPAPERS (TAP: PREVIEW • HOLD: FORCE APPLY)"
            setTextColor(Color.parseColor("#777777"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(8))
        }

        listView = ListView(this).apply {
            dividerHeight = dp(10)
            selector = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        rootLayout.addView(titleView)
        rootLayout.addView(statusText)
        rootLayout.addView(videoPanel)
        rootLayout.addView(listHeader)
        rootLayout.addView(listView)
        setContentView(rootLayout)

        loadLiveWallpapers()
    }

    override fun onResume() {
        super.onResume()
        updateVideoStatusLabel()
        loadLiveWallpapers()
    }

    private fun updateVideoStatusLabel() {
        val file = File(filesDir, VideoWallpaperService.VIDEO_FILE_NAME)
        if (file.exists() && file.length() > 0) {
            val sizeMb = String.format("%.2f MB", file.length() / (1024f * 1024f))
            videoStatusText.text = "Loaded Video: active_wallpaper.mp4 ($sizeMb)"
            videoStatusText.setTextColor(Color.parseColor("#00FF88"))
        } else {
            videoStatusText.text = "No MP4 loaded yet. Tap 'SELECT .MP4 FILE' to import."
            videoStatusText.setTextColor(Color.parseColor("#AAAAAA"))
        }
    }

    private fun openMp4FilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, REQ_PICK_MP4)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_MP4 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            copyVideoToInternalStorage(uri)
        }
    }

    private fun copyVideoToInternalStorage(uri: Uri) {
        try {
            val destFile = File(filesDir, VideoWallpaperService.VIDEO_FILE_NAME)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            updateVideoStatusLabel()
            Toast.makeText(this, "MP4 Imported! Activating 120FPS Engine...", Toast.LENGTH_SHORT).show()

            sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD_VIDEO).setPackage(packageName))

            val component = ComponentName(this, VideoWallpaperService::class.java)
            launchComponentPreview(component)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to copy video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadLiveWallpapers() {
        wallpaperList.clear()
        val intent = Intent(WallpaperService.SERVICE_INTERFACE)
        val resolveInfos = packageManager.queryIntentServices(
            intent,
            PackageManager.GET_META_DATA
        )

        for (resolveInfo in resolveInfos) {
            try {
                val info = WallpaperInfo(this, resolveInfo)
                wallpaperList.add(info)
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        wallpaperList.sortBy { it.loadLabel(packageManager).toString().lowercase() }
        statusText.text = "AxionOS 2.9 (Android 16) • ${wallpaperList.size} Engine(s) Available"

        listView.adapter = WallpaperAdapter(this, wallpaperList)

        listView.setOnItemClickListener { _, _, position, _ ->
            launchComponentPreview(wallpaperList[position].component)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showActionDialog(wallpaperList[position])
            true
        }
    }

    private fun launchComponentPreview(component: ComponentName) {
        try {
            val previewIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(previewIntent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "System previewer missing. Applying directly...",
                Toast.LENGTH_SHORT
            ).show()
            forceApplyWallpaper(component)
        }
    }

    private fun showActionDialog(info: WallpaperInfo) {
        val options = mutableListOf("Preview Wallpaper", "Force Apply Directly (Priv/Root)")
        val settingsActivity = info.settingsActivity
        if (!settingsActivity.isNullOrEmpty() && settingsActivity != MainActivity::class.java.name) {
            options.add("Open Wallpaper Settings")
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(info.loadLabel(packageManager))
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> launchComponentPreview(info.component)
                    1 -> forceApplyWallpaper(info.component)
                    2 -> {
                        try {
                            val settingsIntent = Intent().apply {
                                component = ComponentName(info.packageName, settingsActivity!!)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(settingsIntent)
                        } catch (ex: Exception) {
                            Toast.makeText(this, "Cannot open settings: ${ex.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun forceApplyWallpaper(component: ComponentName) {
        try {
            val wm = WallpaperManager.getInstance(this)
            val method = WallpaperManager::class.java.getMethod(
                "setWallpaperComponent",
                ComponentName::class.java
            )
            method.invoke(wm, component)
            Toast.makeText(this, "Live Wallpaper Applied!", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) {
        }

        try {
            val flatComponent = component.flattenToShortString()
            val cmd = arrayOf(
                "su", "-c",
                "service call wallpaper 2 s16 ${component.packageName} s16 ${component.className} || am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER --ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT $flatComponent"
            )
            Runtime.getRuntime().exec(cmd)
            Toast.makeText(this, "Triggered via Root Shell", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Direct apply requires priv-app permission or Root.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private inner class WallpaperAdapter(
        context: Context,
        private val items: List<WallpaperInfo>
    ) : ArrayAdapter<WallpaperInfo>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val info = items[position]
            val isBuiltInVideo = info.packageName == packageName

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = GradientDrawable().apply {
                    setColor(if (isBuiltInVideo) Color.parseColor("#140006") else Color.parseColor("#0D0D0D"))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), if (isBuiltInVideo) Color.parseColor("#FF0033") else Color.parseColor("#33FF0033"))
                }
            }

            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                    rightMargin = dp(14)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                val thumb = try {
                    info.loadThumbnail(packageManager)
                } catch (_: Exception) {
                    null
                } ?: info.loadIcon(packageManager)
                setImageDrawable(thumb)
            }

            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameText = TextView(context).apply {
                text = info.loadLabel(packageManager)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
            }

            val pkgText = TextView(context).apply {
                text = "${info.packageName}
${info.serviceName.substringAfterLast('.')}"
                setTextColor(Color.parseColor("#777777"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }

            val badge = TextView(context).apply {
                text = if (isBuiltInVideo) "120FPS" else "APPLY"
                setTextColor(Color.parseColor("#FF0033"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1AFF0033"))
                    cornerRadius = dp(6).toFloat()
                }
            }

            textContainer.addView(nameText)
            textContainer.addView(pkgText)

            card.addView(iconView)
            card.addView(textContainer)
            card.addView(badge)

            return card
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
