package com.example.spotifyclone.exoPlayer


import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.Token
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerNotificationManager.NotificationListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.spotifyclone.R
import com.example.spotifyclone.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.spotifyclone.other.Constants.NOTIFICATION_ID

@UnstableApi
class MusicNotificationManager @OptIn(UnstableApi::class) constructor
    (
    private val context: Context,
    sessionToken: Token,
    notificationListener: NotificationListener,
    private val newSongCallback: () -> Unit
) {

        private val notificationManager: PlayerNotificationManager
        private val mediaController: MediaControllerCompat =
            MediaControllerCompat(context, sessionToken)

    init {

        notificationManager = PlayerNotificationManager.Builder(
                context,
                NOTIFICATION_ID,
                NOTIFICATION_CHANNEL_ID

            ).setMediaDescriptionAdapter(object :
                PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return mediaController.metadata.description.title.toString()
                }

                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    return  mediaController.sessionActivity
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return mediaController.metadata.description.subtitle.toString()
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    Glide.with(context).asBitmap()
                        .load(mediaController.metadata.description.iconUri)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                callback.onBitmap(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) = Unit

                        })

                    return null

                }

            })
                .setSmallIconResourceId(R.drawable.ic_image)
                .setChannelNameResourceId(R.string.notification_channel_name)
                .setChannelDescriptionResourceId(R.string.notification_channel_description)
                .setNotificationListener(notificationListener)
                .build()

        notificationManager.setMediaSessionToken(sessionToken)


        }

    fun showNotification(player: Player){
        notificationManager.setPlayer(player)

    }



}