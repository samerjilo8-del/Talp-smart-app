package com.smartteacher.app.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Helper to request POST_NOTIFICATIONS permission on Android 13+.
 * Each activity that needs notifications must call [register] in onCreate
 * before [request] is invoked.
 */
object NotificationPermissionHelper {

    private var launcher: ActivityResultLauncher<String>? = null

    fun register(activity: AppCompatActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result ignored: notifications are best-effort */ }
    }

    fun request(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
