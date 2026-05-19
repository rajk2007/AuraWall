package com.rajk2007.aurawall

import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("aurawall_prefs", MODE_PRIVATE)
        val lockSwitch = findViewById<Switch>(R.id.switch_lockscreen)
        lockSwitch.isChecked = prefs.getBoolean("lock_screen_enabled", false)

        lockSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("lock_screen_enabled", isChecked).apply()
            if (isChecked) {
                startForegroundService(Intent(this, LockScreenService::class.java))
            } else {
                stopService(Intent(this, LockScreenService::class.java))
            }
        }
    }
}
