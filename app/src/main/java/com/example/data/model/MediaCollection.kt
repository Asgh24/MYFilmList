package com.example.data.model

data class MediaCollection(
    val collectionKey: String,
    val title: String,
    val mediaType: MediaType,
    val items: List<MediaItem>,
    val primaryMetadata: MediaMetadata?,
    val needsReview: Boolean = false,
    val candidateMatches: List<CandidateMatch> = emptyList()
) {
    val totalCount: Int get() = items.size

    val posterUrl: String?
        get() = primaryMetadata?.posterUrl
            ?: items.firstOrNull { !it.metadata?.posterUrl.isNullOrEmpty() }?.metadata?.posterUrl

    val bannerUrl: String?
        get() = primaryMetadata?.bannerUrl
            ?: items.firstOrNull { !it.metadata?.bannerUrl.isNullOrEmpty() }?.metadata?.bannerUrl

    val synopsis: String?
        get() = primaryMetadata?.synopsis
            ?: items.firstOrNull { !it.metadata?.synopsis.isNullOrEmpty() }?.metadata?.synopsis

    val rating: Double?
        get() = primaryMetadata?.rating
            ?: items.firstOrNull { it.metadata?.rating != null }?.metadata?.rating

    val scoreSource: String?
        get() = primaryMetadata?.scoreSource
            ?: items.firstOrNull { !it.metadata?.scoreSource.isNullOrEmpty() }?.metadata?.scoreSource
            ?: "AniList"

    val isFavorite: Boolean get() = items.any { it.isFavorite }
    val isWatched: Boolean get() = items.isNotEmpty() && items.all { it.isWatched }
}
