package com.subho.aurabeat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.subho.aurabeat.model.AudioItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    audioList: List<AudioItem>,
    currentPlaying: AudioItem?,
    isPlaying: Boolean,
    onSongSelect: (AudioItem) -> Unit,
    onPlayPauseToggle: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AuraBeat Music") }
            )
        },
        bottomBar = {
            currentPlaying?.let { song ->
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                            Text(text = song.artist, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onPlayPauseToggle) {
                            Text(if (isPlaying) "Pause" else "Play")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(audioList) { audio ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongSelect(audio) }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = audio.title, style = MaterialTheme.typography.bodyLarge)
                        Text(text = audio.artist, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
