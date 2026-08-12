package com.example.data.model

data class CandidateMatch(
    val title: String,
    val englishTitle: String? = null,
    val romajiTitle: String? = null,
    val releaseYear: Int? = null,
    val mediaType: MediaType = MediaType.ANIME,
    val posterUrl: String? = null,
    val synopsis: String? = null,
    val rating: Double? = null,
    val scoreSource: String = "Gemini AI",
    val explanation: String? = null
)
