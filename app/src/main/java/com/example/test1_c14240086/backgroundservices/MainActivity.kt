package com.example.test1_c14240086.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.ContextWrapper


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart.setOnClickListener {
            if (hasAllPermissions()){
                startTrackerService()
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    permissionToRequest,
                    REQUEST_CODE_PERMISSION
                )
            }
        }

        handleIntent(intent)

    }

    private lateinit var btnStart : Button
    private lateinit var statusTextView: TextView

    private val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }else{
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    companion object{
        const val ACTION_SERVICE_STOPPED_JUMP = "com.example.test1_c14240086.backgroundservices"
        const val REQUEST_CODE_PERMISSION = 101
    }

    private fun hasAllPermissions(): Boolean{
        return permissionToRequest.all {
            ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService(){
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this,serviceIntent)
        statusTextView.text = "Background service sedang berjalan.."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION){
            var  allPermissionGranted = true
            for (result in grantResults){
                if (result != PackageManager.PERMISSION_GRANTED){
                    allPermissionGranted = false
                    break
                }
            }
            if (allPermissionGranted){
                val serviceIntent = Intent(this, TrackerService::class.java)
                startForegroundService(serviceIntent)
                finish()
            }else{
                statusTextView.text = "Izin Ditolak, Service tidak dapat dijalankan."
            }
        }
    }

    private fun handleIntent(intent: Intent?){
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP){
            statusTextView.text = "Background service sudah berhenti karena Anda Jumpped"
        }
    }

    override fun onNewintent(intent: Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }


}