package com.subho.aurabeat.model

import android.net.Uri

data class AudioItem(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri
)
