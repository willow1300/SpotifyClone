package com.example.spotifyclone.exoPlayer.callBacks

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import com.example.spotifyclone.exoPlayer.MusicService
import com.example.spotifyclone.other.Constants.NOTIFICATION_ID

@UnstableApi
class MusicPlayerNotificationListener(
    private val musicService: MusicService
): PlayerNotificationManager.NotificationListener {
    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)

        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf()

        }

    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)

        musicService.apply {
            if (ongoing && !isForegroundService){
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)

                )
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
            }

        }

    }

}