package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MediaEntity
import com.example.data.model.MediaItem
import com.example.data.model.MediaMetadata
import com.example.data.model.MediaType
import com.example.data.model.ParsedFileInfo
import com.example.data.model.RecommendationItem
import com.example.data.remote.AniListClient
import com.example.data.remote.GeminiMetadataService
import com.example.data.remote.PublicMediaApiClient
import com.example.data.scanner.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val mediaDao = db.mediaDao()
    private val scanner = MediaScanner(context)

    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun scanAndEnrichMedia(
        customFolderPath: String? = null,
        includeDemoFallback: Boolean = false,
        targetLanguage: String = "English",
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val scannedFiles = scanner.scanLocalMediaFiles(
            customFolderPath = customFolderPath,
            includeDemoFallback = includeDemoFallback
        )
        val total = scannedFiles.size
        if (total == 0) {
            onProgress(0, 0)
            return@withContext
        }

        // 1. FAST INSTANT INSERT: Create initial entities and insert into DB immediately so UI shows files instantly!
        val initialEntities = scannedFiles.map { item ->
            val existing = mediaDao.getMediaById(item.id)
            if (existing != null) {
                existing
            } else {
                item.toEntity(null)
            }
        }
        mediaDao.insertAll(initialEntities)
        onProgress(total, total)

        // 2. PARALLEL ENRICHMENT: Identify items missing poster/synopsis and fetch metadata in parallel batches
        val itemsToEnrich = initialEntities.filter { entity ->
            val hasStockPoster = entity.posterUrl?.contains("unsplash.com") == true || entity.posterUrl == null
            entity.synopsis == null || hasStockPoster
        }

        if (itemsToEnrich.isEmpty()) return@withContext

        val semaphore = kotlinx.coroutines.sync.Semaphore(5)
        kotlinx.coroutines.coroutineScope {
            itemsToEnrich.map { entity ->
                async {
                    semaphore.withPermit {
                        val cleanTitle = entity.cleanTitle
                        val mediaType = try { MediaType.valueOf(entity.detectedType) } catch (e: Exception) { MediaType.UNKNOWN }

                        var fetchedMetadata: MediaMetadata? = null

                        // 1. If Gemini AI Key is active, query Gemini FIRST for intelligent classification and Persian synopsis!
                        if (GeminiMetadataService.getApiKey().isNotBlank()) {
                            val geminiResult = GeminiMetadataService.fetchMetadataAndRecommendations(
                                rawTitle = cleanTitle,
                                mediaType = mediaType,
                                targetLanguage = targetLanguage
                            )
                            if (geminiResult != null && !geminiResult.metadata.synopsis.isNullOrBlank()) {
                                fetchedMetadata = geminiResult.metadata
                            }
                        }

                        // 2. If Gemini is not active or returned null, query Public Media API (which prioritizes AniList/MAL for anime)
                        if (fetchedMetadata == null) {
                            val publicResult = PublicMediaApiClient.fetchAccurateMetadata(
                                cleanTitle = cleanTitle,
                                mediaType = mediaType,
                                targetLanguage = targetLanguage
                            )
                            if (publicResult != null) {
                                fetchedMetadata = publicResult.metadata
                            }
                        }

                        // 3. Additional fallback to AniListClient if media is Anime
                        if (fetchedMetadata == null && (mediaType == MediaType.ANIME || com.example.data.parser.FileNameParser.isAnimeCandidate(cleanTitle))) {
                            val aniResult = AniListClient.fetchAnimeMetadata(cleanTitle)
                            if (aniResult != null) {
                                fetchedMetadata = aniResult.metadata
                            }
                        }

                        if (fetchedMetadata != null) {
                            val updatedEntity = entity.copy(
                                titleRomaji = fetchedMetadata.titleRomaji ?: entity.titleRomaji,
                                titleEnglish = fetchedMetadata.titleEnglish ?: entity.titleEnglish,
                                titleNative = fetchedMetadata.titleNative ?: entity.titleNative,
                                synopsis = fetchedMetadata.synopsis ?: entity.synopsis,
                                posterUrl = fetchedMetadata.posterUrl ?: entity.posterUrl,
                                bannerUrl = fetchedMetadata.bannerUrl ?: entity.bannerUrl,
                                rating = fetchedMetadata.rating ?: entity.rating,
                                scoreSource = fetchedMetadata.scoreSource ?: entity.scoreSource,
                                genresJson = fetchedMetadata.genres.joinToString(",").ifEmpty { entity.genresJson },
                                releaseYear = fetchedMetadata.releaseYear ?: entity.releaseYear,
                                totalEpisodes = fetchedMetadata.totalEpisodes ?: entity.totalEpisodes
                            )
                            mediaDao.insertOrUpdate(updatedEntity)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getRecommendationsForMedia(
        item: MediaItem,
        targetLanguage: String = "English",
        source: String = "AI"
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        if (source == "AI") {
            val geminiResult = GeminiMetadataService.fetchMetadataAndRecommendations(
                rawTitle = item.parsedInfo.cleanTitle,
                mediaType = item.parsedInfo.detectedType,
                targetLanguage = targetLanguage
            )
            val aiRecs = geminiResult?.recommendations ?: emptyList()
            if (aiRecs.isNotEmpty()) {
                return@withContext aiRecs
            }
            return@withContext emptyList()
        }

        // --- Reference API Mode ("سایت‌های مرجع") ---
        val cleanTitle = item.parsedInfo.cleanTitle

        // 1. Try AniList (highest quality anime & animation data)
        val aniResult = AniListClient.fetchAnimeMetadata(cleanTitle)
        if (aniResult != null && aniResult.recommendations.isNotEmpty()) {
            return@withContext aniResult.recommendations
        }

        // 2. Try TVMaze / iTunes / MyAnimeList open APIs
        val publicResult = PublicMediaApiClient.fetchAccurateMetadata(
            cleanTitle = cleanTitle,
            mediaType = item.parsedInfo.detectedType,
            targetLanguage = targetLanguage
        )
        if (publicResult != null && publicResult.recommendations.isNotEmpty()) {
            return@withContext publicResult.recommendations
        }

        // 3. Fallback to curated reference site recommendations (AniList/TVMaze/TMDB)
        val curated = PublicMediaApiClient.getCuratedRecommendations(cleanTitle)
        if (curated.isNotEmpty()) {
            return@withContext curated
        }

        emptyList()
    }

    suspend fun updateWatchStatus(id: String, isWatched: Boolean, positionMs: Long) {
        mediaDao.updateWatchStatus(id, isWatched, positionMs)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        mediaDao.updateFavorite(id, isFavorite)
    }

    suspend fun clearDatabase() {
        mediaDao.clearAll()
    }

    suspend fun deleteMediaItem(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteById(id)
    }

    suspend fun deleteCollection(itemIds: List<String>) = withContext(Dispatchers.IO) {
        if (itemIds.isNotEmpty()) {
            mediaDao.deleteByIds(itemIds)
            // Clear Coil image cache if possible to free up disk space
            try {
                coil.Coil.imageLoader(context).diskCache?.clear()
            } catch (e: Exception) {
                // Ignore if cache clearing fails
            }
        }
    }

    suspend fun updateCollectionMetadata(
        itemIds: List<String>,
        newTitle: String,
        newType: MediaType,
        metadata: MediaMetadata
    ) = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext
        mediaDao.updateCollectionMetadata(
            ids = itemIds,
            newTitle = newTitle,
            newType = newType.name,
            titleEng = metadata.titleEnglish ?: newTitle,
            titleRom = metadata.titleRomaji ?: newTitle,
            synopsis = metadata.synopsis,
            poster = metadata.posterUrl,
            rating = metadata.rating,
            genres = metadata.genres.joinToString(",")
        )
    }

    suspend fun applyRecategorizations(results: List<com.example.data.remote.FileRecategorizationResult>) = withContext(Dispatchers.IO) {
        val entitiesToUpdate = mutableListOf<MediaEntity>()
        results.forEach { res ->
            val existing = mediaDao.getMediaById(res.mediaItemId)
            if (existing != null) {
                val updated = existing.copy(
                    cleanTitle = res.correctedTitle,
                    detectedType = res.correctedType.name,
                    season = res.season ?: existing.season,
                    episode = res.episode ?: existing.episode
                )
                entitiesToUpdate.add(updated)
            }
        }
        if (entitiesToUpdate.isNotEmpty()) {
            mediaDao.insertAll(entitiesToUpdate)
        }
    }

    // Playback Intent Launcher (Requirement #5)
    fun createPlayIntent(filePath: String, targetPackage: String? = null): Intent {
        val uri: Uri = if (filePath.startsWith("content://")) {
            Uri.parse(filePath)
        } else {
            val file = File(filePath)
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!targetPackage.isNull_or_empty()) {
                setPackage(targetPackage)
            }
        }
        return intent
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    private fun MediaItem.toEntity(meta: MediaMetadata?): MediaEntity {
        return MediaEntity(
            id = id,
            filePath = filePath,
            fileName = fileName,
            fileSize = fileSize,
            lastModified = lastModified,
            cleanTitle = parsedInfo.cleanTitle,
            season = parsedInfo.season,
            episode = parsedInfo.episode,
            year = parsedInfo.year,
            resolution = parsedInfo.resolution,
            codec = parsedInfo.codec,
            releaseGroup = parsedInfo.releaseGroup,
            detectedType = parsedInfo.detectedType.name,
            titleRomaji = meta?.titleRomaji,
            titleEnglish = meta?.titleEnglish,
            titleNative = meta?.titleNative,
            synopsis = meta?.synopsis,
            posterUrl = meta?.posterUrl,
            bannerUrl = meta?.bannerUrl,
            rating = meta?.rating,
            scoreSource = meta?.scoreSource,
            genresJson = meta?.genres?.joinToString(","),
            releaseYear = meta?.releaseYear,
            totalEpisodes = meta?.totalEpisodes,
            isWatched = isWatched,
            playbackPositionMs = playbackPositionMs,
            isFavorite = isFavorite
        )
    }

    private fun MediaEntity.toDomainModel(): MediaItem {
        val parsed = ParsedFileInfo(
            cleanTitle = cleanTitle,
            season = season,
            episode = episode,
            year = year,
            resolution = resolution,
            codec = codec,
            releaseGroup = releaseGroup,
            detectedType = try { MediaType.valueOf(detectedType) } catch (e: Exception) { MediaType.UNKNOWN }
        )

        val metadata = if (synopsis != null || posterUrl != null) {
            MediaMetadata(
                titleRomaji = titleRomaji,
                titleEnglish = titleEnglish,
                titleNative = titleNative,
                synopsis = synopsis,
                posterUrl = posterUrl,
                bannerUrl = bannerUrl,
                rating = rating,
                scoreSource = scoreSource ?: "AniList",
                genres = genresJson?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                releaseYear = releaseYear,
                totalEpisodes = totalEpisodes
            )
        } else null

        return MediaItem(
            id = id,
            filePath = filePath,
            fileName = fileName,
            fileSize = fileSize,
            lastModified = lastModified,
            parsedInfo = parsed,
            metadata = metadata,
            isWatched = isWatched,
            playbackPositionMs = playbackPositionMs,
            isFavorite = isFavorite,
            needsReview = needsReview,
            candidateMatches = deserializeCandidateMatches(candidatesJson)
        )
    }

    suspend fun updateCollectionReviewStatus(
        itemIds: List<String>,
        needsReview: Boolean,
        candidates: List<com.example.data.model.CandidateMatch>
    ) = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext
        val json = if (candidates.isNotEmpty()) serializeCandidateMatches(candidates) else null
        mediaDao.updateCollectionReviewStatus(itemIds, needsReview, json)
    }

    suspend fun fetchCandidatesForCollection(
        collectionTitle: String,
        mediaType: MediaType,
        targetLanguage: String = "Persian"
    ): List<com.example.data.model.CandidateMatch> = withContext(Dispatchers.IO) {
        GeminiMetadataService.fetchCandidateMatchesForTitle(collectionTitle, mediaType, targetLanguage)
    }

    private fun serializeCandidateMatches(candidates: List<com.example.data.model.CandidateMatch>): String {
        val arr = org.json.JSONArray()
        candidates.forEach { c ->
            val obj = org.json.JSONObject()
            obj.put("title", c.title)
            obj.put("englishTitle", c.englishTitle)
            obj.put("romajiTitle", c.romajiTitle)
            obj.put("releaseYear", c.releaseYear)
            obj.put("mediaType", c.mediaType.name)
            obj.put("posterUrl", c.posterUrl)
            obj.put("synopsis", c.synopsis)
            obj.put("rating", c.rating)
            obj.put("scoreSource", c.scoreSource)
            obj.put("explanation", c.explanation)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeCandidateMatches(jsonStr: String?): List<com.example.data.model.CandidateMatch> {
        if (jsonStr.isNull_or_empty()) return emptyList()
        val result = mutableListOf<com.example.data.model.CandidateMatch>()
        try {
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val typeStr = obj.optString("mediaType", "ANIME")
                val mType = try { MediaType.valueOf(typeStr) } catch (e: Exception) { MediaType.ANIME }
                result.add(
                    com.example.data.model.CandidateMatch(
                        title = obj.optString("title", ""),
                        englishTitle = obj.optString("englishTitle", null),
                        romajiTitle = obj.optString("romajiTitle", null),
                        releaseYear = if (obj.has("releaseYear") && !obj.isNull("releaseYear")) obj.getInt("releaseYear") else null,
                        mediaType = mType,
                        posterUrl = obj.optString("posterUrl", null),
                        synopsis = obj.optString("synopsis", null),
                        rating = if (obj.has("rating") && !obj.isNull("rating")) obj.getDouble("rating") else null,
                        scoreSource = obj.optString("scoreSource", "Gemini AI"),
                        explanation = obj.optString("explanation", null)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
