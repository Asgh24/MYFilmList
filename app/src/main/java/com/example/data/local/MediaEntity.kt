package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey val id: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val cleanTitle: String,
    val season: Int?,
    val episode: Int?,
    val year: Int?,
    val resolution: String?,
    val codec: String?,
    val releaseGroup: String?,
    val detectedType: String,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val synopsis: String?,
    val posterUrl: String?,
    val bannerUrl: String?,
    val rating: Double?,
    val scoreSource: String?,
    val genresJson: String?,
    val releaseYear: Int?,
    val totalEpisodes: Int?,
    val isWatched: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val needsReview: Boolean = false,
    val candidatesJson: String? = null
)
