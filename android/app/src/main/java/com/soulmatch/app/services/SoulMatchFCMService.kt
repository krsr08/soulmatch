package com.soulmatch.app.services
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.MainActivity
import com.soulmatch.app.R
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.util.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
class SoulMatchFCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerToken(token)
    }
    override fun onMessageReceived(msg: RemoteMessage) {
        CrashReporter.breadcrumb("fcm_message:${msg.data["type"].orEmpty().ifBlank { "default" }}")
        ensureNotificationChannels()
        val title = msg.notification?.title ?: "SoulMatch"; val body = msg.notification?.body ?: ""
        val channel = when (msg.data["type"]) { "message" -> "soulmatch_messages"; else -> "soulmatch_default" }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, channel).setContentTitle(title).setContentText(body).setSmallIcon(R.drawable.ic_notification_soulmatch).setAutoCancel(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        getSystemService(NotificationManager::class.java).notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun registerToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = UserPreferences(applicationContext)
            val authToken = prefs.authToken.first() ?: return@launch
            if (!prefs.pushNotifications.first()) return@launch
            val body = """{"token":"$token"}""".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${BuildConfig.NOTIFICATION_BASE_URL}notifications/devices/fcm-token")
                .post(body)
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            runCatching { OkHttpClient().newCall(request).execute().close() }
                .onFailure { CrashReporter.recordNonFatal(it, "fcm_service_token_registration") }
        }
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel("soulmatch_default", "SoulMatch alerts", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("soulmatch_messages", "SoulMatch messages", NotificationManager.IMPORTANCE_HIGH)
        )
        channels.forEach(manager::createNotificationChannel)
    }
}
