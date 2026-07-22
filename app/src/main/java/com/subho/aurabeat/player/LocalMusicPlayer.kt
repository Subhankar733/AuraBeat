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

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

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
        _duration.value = mediaPlayer?.duration ?: 0
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
        }
    }

    fun playNext() {
        val list = _audioList.value
        val current = _currentPlaying.value
        if (list.isNotEmpty() && current != null) {
            val currentIndex = list.indexOf(current)
            val nextIndex = (currentIndex + 1) % list.size
            playSong(list[nextIndex])
        }
    }

    fun playPrevious() {
        val list = _audioList.value
        val current = _currentPlaying.value
        if (list.isNotEmpty() && current != null) {
            val currentIndex = list.indexOf(current)
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playSong(list[prevIndex])
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
