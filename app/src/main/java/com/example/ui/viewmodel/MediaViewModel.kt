package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MediaCollection
import com.example.data.model.MediaItem
import com.example.data.model.MediaMetadata
import com.example.data.model.MediaType
import com.example.data.model.RecommendationItem
import com.example.data.repository.MediaRepository
import com.example.data.remote.GeminiMetadataService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppThemeMode

sealed class KeyTestStatus {
    object Idle : KeyTestStatus()
    object Testing : KeyTestStatus()
    data class Success(val message: String) : KeyTestStatus()
    data class Error(val errorMessage: String) : KeyTestStatus()
}

data class ProposedCluster(
    val canonicalTitle: String,
    val items: List<MediaItem>,
    val detectedSitesRemoved: List<String>
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val prefs = application.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow(MediaType.ALL)
    val targetLanguage = MutableStateFlow("English")
    val uiLanguage = MutableStateFlow<AppLanguage>(
        if (prefs.getString("ui_language", "fa") == "en") AppLanguage.ENGLISH else AppLanguage.PERSIAN
    )

    fun setUiLanguage(lang: AppLanguage) {
        uiLanguage.value = lang
        prefs.edit().putString("ui_language", lang.code).apply()
    }

    val themeMode = MutableStateFlow<AppThemeMode>(
        when (prefs.getString("app_theme_mode", "system")) {
            "dark" -> AppThemeMode.DARK
            "light" -> AppThemeMode.LIGHT
            else -> AppThemeMode.SYSTEM
        }
    )

    fun setThemeMode(mode: AppThemeMode) {
        themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.code).apply()
    }

    val geminiApiKey = MutableStateFlow("")
    val keyTestStatus = MutableStateFlow<KeyTestStatus>(KeyTestStatus.Idle)

    val tmdbApiKey = MutableStateFlow("")
    val tmdbKeyTestStatus = MutableStateFlow<KeyTestStatus>(KeyTestStatus.Idle)

    val omdbApiKey = MutableStateFlow("")
    val omdbKeyTestStatus = MutableStateFlow<KeyTestStatus>(KeyTestStatus.Idle)

    val selectedFileIds = MutableStateFlow<Set<String>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)
    val isAnalyzingSelection = MutableStateFlow(false)
    val recategorizationResults = MutableStateFlow<List<com.example.data.remote.FileRecategorizationResult>>(emptyList())
    val showRecategorizationSheet = MutableStateFlow(false)

    val showApiKeyOnboardingSheet = MutableStateFlow(false)

    val showCollectionAiEditSheet = MutableStateFlow(false)
    val isAnalyzingCollection = MutableStateFlow(false)
    val proposedCollectionMetadata = MutableStateFlow<com.example.data.model.MediaMetadata?>(null)

    val isScanning = MutableStateFlow(false)
    val isGroupingWithAi = MutableStateFlow(false)
    val scanProgress = MutableStateFlow(0 to 0)

    init {
        val savedKey = prefs.getString("user_gemini_key", "") ?: ""
        if (savedKey.isNotBlank()) {
            geminiApiKey.value = savedKey
            GeminiMetadataService.userApiKey = savedKey
            keyTestStatus.value = KeyTestStatus.Success("✓ کلید Gemini API معتبر است و در حافظه فعال می‌باشد.")
        } else {
            val hasDismissed = prefs.getBoolean("has_dismissed_api_prompt", false)
            if (!hasDismissed) {
                showApiKeyOnboardingSheet.value = true
            }
        }

        val savedTmdb = prefs.getString("user_tmdb_key", "") ?: ""
        if (savedTmdb.isNotBlank()) {
            tmdbApiKey.value = savedTmdb
            com.example.data.remote.PublicMediaApiClient.tmdbApiKey = savedTmdb
            tmdbKeyTestStatus.value = KeyTestStatus.Success("✓ کلید TMDB API فعال است.")
        }

        val savedOmdb = prefs.getString("user_omdb_key", "") ?: ""
        if (savedOmdb.isNotBlank()) {
            omdbApiKey.value = savedOmdb
            com.example.data.remote.PublicMediaApiClient.omdbApiKey = savedOmdb
            omdbKeyTestStatus.value = KeyTestStatus.Success("✓ کلید OMDb API فعال است.")
        }
    }

    fun dismissApiKeyOnboarding() {
        showApiKeyOnboardingSheet.value = false
        prefs.edit().putBoolean("has_dismissed_api_prompt", true).apply()
    }

    fun openApiKeyOnboarding() {
        showApiKeyOnboardingSheet.value = true
    }

    fun saveAndTestGeminiApiKey(keyInput: String) {
        viewModelScope.launch {
            val trimmed = keyInput.trim()
            if (trimmed.isBlank()) {
                keyTestStatus.value = KeyTestStatus.Error("لطفاً ابتدا کلید API Gemini خود را وارد کنید.")
                return@launch
            }

            keyTestStatus.value = KeyTestStatus.Testing
            val result = GeminiMetadataService.testGeminiApiKey(trimmed)

            if (result.isSuccess) {
                geminiApiKey.value = trimmed
                GeminiMetadataService.userApiKey = trimmed
                prefs.edit().putString("user_gemini_key", trimmed).apply()
                prefs.edit().putBoolean("has_dismissed_api_prompt", true).apply()
                val activeModel = GeminiMetadataService.getCachedWorkingModelName()
                keyTestStatus.value = KeyTestStatus.Success("✓ کلید Gemini معتبر است. مدل فعال: $activeModel")
                showApiKeyOnboardingSheet.value = false
            } else {
                val err = result.exceptionOrNull()?.message ?: "خطای ناشناخته"
                keyTestStatus.value = KeyTestStatus.Error("❌ کلید نامعتبر است: $err")
            }
        }
    }

    fun clearGeminiApiKey() {
        geminiApiKey.value = ""
        GeminiMetadataService.userApiKey = ""
        prefs.edit().remove("user_gemini_key").apply()
        keyTestStatus.value = KeyTestStatus.Idle
    }

    fun saveAndTestTmdbApiKey(keyInput: String) {
        viewModelScope.launch {
            val trimmed = keyInput.trim()
            if (trimmed.isBlank()) {
                tmdbKeyTestStatus.value = KeyTestStatus.Error("لطفاً کلید API TMDB را وارد کنید.")
                return@launch
            }

            tmdbKeyTestStatus.value = KeyTestStatus.Testing
            val res = com.example.data.remote.PublicMediaApiClient.testTmdbApiKey(trimmed)

            if (res.isSuccess) {
                tmdbApiKey.value = trimmed
                com.example.data.remote.PublicMediaApiClient.tmdbApiKey = trimmed
                prefs.edit().putString("user_tmdb_key", trimmed).apply()
                tmdbKeyTestStatus.value = KeyTestStatus.Success("✓ کلید TMDB API با موفقیت تأیید شد.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "خطا در بررسی کلید"
                tmdbKeyTestStatus.value = KeyTestStatus.Error("❌ کلید TMDB نامعتبر است: $err")
            }
        }
    }

    fun clearTmdbApiKey() {
        tmdbApiKey.value = ""
        com.example.data.remote.PublicMediaApiClient.tmdbApiKey = ""
        prefs.edit().remove("user_tmdb_key").apply()
        tmdbKeyTestStatus.value = KeyTestStatus.Idle
    }

    fun saveAndTestOmdbApiKey(keyInput: String) {
        viewModelScope.launch {
            val trimmed = keyInput.trim()
            if (trimmed.isBlank()) {
                omdbKeyTestStatus.value = KeyTestStatus.Error("لطفاً کلید API OMDb را وارد کنید.")
                return@launch
            }

            omdbKeyTestStatus.value = KeyTestStatus.Testing
            val res = com.example.data.remote.PublicMediaApiClient.testOmdbApiKey(trimmed)

            if (res.isSuccess) {
                omdbApiKey.value = trimmed
                com.example.data.remote.PublicMediaApiClient.omdbApiKey = trimmed
                prefs.edit().putString("user_omdb_key", trimmed).apply()
                omdbKeyTestStatus.value = KeyTestStatus.Success("✓ کلید OMDb API با موفقیت تأیید شد.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "خطا در بررسی کلید"
                omdbKeyTestStatus.value = KeyTestStatus.Error("❌ کلید OMDb نامعتبر است: $err")
            }
        }
    }

    fun clearOmdbApiKey() {
        omdbApiKey.value = ""
        com.example.data.remote.PublicMediaApiClient.omdbApiKey = ""
        prefs.edit().remove("user_omdb_key").apply()
        omdbKeyTestStatus.value = KeyTestStatus.Idle
    }

    suspend fun askGeminiCustomPrompt(promptInput: String): String {
        val result = GeminiMetadataService.generateCustomPromptResponse(
            prompt = promptInput,
            targetLanguage = if (uiLanguage.value == AppLanguage.PERSIAN) "Persian" else "English"
        )
        return if (result.isSuccess) {
            result.getOrDefault("")
        } else {
            result.exceptionOrNull()?.message ?: "اتصال به هوش مصنوعی برقرار نشد: خطای ناشناخته"
        }
    }

    val aiCanonicalTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val proposedClusters = MutableStateFlow<List<ProposedCluster>>(emptyList())
    val showAiConfirmationSheet = MutableStateFlow(false)

    val selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedCollection = MutableStateFlow<MediaCollection?>(null)
    val recommendations = MutableStateFlow<List<RecommendationItem>>(emptyList())
    val isLoadingRecommendations = MutableStateFlow(false)

    val mediaItems: StateFlow<List<MediaItem>> = combine(
        repository.allMediaItems,
        searchQuery,
        selectedCategory
    ) { items, query, category ->
        items.filter { item ->
            val matchesCategory = category == MediaType.ALL || item.parsedInfo.detectedType == category
            val matchesQuery = query.isBlank() ||
                    item.parsedInfo.cleanTitle.contains(query, ignoreCase = true) ||
                    item.fileName.contains(query, ignoreCase = true) ||
                    (item.metadata?.genres?.any { it.contains(query, ignoreCase = true) } == true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaCollections: StateFlow<List<MediaCollection>> = combine(
        repository.allMediaItems,
        searchQuery,
        selectedCategory,
        aiCanonicalTitles
    ) { items, query, category, aiMap ->
        val grouped = items.groupBy { item ->
            val clean = item.parsedInfo.cleanTitle.trim()
            val mapped = aiMap[clean] ?: aiMap[item.fileName] ?: clean
            mapped.trim().lowercase()
        }

        val collections = grouped.map { (key, groupItems) ->
            val sortedItems = groupItems.sortedWith(
                compareBy<MediaItem> { it.parsedInfo.season ?: 1 }
                    .thenBy { it.parsedInfo.episode ?: 0 }
                    .thenBy { it.fileName }
            )

            val firstItem = sortedItems.first()
            val canonicalName = aiMap[firstItem.parsedInfo.cleanTitle.trim()] 
                ?: aiMap[firstItem.fileName] 
                ?: firstItem.parsedInfo.cleanTitle
            val meta = sortedItems.firstOrNull { it.metadata != null }?.metadata ?: firstItem.metadata

            MediaCollection(
                collectionKey = key,
                title = canonicalName,
                mediaType = firstItem.parsedInfo.detectedType,
                items = sortedItems,
                primaryMetadata = meta
            )
        }

        collections.filter { col ->
            val matchesCategory = category == MediaType.ALL || col.mediaType == category
            val matchesQuery = query.isBlank() ||
                    col.title.contains(query, ignoreCase = true) ||
                    col.items.any { it.fileName.contains(query, ignoreCase = true) } ||
                    (col.primaryMetadata?.genres?.any { it.contains(query, ignoreCase = true) } == true)
            matchesCategory && matchesQuery
        }.sortedByDescending { col ->
            col.items.maxOfOrNull { it.lastModified } ?: 0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Trigger initial scan on startup (only scan real device storage without force-injecting demo files)
        scanLocalFiles(includeDemoFallback = false)
    }

    fun runGeminiSmartGrouping(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            isGroupingWithAi.value = true
            val items = repository.allMediaItems.firstOrNull() ?: mediaItems.value
            val cleanTitles = items.map { item ->
                if (item.parsedInfo.cleanTitle.isNotBlank()) item.parsedInfo.cleanTitle.trim() else item.fileName.trim()
            }.distinct()
            
            val aiMap = GeminiMetadataService.groupAndClusterWithGemini(cleanTitles)
            val siteKeywords = listOf("AioFilm", "AnimeList", "Soft98", "Film2Media", "DigiMoviez", "ZarFilm", "FilmBaran", "Film2Movie", "Bi2Media", "MovieSub", "300MB", "FarsiSub", "Telegram")

            val grouped = items.groupBy { item ->
                val cleanKey = if (item.parsedInfo.cleanTitle.isNotBlank()) item.parsedInfo.cleanTitle.trim() else item.fileName.trim()
                aiMap[cleanKey] ?: item.parsedInfo.cleanTitle
            }

            val clustersList = grouped.map { (canonical, groupItems) ->
                val removedSites = groupItems.flatMap { item ->
                    siteKeywords.filter { item.fileName.contains(it, ignoreCase = true) }
                }.distinct()

                ProposedCluster(
                    canonicalTitle = canonical,
                    items = groupItems,
                    detectedSitesRemoved = removedSites
                )
            }.sortedByDescending { it.items.size }

            proposedClusters.value = clustersList
            showAiConfirmationSheet.value = true
            isGroupingWithAi.value = false
            onComplete(clustersList.size)
        }
    }

    fun applyProposedAiGrouping() {
        val newAiMap = mutableMapOf<String, String>()
        proposedClusters.value.forEach { cluster ->
            cluster.items.forEach { item ->
                newAiMap[item.parsedInfo.cleanTitle.trim()] = cluster.canonicalTitle
                newAiMap[item.fileName.trim()] = cluster.canonicalTitle
            }
        }
        aiCanonicalTitles.value = aiCanonicalTitles.value + newAiMap
        showAiConfirmationSheet.value = false
    }

    fun dismissAiConfirmationSheet() {
        showAiConfirmationSheet.value = false
    }

    fun scanLocalFiles(customFolder: String? = null, includeDemoFallback: Boolean = false) {
        viewModelScope.launch {
            isScanning.value = true
            repository.scanAndEnrichMedia(
                customFolderPath = customFolder,
                includeDemoFallback = includeDemoFallback,
                targetLanguage = targetLanguage.value,
                onProgress = { scanned, total ->
                    scanProgress.value = scanned to total
                }
            )
            isScanning.value = false
        }
    }

    val selectedRecommendationSource = MutableStateFlow("AI") // "AI" or "API"

    fun selectCollection(collection: MediaCollection?) {
        selectedCollection.value = collection
        if (collection != null) {
            val firstItem = collection.items.firstOrNull()
            if (firstItem != null) {
                loadRecommendations(firstItem)
            }
        } else {
            recommendations.value = emptyList()
        }
    }

    fun selectMediaItem(item: MediaItem?) {
        selectedMedia.value = item
        if (item != null) {
            loadRecommendations(item)
        } else {
            recommendations.value = emptyList()
        }
    }

    fun setRecommendationSource(source: String) {
        if (selectedRecommendationSource.value == source) return
        selectedRecommendationSource.value = source
        val currentItem = selectedMedia.value ?: selectedCollection.value?.items?.firstOrNull()
        if (currentItem != null) {
            loadRecommendations(currentItem, source)
        }
    }

    private fun loadRecommendations(item: MediaItem, source: String = selectedRecommendationSource.value) {
        viewModelScope.launch {
            isLoadingRecommendations.value = true
            val recs = repository.getRecommendationsForMedia(item, targetLanguage.value, source)
            recommendations.value = recs
            isLoadingRecommendations.value = false
        }
    }

    fun setLanguage(lang: String) {
        targetLanguage.value = lang
        // Re-scan or re-fetch for selected item if open
        selectedMedia.value?.let { item ->
            loadRecommendations(item)
        }
    }

    fun toggleWatchStatus(item: MediaItem) {
        viewModelScope.launch {
            val newStatus = !item.isWatched
            repository.updateWatchStatus(item.id, newStatus, item.playbackPositionMs)
            if (selectedMedia.value?.id == item.id) {
                selectedMedia.value = item.copy(isWatched = newStatus)
            }
            selectedCollection.value?.let { col ->
                val updatedItems = col.items.map {
                    if (it.id == item.id) it.copy(isWatched = newStatus) else it
                }
                selectedCollection.value = col.copy(items = updatedItems)
            }
        }
    }

    fun markAllInCollectionAsWatched(collection: MediaCollection, isWatched: Boolean) {
        viewModelScope.launch {
            collection.items.forEach { item ->
                repository.updateWatchStatus(item.id, isWatched, item.playbackPositionMs)
            }
            val updatedItems = collection.items.map { it.copy(isWatched = isWatched) }
            selectedCollection.value = collection.copy(items = updatedItems)
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val newFav = !item.isFavorite
            repository.toggleFavorite(item.id, newFav)
            selectedMedia.value = item.copy(isFavorite = newFav)
        }
    }

    fun getPlayIntent(filePath: String, playerPackage: String? = null): Intent {
        return repository.createPlayIntent(filePath, playerPackage)
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearDatabase()
        }
    }

    fun toggleSelectionMode() {
        isSelectionMode.value = !isSelectionMode.value
        if (!isSelectionMode.value) {
            selectedFileIds.value = emptySet()
        }
    }

    fun toggleFileSelection(id: String) {
        val current = selectedFileIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedFileIds.value = current
        if (current.isNotEmpty()) {
            isSelectionMode.value = true
        }
    }

    fun selectAllFiles(allIds: List<String>) {
        isSelectionMode.value = true
        selectedFileIds.value = allIds.toSet()
    }

    fun clearFileSelection() {
        selectedFileIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun reCategorizeSelectedFiles() {
        viewModelScope.launch {
            val selectedIds = selectedFileIds.value
            val currentMediaList = mediaItems.value
            val selectedItems = currentMediaList.filter { selectedIds.contains(it.id) }
            if (selectedItems.isEmpty()) return@launch

            isAnalyzingSelection.value = true
            val results = GeminiMetadataService.reCategorizeSelectedFilesWithGemini(selectedItems)
            recategorizationResults.value = results
            isAnalyzingSelection.value = false
            showRecategorizationSheet.value = true
        }
    }

    fun applyRecategorizationResults() {
        viewModelScope.launch {
            val results = recategorizationResults.value
            if (results.isNotEmpty()) {
                repository.applyRecategorizations(results)
            }
            showRecategorizationSheet.value = false
            recategorizationResults.value = emptyList()
            clearFileSelection()
        }
    }

    fun dismissRecategorizationSheet() {
        showRecategorizationSheet.value = false
    }

    fun reAnalyzeCollectionWithAi(collection: MediaCollection) {
        viewModelScope.launch {
            showCollectionAiEditSheet.value = true
            isAnalyzingCollection.value = true
            proposedCollectionMetadata.value = null

            // 1. If Gemini Key is present, query Gemini AI
            var resultMeta: MediaMetadata? = null
            if (GeminiMetadataService.getApiKey().isNotBlank()) {
                val res = GeminiMetadataService.fetchMetadataAndRecommendations(
                    rawTitle = collection.title,
                    mediaType = collection.mediaType,
                    targetLanguage = targetLanguage.value
                )
                resultMeta = res?.metadata
            }

            // 2. Fallback to AniList/Public Media APIs
            if (resultMeta == null) {
                val publicRes = com.example.data.remote.PublicMediaApiClient.fetchAccurateMetadata(
                    cleanTitle = collection.title,
                    mediaType = collection.mediaType,
                    targetLanguage = targetLanguage.value
                )
                resultMeta = publicRes?.metadata
            }

            if (resultMeta == null && (collection.mediaType == MediaType.ANIME || com.example.data.parser.FileNameParser.isAnimeCandidate(collection.title))) {
                val aniRes = com.example.data.remote.AniListClient.fetchAnimeMetadata(collection.title)
                resultMeta = aniRes?.metadata
            }

            proposedCollectionMetadata.value = resultMeta
            isAnalyzingCollection.value = false
        }
    }

    fun applyCollectionUpdate(
        collection: MediaCollection,
        newTitle: String,
        newType: MediaType,
        newSynopsis: String,
        newPoster: String
    ) {
        viewModelScope.launch {
            val itemIds = collection.items.map { it.id }
            val existingMeta = proposedCollectionMetadata.value ?: collection.primaryMetadata
            val updatedMeta = MediaMetadata(
                titleRomaji = existingMeta?.titleRomaji ?: newTitle,
                titleEnglish = newTitle,
                titleNative = existingMeta?.titleNative ?: newTitle,
                synopsis = newSynopsis,
                posterUrl = newPoster,
                bannerUrl = existingMeta?.bannerUrl ?: newPoster,
                rating = existingMeta?.rating ?: 8.5,
                scoreSource = existingMeta?.scoreSource ?: "AI Verified",
                genres = if (newType == MediaType.ANIME) listOf("Anime") + (existingMeta?.genres ?: emptyList()) else existingMeta?.genres ?: emptyList(),
                releaseYear = existingMeta?.releaseYear,
                totalEpisodes = collection.totalCount
            )

            repository.updateCollectionMetadata(itemIds, newTitle, newType, updatedMeta)

            // Update local AI canonical title map so grouping instantly updates
            val newAiMap = aiCanonicalTitles.value.toMutableMap()
            collection.items.forEach { item ->
                newAiMap[item.parsedInfo.cleanTitle.trim()] = newTitle
                newAiMap[item.fileName.trim()] = newTitle
            }
            aiCanonicalTitles.value = newAiMap

            showCollectionAiEditSheet.value = false
            proposedCollectionMetadata.value = null

            // Refresh selected collection view
            val updatedItems = collection.items.map { item ->
                item.copy(
                    parsedInfo = item.parsedInfo.copy(cleanTitle = newTitle, detectedType = newType),
                    metadata = updatedMeta
                )
            }
            selectedCollection.value = collection.copy(
                title = newTitle,
                mediaType = newType,
                items = updatedItems,
                primaryMetadata = updatedMeta
            )
        }
    }

    fun dismissCollectionAiEditSheet() {
        showCollectionAiEditSheet.value = false
        proposedCollectionMetadata.value = null
    }

    fun deleteCollection(collection: MediaCollection) {
        viewModelScope.launch {
            val itemIds = collection.items.map { it.id }
            repository.deleteCollection(itemIds)
            if (selectedCollection.value?.collectionKey == collection.collectionKey) {
                selectedCollection.value = null
            }
        }
    }
}
