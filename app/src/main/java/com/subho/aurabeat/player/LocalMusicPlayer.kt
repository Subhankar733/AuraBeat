package com.subho.aurabeat.player

import android.content.Context
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import com.subho.aurabeat.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode {
    OFF, ALL, ONE
}

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

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private var job: Job? = null
    private var sleepTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun loadAudioFiles() {
        val songs = mutableListOf<Song>()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media._TITLE,
            MediaStore.Audio.Media._ARTIST,
            MediaStore.Audio.Media._DATA,
            MediaStore.Audio.Media._DURATION
        )

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, MediaStore.Audio.Media.TITLE + " ASC")
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ARTIST)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._DATA)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown"
                val artist = it.getString(artistCol) ?: "Unknown"
                val path = it.getString(dataCol)
                val dur = it.getLong(durationCol)
                songs.add(Song(id, title, artist, path, dur))
            }
        }
        _audioList.value = songs
    }

    fun playSong(song: Song) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }
        _currentPlaying.value = song
        _duration.value = mediaPlayer?.duration?.toLong() ?: song.duration
        _isPlaying.value = true
        startProgressTracking()

        mediaPlayer?.setOnCompletionListener {
            when (_repeatMode.value) {
                RepeatMode.ONE -> playSong(song)
                RepeatMode.ALL -> playNext()
                RepeatMode.OFF -> {
                    val list = _audioList.value
                    if (list.isNotEmpty() && list.last() == song) {
                        _isPlaying.value = false
                        _currentPosition.value = 0L
                    } else {
                        playNext()
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                job?.cancel()
            } else {
                it.start()
                _isPlaying.value = true
                startProgressTracking()
            }
        } ?: run {
            if (_audioList.value.isNotEmpty()) {
                playSong(_audioList.value[0])
            }
        }
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setSleepTimer(minutes: Long) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) return
        sleepTimerJob = scope.launch {
            delay(minutes * 60 * 1000L)
            if (_isPlaying.value) {
                togglePlayPause()
            }
        }
    }

    private fun startProgressTracking() {
        job?.cancel()
        job = scope.launch {
            while (isActive && mediaPlayer != null) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        _currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500L)
            }
        }
    }

    fun playNext() {
        val list = _audioList.value
        val current = _currentPlaying.value
        if (list.isNotEmpty()) {
            if (_isShuffleEnabled.value) {
                val randomSong = list.random()
                playSong(randomSong)
            } else if (current != null) {
                val currentIndex = list.indexOf(current)
                val nextIndex = (currentIndex + 1) % list.size
                playSong(list[nextIndex])
            } else {
                playSong(list[0])
            }
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

    fun release() {
        job?.cancel()
        sleepTimerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        scope.cancel()
    }
}
