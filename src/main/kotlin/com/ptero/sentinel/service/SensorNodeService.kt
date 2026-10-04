package com.ptero.sentinel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ptero.sentinel.BuildConfig
import com.ptero.sentinel.R
import com.ptero.sentinel.delivery.BoundedDeliveryQueue
import com.ptero.sentinel.delivery.DeliveryMessage
import com.ptero.sentinel.telemetry.NodeHealthTelemetry
import com.ptero.sentinel.telemetry.TelemetryCodec
import com.ptero.sentinel.transport.FailoverTransport
import com.ptero.sentinel.transport.SendResult
import com.ptero.sentinel.transport.TcpLineTransport
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorNodeService : LifecycleService() {
    private val sequence = AtomicLong(1)
    private val queue = BoundedDeliveryQueue(QUEUE_CAPACITY)
    private val codec = TelemetryCodec(NODE_ID)
    private val transport = FailoverTransport(
        listOf(
            TcpLineTransport(
                id = "usb_adb_reverse",
                host = "127.0.0.1",
                port = BuildConfig.BRIDGE_PORT,
            ),
            TcpLineTransport(
                id = "wifi",
                host = BuildConfig.WIFI_BRIDGE_HOST,
                port = BuildConfig.BRIDGE_PORT,
            ),
        ),
    )

    @Volatile
    private var activeTransport: String? = null

    @Volatile
    private var lastError: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        launchHealthProducer()
        launchDeliveryWorker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    private fun launchHealthProducer() {
        lifecycleScope.launch {
            while (isActive) {
                enqueueHealth()
                delay(HEALTH_INTERVAL_MS)
            }
        }
    }

    private fun launchDeliveryWorker() {
        lifecycleScope.launch {
            while (isActive) {
                val message = queue.peek()
                if (message == null) {
                    delay(IDLE_POLL_MS)
                    continue
                }

                val result = transport.send(message)
                when (val sendResult = result.sendResult) {
                    is SendResult.Acknowledged -> {
                        queue.acknowledgeThrough(sendResult.sequenceId)
                        activeTransport = result.transportId
                        lastError = null
                    }

                    is SendResult.Failed -> {
                        activeTransport = null
                        lastError = sendResult.reason
                        delay(RETRY_DELAY_MS)
                    }
                }
            }
        }
    }

    private fun enqueueHealth() {
        val nextSequence = sequence.getAndIncrement()
        val payload = NodeHealthTelemetry(
            serviceState = "running",
            activeTransport = activeTransport,
            queueDepth = queue.size(),
            batteryPercent = readBatteryPercent(),
            thermalStatus = getSystemService<PowerManager>()?.currentThermalStatus,
            lastError = lastError,
        )
        val jsonLine = codec.encodeHealth(
            sequenceId = nextSequence,
            capturedAtMs = System.currentTimeMillis(),
            capturedElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            payload = payload,
        )
        queue.enqueue(
            DeliveryMessage(
                sessionId = codec.sessionId,
                sequenceId = nextSequence,
                jsonLine = jsonLine,
            ),
        )
    }

    private fun readBatteryPercent(): Int? {
        val value = getSystemService<BatteryManager>()
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return value?.takeIf { it in 0..100 }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification = NotificationCompat.Builder(
        this,
        NOTIFICATION_CHANNEL_ID,
    )
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .setOngoing(true)
        .build()

    companion object {
        private const val NODE_ID = "s25fe_node_01"
        private const val NOTIFICATION_CHANNEL_ID = "sensor_node"
        private const val NOTIFICATION_ID = 1001
        private const val QUEUE_CAPACITY = 256
        private const val HEALTH_INTERVAL_MS = 10_000L
        private const val IDLE_POLL_MS = 250L
        private const val RETRY_DELAY_MS = 2_000L
    }
}
