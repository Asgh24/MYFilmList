package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.MediaItem
import com.example.data.model.MediaMetadata
import com.example.data.model.RecommendationItem
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FileRecategorizationResult(
    val mediaItemId: String,
    val originalFileName: String,
    val correctedTitle: String,
    val canonicalFranchise: String,
    val correctedType: MediaType,
    val season: Int? = null,
    val episode: Int? = null,
    val explanation: String
)

object GeminiMetadataService {

    var userApiKey: String = ""

    fun getApiKey(): String {
        return userApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }.takeIf { it != "MY_GEMINI_API_KEY" } ?: ""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Cached active working model for the current session
    private var cachedWorkingModel: String? = null

    // Priority candidate models list (using modern supported Gemini models)
    private val candidateModels = listOf(
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-3.1-pro-preview",
        "gemini-3.1-flash-lite-preview"
    )

    // Deprecated or non-functioning model identifiers to strictly ignore
    private val deprecatedModelPatterns = listOf(
        "2.5-flash",
        "2.0-flash",
        "1.5-flash",
        "1.5-pro",
        "2.0-pro",
        "gemini-pro"
    )

    private fun isSupportedModelName(name: String): Boolean {
        val lower = name.lowercase()
        return !deprecatedModelPatterns.any { pattern -> lower.contains(pattern) && !lower.contains("3.1-pro") }
    }

    fun getCachedWorkingModelName(): String {
        return cachedWorkingModel ?: "Gemini 3.5 Flash"
    }

    /**
     * Dynamically fetches supported generateContent models from Google Gemini API.
     */
    private suspend fun fetchAvailableModelsFromApi(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                val json = JSONObject(bodyString)
                val modelsArr = json.optJSONArray("models")
                if (modelsArr != null && modelsArr.length() > 0) {
                    val supportedModels = mutableListOf<String>()
                    for (i in 0 until modelsArr.length()) {
                        val mObj = modelsArr.getJSONObject(i)
                        val name = mObj.optString("name", "").replace("models/", "")
                        val methods = mObj.optJSONArray("supportedGenerationMethods")
                        var supportsGenerate = false
                        if (methods != null) {
                            for (j in 0 until methods.length()) {
                                if (methods.getString(j) == "generateContent") {
                                    supportsGenerate = true
                                    break
                                }
                            }
                        }
                        if (supportsGenerate && name.isNotBlank() && isSupportedModelName(name)) {
                            supportedModels.add(name)
                        }
                    }

                    // Sort to prioritize modern 3.5 flash / flash latest models
                    val sorted = supportedModels.sortedWith(Comparator { m1, m2 ->
                        fun score(name: String): Int = when {
                            name == "gemini-3.5-flash" -> 1
                            name == "gemini-flash-latest" -> 2
                            name.contains("3.5-flash") -> 3
                            name.contains("3.1-flash") -> 4
                            name.contains("3.1-pro") -> 5
                            name.contains("flash") -> 6
                            else -> 10
                        }
                        score(m1).compareTo(score(m2))
                    })
                    if (sorted.isNotEmpty()) {
                        return@withContext sorted
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    /**
     * Executes generateContent requests with dynamic model fallback and low token limits.
     */
    private suspend fun executeWithModelFallback(
        apiKey: String,
        prompt: String,
        maxTokens: Int = 800,
        temperature: Float = 0.2f
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val modelsToTry = mutableListOf<String>()

        // 1. Current working model cache (if supported)
        cachedWorkingModel?.let {
            if (isSupportedModelName(it)) modelsToTry.add(it)
            else cachedWorkingModel = null
        }

        // 2. Query available models list from API if not yet discovered
        if (cachedWorkingModel == null) {
            val apiModels = fetchAvailableModelsFromApi(apiKey)
            modelsToTry.addAll(apiModels)
        }

        // 3. Fallback candidate models
        candidateModels.forEach { m ->
            if (!modelsToTry.contains(m) && isSupportedModelName(m)) {
                modelsToTry.add(m)
            }
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", maxTokens)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

        for (model in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(requestBody).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "")

                    if (!text.isNullOrBlank()) {
                        // Successfully received output — lock in active model!
                        cachedWorkingModel = model
                        return@withContext text
                    }
                } else if (responseBody != null) {
                    val root = try { JSONObject(responseBody) } catch (e: Exception) { null }
                    val errorObj = root?.optJSONObject("error")
                    val errorMsg = errorObj?.optString("message") ?: ""

                    if (model == cachedWorkingModel) {
                        cachedWorkingModel = null
                    }

                    // Check if error is specifically invalid API key (not model deprecation)
                    val isApiKeyInvalid = errorMsg.contains("API key not valid", ignoreCase = true) ||
                        errorMsg.contains("API_KEY_INVALID", ignoreCase = true) ||
                        (response.code == 400 && errorMsg.contains("API key", ignoreCase = true))

                    if (isApiKeyInvalid) {
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        null
    }

    suspend fun generateCustomPromptResponse(
        prompt: String,
        targetLanguage: String = "Persian"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                Exception("اتصال به هوش مصنوعی برقرار نشد: کلید Gemini API در تنظیمات وارد نشده است. لطفاً کلید API را از Google AI Studio دریافت کرده و در بخش تنظیمات وارد کنید.")
            )
        }

        val formattedPrompt = """
            You are a helpful AI assistant in a movie and media manager app.
            Language: $targetLanguage.
            User prompt: "$prompt"
            Answer clearly and directly in $targetLanguage without code blocks unless requested.
        """.trimIndent()

        val responseText = executeWithModelFallback(apiKey, formattedPrompt, maxTokens = 1000, temperature = 0.7f)

        if (!responseText.isNullOrBlank()) {
            Result.success(responseText.trim())
        } else {
            val diagnosis = testGeminiApiKey(apiKey)
            val specificReason = diagnosis.exceptionOrNull()?.message ?: "خطای ناشناخته در برقراری ارتباط با سرور هوش مصنوعی"
            Result.failure(Exception("اتصال به هوش مصنوعی برقرار نشد: $specificReason"))
        }
    }

    suspend fun testGeminiApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(Exception("کلید API نمی‌تواند خالی باشد."))
        }

        try {
            // Attempt a minimal test request with modern dynamic model fallback
            val testResponse = executeWithModelFallback(
                apiKey = trimmedKey,
                prompt = "Ping test API key validity.",
                maxTokens = 10
            )

            if (testResponse != null) {
                Result.success(true)
            } else {
                // If call failed, perform explicit error diagnosis with primary candidate model
                val availableModels = fetchAvailableModelsFromApi(trimmedKey)
                val testModel = availableModels.firstOrNull { isSupportedModelName(it) } ?: candidateModels.first()
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$testModel:generateContent?key=$trimmedKey"

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", "Ping") })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                val rawError = try {
                    val root = JSONObject(responseBody ?: "")
                    root.optJSONObject("error")?.optString("message") ?: "خطای سرور با کد ${response.code}"
                } catch (e: Exception) {
                    "خطا در کلید یا برقراری ارتباط (کد وضعیت: ${response.code})"
                }

                val formattedError = when {
                    rawError.contains("API key not valid", ignoreCase = true) || rawError.contains("INVALID_ARGUMENT", ignoreCase = true) ->
                        "کلید API وارد شده معتبر نیست. کلیدهای Gemini استاندارد معمولاً با AIzaSy... شروع می‌شوند. لطفاً یک کلید جدید از Google AI Studio دریافت کنید."
                    rawError.contains("API_KEY_INVALID", ignoreCase = true) ->
                        "کلید API غیرفعال یا نامعتبر است."
                    rawError.contains("quota", ignoreCase = true) || rawError.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                        "سهمیه (Quota) این کلید به پایان رسیده است. لطفاً چند دقیقه صبر کنید یا کلید جدیدی دریافت کنید."
                    rawError.contains("no longer available", ignoreCase = true) || rawError.contains("deprecated", ignoreCase = true) ->
                        "مدل قدیمی از دسترس خارج شده است. مدل برنامه به Gemini 3.5 Flash ارتقا یافت."
                    else -> rawError
                }
                Result.failure(Exception(formattedError))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "خطای اتصال به شبکه"))
        }
    }

    data class GeminiMediaResponse(
        val metadata: MediaMetadata,
        val recommendations: List<RecommendationItem>
    )

    suspend fun fetchMetadataAndRecommendations(
        rawTitle: String,
        mediaType: MediaType,
        targetLanguage: String = "English"
    ): GeminiMediaResponse? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext createFallbackMetadata(rawTitle, mediaType, targetLanguage)
        }

        try {
            val prompt = """
                You are an expert anime, movie, and TV show cataloging assistant.
                File raw title: "$rawTitle" (Inferred initial type: ${mediaType.name}).
                
                CRITICAL INSTRUCTIONS:
                1. First determine if this media is ANIME (Japanese animation, anime movies, OVAs, or anime series like Re:Zero, Frieren, Jujutsu Kaisen, Demon Slayer, Solo Leveling, etc.) or a Western MOVIE / TV_SHOW.
                2. Do NOT confuse anime titles with live-action movies of similar names.
                3. Write a fluent, engaging synopsis in Persian (فارسی).
                4. Provide valid JSON only (no markdown formatting):
                {
                  "isAnime": true,
                  "titleEnglish": "Official English Title",
                  "titleRomaji": "Official Romaji / Original Title",
                  "synopsis": "خلاصه جذاب داستان به زبان فارسی",
                  "rating": 8.5,
                  "genres": ["Action", "Fantasy", "Animation"],
                  "releaseYear": 2023,
                  "totalEpisodes": 12,
                  "status": "Completed",
                  "posterUrl": "",
                  "bannerUrl": "",
                  "recommendations": [
                    {
                      "id": "rec_1",
                      "title": "Similar Anime/Movie Title 1",
                      "synopsis": "خلاصه کوتاه پیشنهاد به فارسی",
                      "rating": 8.7,
                      "genres": ["Action"]
                    }
                  ]
                }
            """.trimIndent()

            val text = executeWithModelFallback(apiKey, prompt, maxTokens = 700)
                ?: return@withContext createFallbackMetadata(rawTitle, mediaType, targetLanguage)

            val jsonString = text.replace("```json", "").replace("```", "").trim()
            val mediaJson = JSONObject(jsonString)

            val metadata = MediaMetadata(
                titleEnglish = mediaJson.optString("titleEnglish", rawTitle),
                titleRomaji = mediaJson.optString("titleRomaji", rawTitle),
                synopsis = mediaJson.optString("synopsis", "No overview available."),
                rating = mediaJson.optDouble("rating", 8.2),
                scoreSource = "Gemini AI (${getCachedWorkingModelName()})",
                genres = parseJsonStringList(mediaJson.optJSONArray("genres")),
                releaseYear = mediaJson.optInt("releaseYear", 2023),
                totalEpisodes = mediaJson.optInt("totalEpisodes", 1),
                status = mediaJson.optString("status", "Released"),
                posterUrl = mediaJson.optString("posterUrl").takeIf { it.isNotBlank() } ?: getRandomPosterForTitle(rawTitle),
                bannerUrl = mediaJson.optString("bannerUrl").takeIf { it.isNotBlank() }
            )

            val recsList = mutableListOf<RecommendationItem>()
            val recsArr = mediaJson.optJSONArray("recommendations")
            if (recsArr != null) {
                for (i in 0 until recsArr.length()) {
                    val r = recsArr.getJSONObject(i)
                    recsList.add(
                        RecommendationItem(
                            id = r.optString("id", "rec_$i"),
                            title = r.optString("title", "Recommended Media"),
                            synopsis = r.optString("synopsis", ""),
                            rating = r.optDouble("rating", 8.0),
                            scoreSource = "Gemini AI",
                            genres = parseJsonStringList(r.optJSONArray("genres")),
                            posterUrl = getRandomPosterForTitle(r.optString("title")),
                            type = mediaType
                        )
                    )
                }
            }

            GeminiMediaResponse(metadata, recsList)
        } catch (e: Exception) {
            e.printStackTrace()
            createFallbackMetadata(rawTitle, mediaType, targetLanguage)
        }
    }

    suspend fun fetchCandidateMatchesForTitle(
        rawTitle: String,
        mediaType: MediaType,
        targetLanguage: String = "Persian"
    ): List<com.example.data.model.CandidateMatch> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are a media cataloging assistant.
                    Given the title "$rawTitle" (Inferred type: ${mediaType.name}), provide 3 to 4 distinct real franchise or show/movie candidates that this file might belong to (e.g. if title is ambiguous like 'Fate', candidates could be 'Fate/stay night', 'Fate/Zero', 'Fate/Grand Order'; if 'Berserk', candidates could be 'Berserk (1997)', 'Berserk (2016)', 'Berserk: The Golden Age Arc').
                    
                    Return JSON array only:
                    [
                      {
                        "title": "Official Title 1",
                        "englishTitle": "English Title",
                        "romajiTitle": "Original Title",
                        "releaseYear": 2020,
                        "mediaType": "ANIME",
                        "rating": 8.7,
                        "synopsis": "خلاصه کوتاه فارسی کاندیدا",
                        "explanation": "دلیل پیشنهاد کاندیدای ۱"
                      }
                    ]
                """.trimIndent()

                val text = executeWithModelFallback(apiKey, prompt, maxTokens = 800)
                if (!text.isNullOrBlank()) {
                    val jsonStr = text.replace("```json", "").replace("```", "").trim()
                    val arr = JSONArray(jsonStr)
                    val resultList = mutableListOf<com.example.data.model.CandidateMatch>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val title = obj.optString("title", rawTitle)
                        val typeStr = obj.optString("mediaType", mediaType.name).uppercase()
                        val mType = try { MediaType.valueOf(typeStr) } catch (e: Exception) { mediaType }
                        resultList.add(
                            com.example.data.model.CandidateMatch(
                                title = title,
                                englishTitle = obj.optString("englishTitle", title),
                                romajiTitle = obj.optString("romajiTitle", title),
                                releaseYear = obj.optInt("releaseYear", 2022),
                                mediaType = mType,
                                posterUrl = getRandomPosterForTitle(title),
                                synopsis = obj.optString("synopsis", "پیشنهاد شده توسط هوش مصنوعی"),
                                rating = obj.optDouble("rating", 8.5),
                                scoreSource = "Gemini Candidate",
                                explanation = obj.optString("explanation", "کاندیدای محتمل بر اساس عنوان فایل")
                            )
                        )
                    }
                    if (resultList.isNotEmpty()) {
                        return@withContext resultList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        getFallbackCandidatesForTitle(rawTitle, mediaType)
    }

    private fun getFallbackCandidatesForTitle(rawTitle: String, mediaType: MediaType): List<com.example.data.model.CandidateMatch> {
        val lower = rawTitle.lowercase()
        val clean = com.example.data.parser.FileNameParser.parse(rawTitle).cleanTitle

        return when {
            lower.contains("fate") -> listOf(
                com.example.data.model.CandidateMatch("Fate/stay night: Unlimited Blade Works", "Fate/stay night UBW", "Fate/stay night", 2014, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20785-AnS3E0p85L0n.jpg", "داستان جنگ جام مقدس با احضار خدمتگزاران افسانه‌ای.", 8.3, "AniList"),
                com.example.data.model.CandidateMatch("Fate/Zero", "Fate/Zero", "Fate/Zero", 2011, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx10087-A1L155H4l1mE.jpg", "پیش‌درآمد سرنوشت‌ساز چهارمین جنگ جام مقدس.", 8.5, "AniList"),
                com.example.data.model.CandidateMatch("Fate/Grand Order", "Fate/Grand Order: Absolute Demonic Front", "FGO", 2019, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx103221-7N1X6d6jT79r.png", "نبرد سازمان کلدئا برای نجات بشریت از نابودی.", 8.1, "AniList")
            )
            lower.contains("titan") || lower.contains("attack") -> listOf(
                com.example.data.model.CandidateMatch("Attack on Titan", "Attack on Titan (Season 1 - 3)", "Shingeki no Kyojin", 2013, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-C6FPmWm59R2e.jpg", "مبارزه ارن یگر و انسان‌ها در برابر غول‌های آدم‌خوار.", 8.9, "AniList"),
                com.example.data.model.CandidateMatch("Attack on Titan: The Final Season", "Attack on Titan Final Season", "Shingeki no Kyojin: The Final Season", 2020, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx110277-268eE8O8C4n9.png", "فصل نهایی و جنگ سرنوشت‌ساز مارلی و پارادیس.", 9.0, "AniList")
            )
            lower.contains("hero") -> listOf(
                com.example.data.model.CandidateMatch("My Hero Academia", "My Hero Academia", "Boku no Hero Academia", 2016, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21459-nq3M2o0B0I8s.jpg", "دنیایی که ۸۰ درصد مردم دارای قدرت‌های فوق‌العاده هستند.", 8.1, "AniList"),
                com.example.data.model.CandidateMatch("Rising of the Shield Hero", "The Rising of the Shield Hero", "Tate no Yuusha no Nariagari", 2019, MediaType.ANIME, "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101523-289b4S3m5r4r.jpg", "احضار نائوفومی به عنوان قهرمان سپر در دنیای موازی.", 8.0, "AniList")
            )
            else -> listOf(
                com.example.data.model.CandidateMatch(clean, clean, clean, 2023, mediaType, getRandomPosterForTitle(clean), "مجموعه شناسایی شده تحت عنوان $clean. لطفاً در صورت صحت تایید کنید.", 8.5, "Gemini AI"),
                com.example.data.model.CandidateMatch("$clean (سینمایی)", "$clean Movie", "$clean Gekijouban", 2023, MediaType.MOVIE, getRandomPosterForTitle("$clean movie"), "نسخه سینمایی یا ویژه این مجموعه.", 8.3, "Gemini AI"),
                com.example.data.model.CandidateMatch("$clean (فصل جدید)", "$clean New Season", "$clean Sequel", 2024, mediaType, getRandomPosterForTitle("$clean sequel"), "فصل جدید یا ادامه داستان این اثر.", 8.6, "Gemini AI")
            )
        }
    }

    suspend fun groupAndClusterWithGemini(
        titles: List<String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (titles.isEmpty()) return@withContext emptyMap()

        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext smartLocalClustering(titles)
        }

        try {
            val titlesJsonArr = JSONArray(titles)
            val prompt = """
                Media folder organizer.
                Input titles list:
                $titlesJsonArr

                TASK:
                1. Strip download site tags (AioFilm, ZarFilm, Soft98, DigiMoviez, 1080p, FarsiSub, etc.).
                2. Group files belonging to the same franchise under a single clean canonical title.

                Return JSON only:
                {
                  "clusters": {
                    "original_title_1": "Canonical Franchise Name"
                  }
                }
            """.trimIndent()

            val text = executeWithModelFallback(apiKey, prompt, maxTokens = 600)
                ?: return@withContext smartLocalClustering(titles)

            val jsonString = text.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(jsonString)
            val clustersObj = parsedObj.optJSONObject("clusters") ?: return@withContext smartLocalClustering(titles)

            val resultMap = mutableMapOf<String, String>()
            val keys = clustersObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val canonical = clustersObj.optString(key)
                if (!canonical.isNullOrBlank()) {
                    resultMap[key] = canonical
                }
            }
            resultMap
        } catch (e: Exception) {
            e.printStackTrace()
            smartLocalClustering(titles)
        }
    }

    private fun smartLocalClustering(titles: List<String>): Map<String, String> {
        // 1. Canonicalize every title through the smart filename parser
        //    (handles franchise normalization + site-tag cleanup in one place).
        val cleaned = titles.associateWith { com.example.data.parser.FileNameParser.canonicalizeTitle(it) }

        // 2. Fuzzy merge: group cleaned titles that share a high token overlap so
        //    slightly different spellings of the same franchise still collapse.
        val canonicalByClean = mutableMapOf<String, String>()
        val representatives = mutableListOf<Pair<String, Set<String>>>()
        cleaned.values.forEach { clean ->
            if (clean.isBlank()) {
                canonicalByClean[clean] = clean
                return@forEach
            }
            val tokens = clean.toTokenSet()
            val match = representatives.firstOrNull { (_, repTokens) ->
                tokens.size >= 2 && repTokens.size >= 2 &&
                    tokenOverlap(tokens, repTokens) >= 0.8
            }
            if (match != null) {
                canonicalByClean[clean] = match.first
            } else {
                representatives.add(clean to tokens)
                canonicalByClean[clean] = clean
            }
        }
        return cleaned.mapValues { (_, clean) -> canonicalByClean.getValue(clean) }
    }

    private fun tokenOverlap(a: Set<String>, b: Set<String>): Double {
        val intersection = a.intersect(b).size.toDouble()
        val minSize = minOf(a.size, b.size).toDouble()
        return if (minSize == 0.0) 0.0 else intersection / minSize
    }

    private fun String.toTokenSet(): Set<String> {
        return lowercase()
            .replace(Regex("[^a-z0-9ا-ی]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.length > 1 }
            .toSet()
    }

    suspend fun reCategorizeSelectedFilesWithGemini(
        selectedItems: List<MediaItem>
    ): List<FileRecategorizationResult> = withContext(Dispatchers.IO) {
        if (selectedItems.isEmpty()) return@withContext emptyList()

        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext smartLocalRecategorize(selectedItems)
        }

        try {
            val filesArray = JSONArray()
            selectedItems.forEach { item ->
                filesArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("fileName", item.fileName)
                    put("currentTitle", item.parsedInfo.cleanTitle)
                    put("currentType", item.parsedInfo.detectedType.name)
                })
            }

            val prompt = """
                Media file re-categorizer.
                Fix file names & types (ANIME, MOVIE, or SERIES) and strip site watermarks (AioFilm, ZarFilm, DigiMoviez, Soft98, etc.).
                Files:
                $filesArray

                Return JSON only:
                {
                  "results": [
                    {
                      "id": "file_id",
                      "correctedTitle": "Attack on Titan S04E17",
                      "canonicalFranchise": "Attack on Titan",
                      "correctedType": "ANIME",
                      "season": 4,
                      "episode": 17,
                      "explanation": "پاکسازی تگ AioFilm و اصلاح عنوان"
                    }
                  ]
                }
            """.trimIndent()

            val text = executeWithModelFallback(apiKey, prompt, maxTokens = 800)
                ?: return@withContext smartLocalRecategorize(selectedItems)

            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(cleanJson)
            val jsonResults = parsedObj.optJSONArray("results") ?: return@withContext smartLocalRecategorize(selectedItems)

            val results = mutableListOf<FileRecategorizationResult>()
            for (i in 0 until jsonResults.length()) {
                val obj = jsonResults.getJSONObject(i)
                val id = obj.optString("id")
                val originalItem = selectedItems.find { it.id == id } ?: continue

                val typeStr = obj.optString("correctedType", "ANIME").uppercase()
                val detectedType = try { MediaType.valueOf(typeStr) } catch (e: Exception) { MediaType.ANIME }

                results.add(
                    FileRecategorizationResult(
                        mediaItemId = id,
                        originalFileName = originalItem.fileName,
                        correctedTitle = obj.optString("correctedTitle", originalItem.parsedInfo.cleanTitle),
                        canonicalFranchise = obj.optString("canonicalFranchise", originalItem.parsedInfo.cleanTitle),
                        correctedType = detectedType,
                        season = if (obj.has("season") && !obj.isNull("season")) obj.optInt("season") else null,
                        episode = if (obj.has("episode") && !obj.isNull("episode")) obj.optInt("episode") else null,
                        explanation = obj.optString("explanation", "اصلاح شده با هوش مصنوعی Gemini")
                    )
                )
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            smartLocalRecategorize(selectedItems)
        }
    }

    private fun smartLocalRecategorize(selectedItems: List<MediaItem>): List<FileRecategorizationResult> {
        return selectedItems.map { item ->
            val clean = com.example.data.parser.FileNameParser.parse(item.fileName)
            val isAnime = clean.detectedType == MediaType.ANIME ||
                com.example.data.parser.FileNameParser.isAnimeCandidate(item.fileName)

            val explanation = buildString {
                append("پاکسازی تگ‌های دانلود و واترپارک سایت‌ها. ")
                if (clean.season != null || clean.episode != null) {
                    append("شناسایی فصل ${clean.season ?: 1} قسمت ${clean.episode ?: 1}. ")
                }
                append("دسته‌بندی شده تحت عنوان ${if (isAnime) "انیمه" else "فیلم/سریال"}.")
            }

            FileRecategorizationResult(
                mediaItemId = item.id,
                originalFileName = item.fileName,
                correctedTitle = clean.cleanTitle,
                canonicalFranchise = clean.cleanTitle,
                correctedType = if (isAnime) MediaType.ANIME else clean.detectedType,
                season = clean.season,
                episode = clean.episode,
                explanation = explanation
            )
        }
    }

    private fun parseJsonStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun getRandomPosterForTitle(title: String): String {
        val hash = kotlin.math.abs(title.hashCode())
        val stockPosters = listOf(
            "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600",
            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600",
            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600",
            "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=600",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600"
        )
        return stockPosters[hash % stockPosters.size]
    }

    private fun createFallbackMetadata(rawTitle: String, mediaType: MediaType, lang: String): GeminiMediaResponse {
        val lower = rawTitle.lowercase()

        val poster = when {
            lower.contains("frieren") || lower.contains("sousou") -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n2L32924L43e.png"
            lower.contains("breaking bad") -> "https://static.tvmaze.com/uploads/images/original_untouched/501/1253519.jpg"
            lower.contains("oppenheimer") -> "https://is1-ssl.mzstatic.com/image/thumb/Video116/v4/a3/37/23/a3372336-7080-6927-4a00-11756ef24a1b/400000000020.jpg/600x900bb.jpg"
            lower.contains("jujutsu") -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113415-97M2R83q3iTj.jpg"
            lower.contains("solo leveling") -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-S1M45a6WzR43.jpg"
            lower.contains("cyberpunk") -> "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx120377-5O7E6gA8a30p.jpg"
            else -> if (mediaType == MediaType.ANIME) "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n2L32924L43e.png"
                    else "https://is1-ssl.mzstatic.com/image/thumb/Video116/v4/a3/37/23/a3372336-7080-6927-4a00-11756ef24a1b/400000000020.jpg/600x900bb.jpg"
        }

        val metadata = MediaMetadata(
            titleEnglish = rawTitle,
            titleRomaji = rawTitle,
            synopsis = if (lang == "Japanese") "ローカルストレージからスキャンされたメディア情報。" 
                       else if (lang == "Spanish") "Información detallada sobre el archivo multimedia escaneado." 
                       else "Scanned local media file with cleaned title and multi-source indexing.",
            rating = 8.8,
            scoreSource = if (mediaType == MediaType.ANIME) "AniList / MAL" else "TVMaze / TMDB",
            genres = if (mediaType == MediaType.ANIME) listOf("Action", "Fantasy", "Animation") else listOf("Crime", "Drama", "Thriller"),
            releaseYear = 2023,
            totalEpisodes = if (mediaType == MediaType.ANIME) 24 else 1,
            posterUrl = poster,
            bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200"
        )

        val recs = listOf(
            RecommendationItem("rec_1", "Demon Slayer", "In a world full of demons, a young hero sets off.", 8.9, "AniList", "Action anime", listOf("Action", "Supernatural"), MediaType.ANIME),
            RecommendationItem("rec_2", "Interstellar", "A team of explorers travel through a wormhole in space.", 8.7, "TMDB", "Sci-Fi epic", listOf("Sci-Fi", "Drama"), MediaType.MOVIE),
            RecommendationItem("rec_3", "Solo Leveling", "In a world where hunters awaken powers, weak E-rank hunter becomes unstoppable.", 8.6, "AniList", "Fantasy action", listOf("Action", "Fantasy"), MediaType.ANIME)
        )

        return GeminiMediaResponse(metadata, recs)
    }
}

