package com.example.data.model

data class MediaItem(
    val id: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val parsedInfo: ParsedFileInfo,
    val metadata: MediaMetadata? = null,
    val isWatched: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val isFavorite: Boolean = false
)
