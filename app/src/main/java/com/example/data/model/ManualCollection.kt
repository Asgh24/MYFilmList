package com.example.data.model

/**
 * A single episode row entered by the user in the "Add collection manually" sheet.
 * If [fileName] is blank the title of the collection is used as the file name.
 */
data class ManualEpisode(
    val season: Int? = null,
    val episode: Int? = null,
    val fileName: String = ""
)

/**
 * Fully user-specified collection draft. [episodes] may be empty, in which case
 * a single collection entry is created from the title.
 */
data class ManualCollectionInput(
    val title: String,
    val mediaType: MediaType,
    val synopsis: String = "",
    val posterUrl: String = "",
    val episodes: List<ManualEpisode> = emptyList()
)

/**
 * Result of inserting a manual collection: the DB ids of the created items plus
 * the cleaned titles used so the ViewModel can register them for smart grouping.
 */
data class ManualCollectionResult(
    val ids: List<String>,
    val cleanTitles: List<String>
)
