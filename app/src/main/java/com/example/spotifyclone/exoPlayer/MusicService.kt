package com.example.spotifyclone.exoPlayer

import android.app.PendingIntent
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.MediaSession.QueueItem
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaBrowser
import androidx.navigation.Navigator
import com.example.spotifyclone.conv.toMediaItem
import com.example.spotifyclone.exoPlayer.callBacks.MusicPlaybackPreparer
import com.example.spotifyclone.exoPlayer.callBacks.MusicPlayerEventlistener
import com.example.spotifyclone.exoPlayer.callBacks.MusicPlayerNotificationListener
import com.example.spotifyclone.other.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    private lateinit var musicPlayerEventlistener: MusicPlayerEventlistener

    var isForegroundService = false
    private var curPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInitialized = false

    companion object{
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }

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
            curSongDuration = exoPlayer.duration

        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource){ mediaMetadataCompat ->

            mediaMetadataCompat?.let {
                //Update the current playing song
                curPlayingSong = it

                //set up exoplayer with the media item and prepare
                prepareAndPlay(
                    firebaseMusicSource.songs,
                    curPlayingSong!!.description.mediaId,
                    true
                )

            }

        }

        musicPlayerEventlistener = MusicPlayerEventlistener(this)
        exoPlayer.addListener(musicPlayerEventlistener)
        musicNotificationManager.showNotification(exoPlayer)

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

                override fun onSessionEvent(event: String?, extras: Bundle?) {
                    super.onSessionEvent(event, extras)
                    //Handle media session events
                    if (event == "ACTION_PREPARE_MEDIA"){
                        //prepare media using the mediaId passed through extras
                        val mediaId = extras?.getString("MEDIA_ID")
                        if (mediaId != null ){
                            musicPlaybackPreparer.prepareMedia(mediaId)
                        }
                    }

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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                mediaItem?.let {
                    updateNotification(mediaItem) //calling custom update method
                }
            }

        })

    }

    private fun updateNotification(mediaItem: MediaItem) {
        val mediaMetadata = mediaItem.mediaMetadata
        val description = MediaDescriptionCompat.Builder()
            .setTitle(mediaMetadata.title)
            .setSubtitle(mediaMetadata.subtitle)
            .setDescription(mediaMetadata.description)
            .build()

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, description.title.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, description.subtitle.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, description.description.toString())
                .build()

        )

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


    private fun prepareAndPlay(songs: List<MediaMetadataCompat>, mediaId: String ?= null, playNow: Boolean) {
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
            exoPlayer.playWhenReady = playNow

            //update the current playing song
            curPlayingSong = itemToPlay

        }

    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventlistener)
        exoPlayer.release()


    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){
            MEDIA_ROOT_ID -> {
                val resultSent = firebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            prepareAndPlay(firebaseMusicSource.songs, firebaseMusicSource.songs[0].toString(), false)
                            isPlayerInitialized = true
                        }

                    }else{
                        result.sendResult(null)
                    }

                }

                if (!resultSent){
                    result.detach()
                }

            }
        }

    }
}