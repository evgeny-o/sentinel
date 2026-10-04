package com.ptero.sentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ptero.sentinel.service.SensorNodeService

class MainActivity : ComponentActivity() {
    private lateinit var statusView: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startSensorNodeService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.service_status)
        findViewById<Button>(R.id.start_service).setOnClickListener {
            requestNotificationPermissionAndStart()
        }
        findViewById<Button>(R.id.stop_service).setOnClickListener {
            stopService(Intent(this, SensorNodeService::class.java))
            statusView.setText(R.string.service_stopped)
        }
    }

    private fun requestNotificationPermissionAndStart() {
        val requiresPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val permissionGranted = !requiresPermission ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            startSensorNodeService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startSensorNodeService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, SensorNodeService::class.java),
        )
        statusView.setText(R.string.service_running)
    }
}
