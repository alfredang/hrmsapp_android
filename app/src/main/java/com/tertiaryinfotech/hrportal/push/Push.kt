package com.tertiaryinfotech.hrportal.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tertiaryinfotech.hrportal.MainActivity
import com.tertiaryinfotech.hrportal.R
import com.tertiaryinfotech.hrportal.data.HrmsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Push alerts (FCM). The server (`/api/mobile/push-token`) keeps one row per device token and
 * sends a push whenever it creates an in-app notification: missed clock-in/out and timesheet
 * reminders for interns, approval requests (leave, MC, time off, expense claims) for admins.
 */
object Push {
    const val CHANNEL_ID = "hrms_alerts"
    const val EXTRA_LINK = "link"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A web-style link ("/leave", "/approvals") from a tapped notification, consumed by the nav host. */
    var pendingLink by mutableStateOf<String?>(null)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "HR alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Approvals, clock-in/out and timesheet reminders"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** True when the app may post notifications (always before Android 13). */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: Exception) {
            cont.resume(null)
        }
    }

    /** After sign-in (or a restored session): send this device's token to the server. Best effort. */
    fun registerAfterSignIn() {
        scope.launch {
            val token = currentToken() ?: return@launch
            runCatching { HrmsApi.registerPushToken(token) }
        }
    }

    /** Before sign-out, while the session cookie is still valid: stop pushes to this device. */
    suspend fun unregisterBeforeSignOut() {
        val token = currentToken() ?: return
        runCatching { HrmsApi.unregisterPushToken(token) }
    }

    internal fun registerToken(token: String) {
        // 401 when nobody is signed in — the token is sent again after the next sign-in.
        scope.launch { runCatching { HrmsApi.registerPushToken(token) } }
    }

    /** Shows a notification while the app is in the foreground (FCM only auto-displays in the background). */
    internal fun show(context: Context, title: String, body: String, link: String?) {
        if (!canNotify(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (link != null) putExtra(EXTRA_LINK, link)
        }
        val pi = PendingIntent.getActivity(
            context, (link ?: title).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFF2F5BE6.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), n)
        } catch (_: SecurityException) { /* permission revoked */ }
    }
}

class HrmsMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = Push.registerToken(token)

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        Push.show(applicationContext, title, body, message.data[Push.EXTRA_LINK])
    }
}
