package com.rajk2007.aurawall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LockScreenService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView

    private val clockRunnable = object : Runnable {
        override fun run() {
            clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            dateText.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithNotification()
        showOverlay()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "aurawall_lockscreen"
        val channel = NotificationChannel(
            channelId, "Lock Screen", NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AuraWall Lock Screen Active")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .build()

        startForeground(1, notification)
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val container = FrameLayout(this)

        val bgFile = File(filesDir, "bg_layer.jpg")
        val fgFile = File(filesDir, "fg_layer.png")

        if (bgFile.exists()) {
            val bgView = ImageView(this)
            bgView.setImageBitmap(BitmapFactory.decodeFile(bgFile.absolutePath))
            bgView.scaleType = ImageView.ScaleType.CENTER_CROP
            container.addView(bgView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        val cardParams = FrameLayout.LayoutParams(700, 280).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 220
        }
        val card = FrameLayout(this)
        card.setBackgroundColor(Color.argb(60, 255, 255, 255))
        container.addView(card, cardParams)

        clockText = TextView(this).apply {
            textSize = 52f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        card.addView(clockText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 30 })

        dateText = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.argb(200, 255, 255, 255))
            gravity = Gravity.CENTER
        }
        card.addView(dateText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 160 })

        if (fgFile.exists()) {
            val fgView = ImageView(this)
            fgView.setImageBitmap(BitmapFactory.decodeFile(fgFile.absolutePath))
            fgView.scaleType = ImageView.ScaleType.FIT_CENTER
            container.addView(fgView, FrameLayout.LayoutParams(700, 900).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 80
            })
        }

        var startY = 0f
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startY = event.y
                MotionEvent.ACTION_UP -> {
                    if (startY - event.y > 300f) dismissOverlay()
                }
            }
            true
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        )

        overlayView = container
        windowManager?.addView(container, params)
        handler.post(clockRunnable)
    }

    private fun dismissOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        handler.removeCallbacks(clockRunnable)
    }

    override fun onDestroy() {
        dismissOverlay()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
