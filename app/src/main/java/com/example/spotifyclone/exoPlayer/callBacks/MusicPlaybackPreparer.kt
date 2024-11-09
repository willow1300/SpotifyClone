package com.example.spotifyclone.exoPlayer.callBacks

import android.support.v4.media.MediaMetadataCompat
import com.example.spotifyclone.exoPlayer.FirebaseMusicSource

class MusicPlaybackPreparer(
    private val firebaseMusicSource: FirebaseMusicSource,
    private val playerPrepared: (MediaMetadataCompat?) -> Unit
) {

    fun prepareMedia(mediaId: String){
        firebaseMusicSource.whenReady {
            val itemToPlay = firebaseMusicSource.songs.find { mediaId == it.description.mediaId }
            playerPrepared(itemToPlay)

        }

    }

}