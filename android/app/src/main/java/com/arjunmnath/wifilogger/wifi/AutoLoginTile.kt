package com.arjunmnath.wifilogger.wifi
import android.app.PendingIntent
import android.service.quicksettings.TileService
import android.service.quicksettings.Tile
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.arjunmnath.wifilogger.MainActivity

class AutoLoginTile: TileService() {

    override fun onTileAdded() {
        // Called when the user adds the tile to Quick Settings
        qsTile.icon = Icon.createWithResource(this, android.R.drawable.ic_menu_mapmode)
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    override fun onStartListening() {
        // Called when Quick Settings is opened
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    override fun onClick() {
        val intent = Intent(this, LoginService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Log.d("AutoLoginTile", "Tile clicked")
        // For API Level 34 and above, use PendingIntent
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            val pendingIntent = PendingIntent.getActivity(
//                this,
//                0,  // Request code
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//            )
//            startActivityAndCollapse(pendingIntent)
//        } else {
//            // For devices below API level 34, just open the activity
//            startActivity(intent)
//        }
    }

    override fun onStopListening() {
        // Called when Quick Settings is closed
    }

    override fun onTileRemoved() {
        // Called when the tile is removed from Quick Settings
    }
}
