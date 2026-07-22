package com.subho.aurabeat.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import com.subho.aurabeat.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalMusicPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private val _audioList = MutableStateFlow<List<Song>>(emptyList())
    val audioList: StateFlow<List<Song>> = _audioList

    private val _currentPlaying = MutableStateFlow<Song?>(null)
    val currentPlaying: StateFlow<Song?> = _currentPlaying

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    fun loadAudioFiles() {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val data = cursor.getString(dataColumn) ?: ""
                val duration = cursor.getLong(durationColumn)

                songs.add(Song(id, title, artist, data, duration))
            }
        }
        _audioList.value = songs
    }

    fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, Uri.parse(song.data)).apply {
            start()
            setOnCompletionListener { playNext() }
        }
        _currentPlaying.value = song
        _isPlaying.value = true
        _duration.value = (mediaPlayer?.duration ?: 0).toLong()
        _currentPosition.value = 0L
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.start()
                _isPlaying.value = true
            }
        } ?: run {
            val list = _audioList.value
            if (list.isNotEmpty()) {
                playSong(list[0])
            }
        }
    }

    fun playNext() {
        val list = _audioList.value
        val current = _currentPlaying.value
        if (list.isNotEmpty()) {
            val currentIndex = if (current != null) list.indexOf(current) else -1
            val nextIndex = if (currentIndex < list.size - 1) currentIndex + 1 else 0
            playSong(list[nextIndex])
        }
    }

    fun playPrevious() {
        val list = _audioList.value
        val current = _currentPlaying.value
        if (list.isNotEmpty()) {
            val currentIndex = if (current != null) list.indexOf(current) else 0
            val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
            playSong(list[prevIndex])
        }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
