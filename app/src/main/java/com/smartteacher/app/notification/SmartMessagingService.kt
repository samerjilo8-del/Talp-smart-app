package com.smartteacher.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartteacher.app.R
import com.smartteacher.app.SmartTeacherApp
import com.smartteacher.app.backend.Repository
import com.smartteacher.app.backend.SessionManager
import com.smartteacher.app.ui.student.StudentDashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives real Firebase Cloud Messaging push notifications.
 *
 * These notifications are sent by a Supabase Edge Function (see
 * supabase/send_push.sql) whenever the teacher creates a new assignment,
 * exam or note. They reach the student even when the app is fully closed,
 * because FCM wakes the process to deliver them.
 */
class SmartMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        val title = data["title"] ?: getString(R.string.app_name)
        val body = data["body"] ?: ""

        showNotification(type, title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Persist the FCM token on the student row in the cloud so the
        // server-side push can target this device.
        val session = SessionManager(applicationContext)
        if (session.getRole() == SessionManager.Role.STUDENT) {
            val studentId = session.getStudentId() ?: return
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { Repository.updateStudentFcmToken(studentId, token) }
            }
        }
    }

    private fun showNotification(type: String, title: String, body: String) {
        val intent = Intent(this, StudentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, type.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallIcon = when (type) {
            "exam" -> R.drawable.ic_exams
            "assignment" -> R.drawable.ic_assignments
            "note" -> R.drawable.ic_notes
            else -> R.drawable.ic_launcher_foreground
        }

        val notification = NotificationCompat.Builder(this, SmartTeacherApp.CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setColor(getColor(R.color.green_primary))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
