package com.subho.aurabeat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.subho.aurabeat.player.LocalMusicPlayer
import com.subho.aurabeat.ui.MusicScreen
import com.subho.aurabeat.ui.theme.AuraBeatTheme

class MainActivity : ComponentActivity() {

    private lateinit var musicPlayer: LocalMusicPlayer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            musicPlayer.loadAudioFiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicPlayer = LocalMusicPlayer(applicationContext)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            musicPlayer.loadAudioFiles()
        } else {
            requestPermissionLauncher.launch(permission)
        }

        setContent {
            AuraBeatTheme {
                val songs by musicPlayer.audioList.collectAsState()
                val currentSong by musicPlayer.currentPlaying.collectAsState()
                val isPlaying by musicPlayer.isPlaying.collectAsState()
                val currentPosition by musicPlayer.currentPosition.collectAsState()
                val duration by musicPlayer.duration.collectAsState()

                MusicScreen(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onSongClick = { song -> musicPlayer.playSong(song) },
                    onPlayPauseClick = { musicPlayer.togglePlayPause() },
                    onNextClick = { musicPlayer.playNext() },
                    onPreviousClick = { musicPlayer.playPrevious() },
                    onSeek = { progress -> musicPlayer.seekTo(progress.toLong()) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }
}
