package com.example.spotifyclone.exoPlayer.callBacks

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.spotifyclone.exoPlayer.MusicService

@UnstableApi
class MusicPlayerEventlistener @OptIn(UnstableApi::class) constructor
    (
    private val musicService: MusicService
): Player.Listener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)

        if (playbackState == Player.STATE_READY && !playWhenReady){
            musicService.stopForeground(false)
        }

    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(musicService, "An unknown error occurred", Toast.LENGTH_LONG).show()
    }
}