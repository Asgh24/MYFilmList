package com.example.data.model

data class MediaMetadata(
    val titleRomaji: String? = null,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val synopsis: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val rating: Double? = null,
    val scoreSource: String = "AniList", // "AniList", "MAL", "TMDB", "Gemini"
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val totalEpisodes: Int? = null,
    val status: String? = null
)

data class RecommendationItem(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double?,
    val scoreSource: String,
    val synopsis: String?,
    val genres: List<String>,
    val type: MediaType
)
