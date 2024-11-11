package com.example.spotifyclone.exoPlayer

import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.spotifyclone.conv.toMediaItem
import com.example.spotifyclone.exoPlayer.callBacks.MusicPlaybackPreparer
import com.example.spotifyclone.exoPlayer.callBacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

@UnstableApi
@AndroidEntryPoint
class MusicService: MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaControllerCompat: MediaControllerCompat

    var isForegroundService = false
    private var curPlayingSong: MediaMetadataCompat? = null
    private lateinit var musicPlaybackPreparer: MusicPlaybackPreparer

    override fun onCreate() {
        super.onCreate()

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)

        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true

        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){

        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){ mediaMetadataCompat ->

            mediaMetadataCompat?.let {
                //Update the current playing song
                curPlayingSong = it

                //set up exoplayer with the media item and prepare
                prepareAndPlay(
                    firebaseMusicSource.songs,
                    curPlayingSong!!.description.mediaId
                )

            }

        }

        mediaControllerCompat = MediaControllerCompat(this, mediaSession).apply {
            registerCallback(object : MediaControllerCompat.Callback(){
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    super.onPlaybackStateChanged(state)
                    //Handle playback state changes
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    super.onMetadataChanged(metadata)
                    //update metadata if needed
                }
            })

        }



        //Initial playback state
        updatePlaybackSate()

        //Listening for exoplayer events to update media session
        exoPlayer.addListener(object : Player.Listener{
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

            }

        })

    }

    private fun updatePlaybackSate() {
        val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                .setState(state, exoPlayer.contentPosition, 1F)
                .build()
        )
    }


    private fun prepareAndPlay(songs: List<MediaMetadataCompat>, mediaId: String ?= null) {
        //Find the item to play by its id, otherwise play the first item in the list
        val itemToPlay = mediaId?.let {
            songs.find {
                mediaId == it.description.mediaId
            }
        } ?: songs.firstOrNull()

        //if the item to play if found proceed with setting up the playlist and starting playback
        itemToPlay?.let {
            //Get the index of the item to play, default to 0 if not found
            val curSongIndex = if (curPlayingSong == null) 0 else songs.indexOf(itemToPlay)

            //prepare the exoplayer with playlist as media source
            exoPlayer.setMediaSource(firebaseMusicSource.asMediaSource(dataSourceFactory))
            exoPlayer.prepare()

            //seek to found songs index among the list to play
            exoPlayer.seekTo(curSongIndex, 0L)
            exoPlayer.playWhenReady = true

            //update the current playing song
            curPlayingSong = itemToPlay

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}