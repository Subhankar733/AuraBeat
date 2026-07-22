package com.subho.aurabeat

import android.Manifest
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.subho.aurabeat.model.AudioItem
import com.subho.aurabeat.ui.MusicScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private val audioListState = mutableStateListOf<AudioItem>()
    private var currentPlayingSong by mutableStateOf<AudioItem?>(null)
    private var isPlayingState by mutableStateOf(false)
    private var currentPositionState by mutableLongStateOf(0L)
    private var durationState by mutableLongStateOf(0L)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadAudioFiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationState = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    playNextSong()
                }
            }
        })

        requestStoragePermission()

        setContent {
            // Periodic Position update for Seekbar
            LaunchedEffect(isPlayingState) {
                while (isPlayingState) {
                    currentPositionState = player.currentPosition.coerceAtLeast(0L)
                    delay(500)
                }
            }

            MusicScreen(
                audioList = audioListState,
                currentPlaying = currentPlayingSong,
                isPlaying = isPlayingState,
                currentPosition = currentPositionState,
                duration = durationState,
                onSongSelect = { audio -> playAudio(audio) },
                onPlayPauseToggle = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                },
                onNext = { playNextSong() },
                onPrevious = { playPreviousSong() },
                onSeekTo = { fraction ->
                    val targetMs = (fraction * durationState).toLong()
                    player.seekTo(targetMs)
                    currentPositionState = targetMs
                }
            )
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    private fun loadAudioFiles() {
        val list = mutableListOf<AudioItem>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val albumId = cursor.getLong(albumIdColumn)
                val contentUri = ContentUris.withAppendedId(uri, id)

                list.add(AudioItem(id, title, artist, contentUri, albumId))
            }
        }
        audioListState.clear()
        audioListState.addAll(list)
    }

    private fun playAudio(audio: AudioItem) {
        currentPlayingSong = audio
        val mediaItem = MediaItem.fromUri(audio.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun playNextSong() {
        if (audioListState.isEmpty()) return
        val currentIndex = audioListState.indexOf(currentPlayingSong)
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < audioListState.size) currentIndex + 1 else 0
        playAudio(audioListState[nextIndex])
    }

    private fun playPreviousSong() {
        if (audioListState.isEmpty()) return
        val currentIndex = audioListState.indexOf(currentPlayingSong)
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else audioListState.size - 1
        playAudio(audioListState[prevIndex])
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
