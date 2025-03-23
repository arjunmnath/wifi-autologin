package com.arjunmnath.wifilogger.wifi

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters

class LoginWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val intent = Intent(applicationContext, LoginService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            applicationContext.startForegroundService(intent)
        } else {
//            applicationContext.startService(intent)
        }

        return Result.success()
    }
}