package com.example.data.model

data class ParsedFileInfo(
    val cleanTitle: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val resolution: String? = null,
    val codec: String? = null,
    val releaseGroup: String? = null,
    val detectedType: MediaType = MediaType.UNKNOWN
)
