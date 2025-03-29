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

        // register wifi state change receiver
        stateChangeReceiver = StateChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(stateChangeReceiver, filter)

        // set opening page
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)


        // request location permission (needed to obtain the wifi ssid in stateChangeReceiver)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // register worker (starts the service if not running)
        val request = PeriodicWorkRequestBuilder<LoginWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueue(request);

        // start and stop buttons action assignments
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
