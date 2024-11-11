package com.example.spotifyclone.conv

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.MediaItem


fun MediaMetadataCompat.toMediaItem(): MediaItem {
    val mediaUri = this.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)?.let { Uri.parse(it) }
    val title = this.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
    val artist = this.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
    val album = this.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

    return MediaItem.Builder().apply {
        setUri(mediaUri)
        setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
    }.build()
}
