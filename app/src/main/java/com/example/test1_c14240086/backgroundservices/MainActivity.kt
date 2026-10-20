package com.example.test1_c14240086.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var btnStart: Button
    private lateinit var btnLatihan: Button
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart = findViewById(R.id.btnStart)
        btnLatihan = findViewById(R.id.btnLatihan)
        statusTextView = findViewById(R.id.statusTextView)

        btnStart.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSION
                )
            }
        }

        btnLatihan.setOnClickListener {
            if (hasAllPermissions()) {
                startLatihanService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSION
                )
            }
        }

        handleIntent(intent)
    }

    private val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP = "com.example.test1_c14240086.backgroundservices.STOPPED_JUMP"
        const val ACTION_LATIHAN_SELESAI = "com.example.test1_c14240086.backgroundservices.LATIHAN_SELESAI"
        const val REQUEST_CODE_PERMISSION = 101
    }

    private fun hasAllPermissions(): Boolean {
        return permissionToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Background service sedang berjalan.."
    }

    private fun startLatihanService() {
        val serviceIntent = Intent(this, TrackerService::class.java).apply {
            putExtra("MODE", "LATIHAN")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Mode Latihan sedang berjalan (berjalan > 2 langkah)..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            var allPermissionGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionGranted = false
                    break
                }
            }
            if (allPermissionGranted) {
                val serviceIntent = Intent(this, TrackerService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
                finish()
            } else {
                statusTextView.text = "Izin Ditolak, Service tidak dapat dijalankan."
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            ACTION_SERVICE_STOPPED_JUMP -> {
                statusTextView.text = "Background service berhenti karena lompatan terdeteksi!"
            }
            ACTION_LATIHAN_SELESAI -> {
                val stepCount = intent.getIntExtra("EXTRA_STEP_COUNT", 0)
                statusTextView.text = "Latihan Selesai! Total Lompatan: $stepCount"
                createCalendarEvent(stepCount)
            }
        }
    }

    private fun createCalendarEvent(stepCount: Int) {
        try {
            val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Latihan Selesai - BackgroundServices")
                putExtra(CalendarContract.Events.DESCRIPTION, "Latihan hari ini selesai dengan total lompatan: $stepCount.")
                putExtra(CalendarContract.Events.EVENT_LOCATION, "Rumah / Lokasi Latihan")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 60 * 60 * 1000)
            }
            startActivity(calendarIntent)
        } catch (e: Exception) {
            statusTextView.text = "${statusTextView.text}\n(Gagal membuka kalender: ${e.message})"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}
