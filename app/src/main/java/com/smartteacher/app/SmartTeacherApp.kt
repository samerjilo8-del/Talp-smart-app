package com.smartteacher.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.smartteacher.app.backend.SupabaseConfig

class SmartTeacherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        SupabaseConfig.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الواجبات والاختبارات والملاحظات"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "smart_teacher_channel"
        lateinit var instance: SmartTeacherApp
            private set
    }
}
