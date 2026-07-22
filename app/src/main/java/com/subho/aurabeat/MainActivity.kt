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
import androidx.media3.exoplayer.ExoPlayer
import com.subho.aurabeat.model.AudioItem
import com.subho.aurabeat.ui.MusicScreen

class MainActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer
    private val audioListState = mutableStateListOf<AudioItem>()
    private var currentPlayingSong by mutableStateOf<AudioItem?>(null)
    private var isPlayingState by mutableStateOf(false)

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

        requestStoragePermission()

        setContent {
            MusicScreen(
                audioList = audioListState,
                currentPlaying = currentPlayingSong,
                isPlaying = isPlayingState,
                onSongSelect = { audio ->
                    playAudio(audio)
                },
                onPlayPauseToggle = {
                    if (player.isPlaying) {
                        player.pause()
                        isPlayingState = false
                    } else {
                        player.play()
                        isPlayingState = true
                    }
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
            MediaStore.Audio.Media.ARTIST
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val contentUri = ContentUris.withAppendedId(uri, id)

                list.add(AudioItem(id, title, artist, contentUri))
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
        isPlayingState = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
