package com.soulmatch.app
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.soulmatch.app.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp
class SoulMatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel("soulmatch_default","SoulMatch Notifications",NotificationManager.IMPORTANCE_DEFAULT))
            mgr.createNotificationChannel(NotificationChannel("soulmatch_messages","New Messages",NotificationManager.IMPORTANCE_HIGH))
        }
    }
}
