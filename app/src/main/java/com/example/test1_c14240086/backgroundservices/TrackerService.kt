package com.example.test1_c14240086.backgroundservices

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

class TrackerService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING = "tracker_service_running"
    private val NOTIFICATION_CHANNEL_ID_JUMP_DETECTED = "tracker_jump_detected"
    private val NOTIFICATION_ID_SERVICE_RUNNING = 1
    private val NOTIFICATION_ID_JUMP_DETECTED = 2

    private var isLatihanMode = false
    private var stepCount = 0
    private var lastAboveThreshold = false

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING,
                "Service Running Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            val jumpChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_JUMP_DETECTED,
                "Jump Detected Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(jumpChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
        Log.d("TrackerService", "Service onCreate")
    }

    private fun startJumpDetection() {
        accelerometer?.also {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TrackerService", "Service onStartCommand")

        isLatihanMode = intent?.getStringExtra("MODE") == "LATIHAN"
        stepCount = 0
        lastAboveThreshold = false

        val title = if (isLatihanMode) "Mode Latihan Berjalan" else "Pelacak Aktivitas"
        val text = if (isLatihanMode) "Mendeteksi langkah (threshold 12)..." else "Aplikasi Tracker Service sedang berjalan.."

        startForeground(
            NOTIFICATION_ID_SERVICE_RUNNING,
            buildServiceRunningNotification(title, text)
        )

        startJumpDetection()
        return START_STICKY
    }

    private fun buildServiceRunningNotification(title: String, text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_RUNNING)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]

            if (isLatihanMode) {
                val threshold = 12.0
                if (abs(y) > threshold) {
                    if (!lastAboveThreshold) {
                        stepCount++
                        lastAboveThreshold = true
                        Log.d("TrackerService", "Langkah terdeteksi! Step count: $stepCount, y: $y")

                        if (stepCount > 2) {
                            sensorManager.unregisterListener(this)
                            showLatihanCompletedNotification()
                            stopSelf()
                        }
                    }
                } else {
                    lastAboveThreshold = false
                }
            } else {
                val jumpThreshold = 15.0
                if (abs(y) > jumpThreshold) {
                    Log.d("TrackerService", "Lompatan Terdeteksi! y: $y")
                    sensorManager.unregisterListener(this)
                    showJumpNotification()
                    stopSelf()
                }
            }
        }
    }

    private fun showJumpNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SERVICE_STOPPED_JUMP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingMainActivityIntent = PendingIntent.getActivity(
            this, 1, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val jumpNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("Lompatan Terdeteksi")
            .setContentText("Service Berhenti. Ketuk buka aplikasi")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingMainActivityIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED, jumpNotification)
        } catch (e: SecurityException) {
            Log.e("TrackerService", "Gagal memunculkan notif: ${e.message}")
        }
    }

    private fun showLatihanCompletedNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_LATIHAN_SELESAI
            putExtra("EXTRA_STEP_COUNT", stepCount)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingMainActivityIntent = PendingIntent.getActivity(
            this, 2, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val latihanNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("Latihan Selesai")
            .setContentText("Total Lompatan: $stepCount. Ketuk untuk lihat & buat event kalender.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingMainActivityIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_JUMP_DETECTED, latihanNotification)
        } catch (e: SecurityException) {
            Log.e("TrackerService", "Gagal memunculkan notif latihan: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("TrackerService", "Service onDestroy")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}
