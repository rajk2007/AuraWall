package com.rajk2007.aurawall

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var previewImage: ImageView
    private lateinit var btnPick: Button
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnSettings: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewImage = findViewById(R.id.preview_image)
        btnPick = findViewById(R.id.btn_pick)
        btnSetWallpaper = findViewById(R.id.btn_set_wallpaper)
        btnSettings = findViewById(R.id.btn_settings)
        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.status_text)

        checkOverlayPermission()
        requestStoragePermission()

        btnPick.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSetWallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, AuraLiveWallpaper::class.java)
            )
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Show existing preview if available
        val bgFile = File(filesDir, "bg_layer.jpg")
        if (bgFile.exists()) {
            previewImage.setImageBitmap(BitmapFactory.decodeFile(bgFile.absolutePath))
        }
    }

    private fun processImage(uri: Uri) {
        statusText.text = getString(R.string.processing)
        btnPick.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sourceBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                // Scale down for faster processing (max 1080px wide)
                val scaled = scaleBitmap(sourceBitmap, 1080)

                val (bgBitmap, fgBitmap) = ImageSegmenter.segment(this@MainActivity, scaled)
                val blurredBg = ImageSegmenter.blurBitmap(bgBitmap)

                // Save both layers to internal storage
                saveBitmap(blurredBg, "bg_layer.jpg", Bitmap.CompressFormat.JPEG)
                saveBitmap(fgBitmap, "fg_layer.png", Bitmap.CompressFormat.PNG)

                withContext(Dispatchers.Main) {
                    previewImage.setImageBitmap(blurredBg)
                    progressBar.visibility = View.GONE
                    statusText.text = "Done! Tap 'Set as Live Wallpaper'"
                    btnPick.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    statusText.text = "Error: ${e.message}"
                    btnPick.isEnabled = true
                    Toast.makeText(this@MainActivity, "Segmentation failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveBitmap(bitmap: Bitmap, filename: String, format: Bitmap.CompressFormat) {
        val file = File(filesDir, filename)
        FileOutputStream(file).use { bitmap.compress(format, 95, it) }
    }

    private fun scaleBitmap(src: Bitmap, maxWidth: Int): Bitmap {
        if (src.width <= maxWidth) return src
        val ratio = maxWidth.toFloat() / src.width
        return Bitmap.createScaledBitmap(src, maxWidth, (src.height * ratio).toInt(), true)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    101
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    101
                )
            }
        }
    }
}
