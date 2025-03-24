package com.arjunmnath.wifilogger

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.arjunmnath.wifilogger.wifi.LoginWorker
import com.arjunmnath.wifilogger.wifi.LoginService
import com.arjunmnath.wifilogger.wifi.StateChangeReceiver
import java.util.concurrent.TimeUnit
import android.Manifest
class MainActivity : ComponentActivity() {
    private lateinit var stateChangeReceiver: StateChangeReceiver
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
        val request = PeriodicWorkRequestBuilder<LoginWorker>(1, TimeUnit.HOURS).build()
        stateChangeReceiver = StateChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(stateChangeReceiver, filter)
        WorkManager.getInstance(this).enqueue(request);
        startButton.setOnClickListener {
            val serviceIntent = Intent(this, LoginService::class.java)
            serviceIntent.action = LoginService.ACTION_LOGIN
            startService(serviceIntent)
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, LoginService::class.java)
            stopService(serviceIntent)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permission granted")
            } else {
                Log.d("MainActivity", "Location permission denied")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stateChangeReceiver)
    }
}
