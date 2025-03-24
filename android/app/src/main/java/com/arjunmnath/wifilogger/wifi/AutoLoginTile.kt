package com.arjunmnath.wifilogger.wifi
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.service.quicksettings.TileService
import android.service.quicksettings.Tile
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class AutoLoginTile: TileService() {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_START_LISTENING -> triggerStartListening()
                ACTION_STOP_LISTENING -> triggerStopListening()
            }
        }
    }
    override fun onTileAdded() {
        // Called when the user adds the tile to Quick Settings
        qsTile.icon = Icon.createWithResource(this, android.R.drawable.ic_menu_mapmode)
        qsTile.state = Tile.STATE_ACTIVE
        updateTile()
    }

    override fun onClick() {
        val serviceIntent = Intent(this, LoginService::class.java)
        if (isServiceRunning(this, LoginService::class.java)) {
            stopService(serviceIntent)
        } else {
//            startForegroundService(serviceIntent)
        }
        updateTile()
    }

    override fun onTileRemoved() {
        // Called when the tile is removed from Quick Settings
    }

    companion object {
        const val ACTION_START_LISTENING = "com.arjunmnath.horizon.wifilogger.auto_login_tile.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.arjunmnath.horizon.wifilogger.auto_login_tile.ACTION_STOP_LISTENING"
        const val ACTION_NOTIFY = "com.arjunmnath.horizon.wifilogger.auto_login_tile.NOTIFY"
    }
    fun triggerStartListening() {
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    fun triggerStopListening() {
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartListening() {
        updateTile()
        super.onStartListening()
        val filter = IntentFilter()
        filter.addAction(ACTION_START_LISTENING)
        filter.addAction(ACTION_STOP_LISTENING)
        filter.addAction(ACTION_NOTIFY)
        registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)
    }
    private fun updateTile() {
        val tile = qsTile
        if (isServiceRunning(this, LoginService::class.java)) {
            Log.d("AutoLoginTile", "Service is running");
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.state = Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    override fun onStopListening() {
        // triggered when quick setting is closed
        super.onStopListening()
        unregisterReceiver(broadcastReceiver)
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == serviceClass.name }
    }
}