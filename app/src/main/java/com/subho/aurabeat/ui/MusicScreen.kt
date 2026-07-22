package com.subho.aurabeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subho.aurabeat.model.Song
import java.util.concurrent.TimeUnit

@Composable
fun MusicScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit
) {
    var showPlayerScreen by remember { mutableStateOf(false) }

    // যদি কোনো গান সিলেক্ট করা থাকে এবং ইউজার প্লেয়ার স্ক্রিন দেখতে চায়
    if (showPlayerScreen && currentSong != null) {
        PlayerDetailScreen(
            currentSong = currentSong,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onBackClick = { showPlayerScreen = false },
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            onSeek = onSeek
        )
    } else {
        // মেইন গানের লিস্ট স্ক্রিন
        SongListScreen(
            songs = songs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = { song ->
                onSongClick(song)
                showPlayerScreen = true
            },
            onPlayPauseClick = onPlayPauseClick,
            onBarClick = { if (currentSong != null) showPlayerScreen = true }
        )
    }
}

@Composable
fun SongListScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF1E1B24),
        bottomBar = {
            if (currentSong != null) {
                // মিনি প্লেয়ার বার যা স্ক্রিনের নিচে সবসময় থাকবে
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBarClick() },
                    color = Color(0xFF2B2930),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF6750A4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = currentSong.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(text = currentSong.artist, color = Color(0xFFCAC4D0), fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFFD0BCFF)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "AuraBeat Music",
                color = Color(0xFFD0BCFF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs) { song ->
                    val isSelected = song == currentSong
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF49454F) else Color(0xFF2B2930)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = song.artist,
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerDetailScreen(
    currentSong: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0f) }

    LaunchedEffect(currentPosition) {
        if (!isSeeking) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1B24)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Now Playing",
                    color = Color(0xFFD0BCFF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Album Art
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF6750A4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Album Art",
                    tint = Color.White,
                    modifier = Modifier.size(100.dp)
                )
            }

            // Song Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentSong.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong.artist,
                    color = Color(0xFFCAC4D0),
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            // Progress Slider and Time
            Column(modifier = Modifier.fillMaxWidth()) {
                val maxLimit = if (duration > 0f) duration.toFloat() else 1f

                Slider(
                    value = sliderPosition.coerceIn(0f, maxLimit),
                    onValueChange = { newVal ->
                        isSeeking = true
                        sliderPosition = newVal
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onSeek(sliderPosition)
                    },
                    valueRange = 0f..maxLimit,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFD0BCFF),
                        activeTrackColor = Color(0xFFD0BCFF),
                        inactiveTrackColor = Color(0xFF49454F)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(sliderPosition.toLong()), color = Color(0xFFCAC4D0), fontSize = 12.sp)
                    Text(text = formatTime(duration), color = Color(0xFFCAC4D0), fontSize = 12.sp)
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousClick) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }

                FloatingActionButton(
                    onClick = onPlayPauseClick,
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%d:%02d", minutes, seconds)
}
