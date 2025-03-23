package com.arjunmnath.wifilogger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            val serviceIntent = Intent(this, WifiLoginService::class.java)
            startService(serviceIntent)
        }

        stopButton.setOnClickListener {
            val serviceIntent = Intent(this, WifiLoginService::class.java)
            stopService(serviceIntent)
        }
    }
}
