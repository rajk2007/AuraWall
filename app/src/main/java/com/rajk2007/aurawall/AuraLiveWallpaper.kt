package com.rajk2007.aurawall

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AuraLiveWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = AuraEngine()

    inner class AuraEngine : Engine(), SensorEventListener {

        private val handler = Handler(Looper.getMainLooper())
        private var bgBitmap: Bitmap? = null
        private var fgBitmap: Bitmap? = null
        private var offsetX = 0f
        private var offsetY = 0f
        private var sensorManager: SensorManager? = null

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, 33)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadBitmaps()
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager?.registerListener(
                this,
                sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                loadBitmaps()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawRunnable)
            sensorManager?.unregisterListener(this)
            bgBitmap?.recycle()
            fgBitmap?.recycle()
        }

        private fun loadBitmaps() {
            val bgFile = File(filesDir, "bg_layer.jpg")
            val fgFile = File(filesDir, "fg_layer.png")
            if (bgFile.exists()) bgBitmap = BitmapFactory.decodeFile(bgFile.absolutePath)
            if (fgFile.exists()) fgBitmap = BitmapFactory.decodeFile(fgFile.absolutePath)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) draw(canvas)
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun draw(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            // Layer 1: Background
            bgBitmap?.let { bg ->
                val scaled = Bitmap.createScaledBitmap(bg, w.toInt(), h.toInt(), true)
                val matrix = Matrix()
                matrix.postTranslate(-offsetX * 20f, -offsetY * 20f)
                canvas.drawBitmap(scaled, matrix, null)
            } ?: canvas.drawColor(Color.BLACK)

            // Layer 2: Glass clock
            drawClockCard(canvas, w, h)

            // Layer 3: Foreground
            fgBitmap?.let { fg ->
                val fgWidth = (w * 0.65f).toInt()
                val fgHeight = (fg.height * (fgWidth.toFloat() / fg.width)).toInt()
                val scaled = Bitmap.createScaledBitmap(fg, fgWidth, fgHeight, true)
                val left = (w - fgWidth) / 2f + offsetX * 10f
                val top = h - fgHeight - 80f + offsetY * 10f
                canvas.drawBitmap(scaled, left, top, null)
            }
        }

        private fun drawClockCard(canvas: Canvas, w: Float, h: Float) {
            val cardW = w * 0.75f
            val cardH = 260f
            val cardLeft = (w - cardW) / 2f
            val cardTop = h * 0.18f
            val radius = 48f
            val rect = RectF(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)

            // Glass fill
            canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                color = Color.argb(60, 255, 255, 255)
                isAntiAlias = true
            })

            // Glass border
            canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            })

            // Clock
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText(timeStr, w / 2f, cardTop + 160f, Paint().apply {
                color = Color.WHITE
                textSize = 110f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            })

            // Date
            val dateStr = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
            canvas.drawText(dateStr, w / 2f, cardTop + 220f, Paint().apply {
                color = Color.argb(200, 255, 255, 255)
                textSize = 38f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            })
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                offsetX = -event.values[0] / 10f
                offsetY = event.values[1] / 10f
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
