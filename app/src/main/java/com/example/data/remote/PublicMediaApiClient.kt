package com.example.data.remote

import com.example.data.model.MediaMetadata
import com.example.data.model.MediaType
import com.example.data.model.RecommendationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object PublicMediaApiClient {

    var tmdbApiKey: String = ""
    var omdbApiKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class PublicFetchResult(
        val metadata: MediaMetadata,
        val recommendations: List<RecommendationItem> = emptyList()
    )

    suspend fun fetchAccurateMetadata(
        cleanTitle: String,
        mediaType: MediaType,
        targetLanguage: String = "English"
    ): PublicFetchResult? = withContext(Dispatchers.IO) {
        val isAnimeCandidate = mediaType == MediaType.ANIME ||
            com.example.data.parser.FileNameParser.isAnimeCandidate(cleanTitle)

        // 0. For Anime candidates, ALWAYS check AniList / MyAnimeList FIRST before TMDB/OMDb/TVMaze!
        if (isAnimeCandidate) {
            val animeResult = fetchFromJikanOrAniList(cleanTitle) ?: AniListClient.fetchAnimeMetadata(cleanTitle)?.let {
                PublicFetchResult(it.metadata, it.recommendations)
            }
            if (animeResult != null) return@withContext animeResult
        }

        // 1. Try TMDB / OMDb configured keys for non-anime or as fallback
        if (tmdbApiKey.isNotBlank()) {
            val tmdbRes = fetchFromTMDB(cleanTitle, tmdbApiKey, mediaType)
            if (tmdbRes != null) return@withContext tmdbRes
        }
        if (omdbApiKey.isNotBlank()) {
            val omdbRes = fetchFromOMDb(cleanTitle, omdbApiKey)
            if (omdbRes != null) return@withContext omdbRes
        }

        // 2. Check curated accurate official posters map
        val curated = getCuratedPosterForTitle(cleanTitle)

        // 3. Try type-specific API calls
        val apiResult = when (mediaType) {
            MediaType.ANIME -> fetchFromJikanOrAniList(cleanTitle)
            MediaType.SERIES -> fetchFromTVMaze(cleanTitle)
            MediaType.MOVIE -> fetchFromITunes(cleanTitle) ?: fetchFromTVMaze(cleanTitle)
            MediaType.ALL, MediaType.UNKNOWN -> {
                fetchFromJikanOrAniList(cleanTitle)
                    ?: fetchFromTVMaze(cleanTitle)
                    ?: fetchFromITunes(cleanTitle)
            }
        }

        if (apiResult != null) {
            // If API returned a poster, prefer it; otherwise use curated if available
            val finalPoster = apiResult.metadata.posterUrl ?: curated?.posterUrl
            val finalBanner = apiResult.metadata.bannerUrl ?: curated?.bannerUrl
            val finalMeta = apiResult.metadata.copy(
                posterUrl = finalPoster,
                bannerUrl = finalBanner
            )
            return@withContext PublicFetchResult(finalMeta, apiResult.recommendations)
        }

        // 3. Return curated fallback if available
        if (curated != null) {
            return@withContext PublicFetchResult(curated, getCuratedRecommendations(cleanTitle))
        }

        null
    }

    private const val MAL_CLIENT_ID = "fb6de4abfb30685da942fd6ac5521ca9"

    private suspend fun fetchFromJikanOrAniList(cleanTitle: String): PublicFetchResult? {
        // 1. Try MyAnimeList official API v2 with Client ID
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.myanimelist.net/v2/anime?q=$encoded&limit=1&fields=id,title,main_picture,synopsis,mean,genres,start_date,alternative_titles"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-MAL-CLIENT-ID", MAL_CLIENT_ID)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val dataArr = json.optJSONArray("data")
                    if (dataArr != null && dataArr.length() > 0) {
                        val node = dataArr.getJSONObject(0).optJSONObject("node")
                        if (node != null) {
                            val titleEng = node.optJSONObject("alternative_titles")?.optString("en")
                                ?.takeIf { it.isNotBlank() } ?: node.optString("title")
                            val titleJap = node.optJSONObject("alternative_titles")?.optString("ja")
                                ?.takeIf { it.isNotBlank() } ?: node.optString("title")
                            val synopsis = node.optString("synopsis", "")
                            val score = node.optDouble("mean", 8.5)
                            val mainPic = node.optJSONObject("main_picture")
                            val posterUrl = mainPic?.optString("large") ?: mainPic?.optString("medium")
                            val startDate = node.optString("start_date", "")
                            val releaseYear = if (startDate.length >= 4) startDate.substring(0, 4).toIntOrNull() else 2023

                            val genresList = mutableListOf<String>()
                            val genresArr = node.optJSONArray("genres")
                            if (genresArr != null) {
                                for (i in 0 until genresArr.length()) {
                                    val gName = genresArr.getJSONObject(i).optString("name")
                                    if (gName.isNotBlank()) genresList.add(gName)
                                }
                            }

                            val metadata = MediaMetadata(
                                titleEnglish = titleEng,
                                titleRomaji = titleJap,
                                synopsis = synopsis.replace("\n", " ").trim(),
                                posterUrl = posterUrl,
                                rating = score,
                                scoreSource = "MyAnimeList API",
                                genres = if (genresList.isNotEmpty()) genresList else listOf("Anime", "Animation"),
                                releaseYear = releaseYear ?: 2023
                            )
                            return PublicFetchResult(metadata)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to AniList
        val ani = AniListClient.fetchAnimeMetadata(cleanTitle)
        if (ani != null && ani.metadata.posterUrl != null) {
            return PublicFetchResult(ani.metadata, ani.recommendations)
        }

        // 3. Fallback to Jikan API v4 (MyAnimeList open mirror)
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.jikan.moe/v4/anime?q=$encoded&limit=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val dataArr = json.optJSONArray("data") ?: return null
            if (dataArr.length() == 0) return null

            val anime = dataArr.getJSONObject(0)
            val titleEng = anime.optString("title_english", anime.optString("title"))
            val titleJap = anime.optString("title_japanese", anime.optString("title"))
            val synopsis = anime.optString("synopsis", "")
            val score = anime.optDouble("score", 0.0)

            val images = anime.optJSONObject("images")?.optJSONObject("jpg")
            val posterUrl = images?.optString("large_image_url") ?: images?.optString("image_url")

            val year = anime.optJSONObject("aired")?.optJSONObject("prop")?.optJSONObject("from")?.optInt("year", 0)

            val metadata = MediaMetadata(
                titleEnglish = titleEng,
                titleRomaji = titleJap,
                synopsis = synopsis.replace("\n", " ").trim(),
                posterUrl = posterUrl,
                rating = if (score > 0) score else 8.2,
                scoreSource = "MyAnimeList",
                genres = listOf("Anime", "Animation"),
                releaseYear = if (year != null && year > 0) year else 2023
            )
            return PublicFetchResult(metadata)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchFromTVMaze(cleanTitle: String): PublicFetchResult? {
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.tvmaze.com/search/shows?q=$encoded"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val jsonArray = org.json.JSONArray(body)
            if (jsonArray.length() == 0) return null

            val firstShowObj = jsonArray.getJSONObject(0).optJSONObject("show") ?: return null
            val name = firstShowObj.optString("name")
            val summaryHtml = firstShowObj.optString("summary", "")
            val cleanSummary = summaryHtml.replace(Regex("<[^>]*>"), "").trim()
            val ratingObj = firstShowObj.optJSONObject("rating")
            val rating = ratingObj?.optDouble("average", 0.0) ?: 0.0

            val imageObj = firstShowObj.optJSONObject("image")
            val posterUrl = imageObj?.optString("original") ?: imageObj?.optString("medium")

            val premiered = firstShowObj.optString("premiered", "")
            val releaseYear = if (premiered.length >= 4) premiered.substring(0, 4).toIntOrNull() else null

            val genresList = mutableListOf<String>()
            val genresArr = firstShowObj.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(i))
                }
            }

            val metadata = MediaMetadata(
                titleEnglish = name,
                titleRomaji = name,
                synopsis = cleanSummary,
                posterUrl = posterUrl,
                rating = if (rating > 0) rating else 8.9,
                scoreSource = "TVMaze",
                genres = if (genresList.isNotEmpty()) genresList else listOf("Drama", "TV Series"),
                releaseYear = releaseYear ?: 2010
            )

            val recs = mutableListOf<RecommendationItem>()
            for (i in 1 until jsonArray.length()) {
                val showObj = jsonArray.getJSONObject(i).optJSONObject("show") ?: continue
                val sName = showObj.optString("name")
                val sImg = showObj.optJSONObject("image")
                val sPoster = sImg?.optString("original") ?: sImg?.optString("medium")
                val sSummary = showObj.optString("summary", "").replace(Regex("<[^>]*>"), "").trim()
                val sRating = showObj.optJSONObject("rating")?.optDouble("average", 8.5) ?: 8.5

                if (sName.isNotBlank()) {
                    recs.add(
                        RecommendationItem(
                            id = "tvmaze_$i",
                            title = sName,
                            posterUrl = sPoster,
                            rating = if (sRating > 0) sRating else 8.5,
                            scoreSource = "TVMaze",
                            synopsis = sSummary,
                            genres = genresList,
                            type = MediaType.SERIES
                        )
                    )
                }
            }

            return PublicFetchResult(metadata, recs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchFromITunes(cleanTitle: String): PublicFetchResult? {
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=movie&limit=6"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            val movie = results.getJSONObject(0)
            val trackName = movie.optString("trackName")
            val overview = movie.optString("longDescription", movie.optString("shortDescription", ""))
            val rawArtwork = movie.optString("artworkUrl100")
            val highResArtwork = rawArtwork.replace("100x100bb.jpg", "600x900bb.jpg")

            val primaryGenre = movie.optString("primaryGenreName")
            val releaseDate = movie.optString("releaseDate", "")
            val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4).toIntOrNull() else null

            val metadata = MediaMetadata(
                titleEnglish = trackName,
                titleRomaji = trackName,
                synopsis = overview,
                posterUrl = highResArtwork,
                rating = 8.6,
                scoreSource = "iTunes Store",
                genres = if (primaryGenre.isNotBlank()) listOf(primaryGenre) else listOf("Movie"),
                releaseYear = releaseYear ?: 2023
            )

            val recs = mutableListOf<RecommendationItem>()
            for (i in 1 until results.length()) {
                val mObj = results.getJSONObject(i)
                val tName = mObj.optString("trackName")
                val artwork = mObj.optString("artworkUrl100").replace("100x100bb.jpg", "600x900bb.jpg")
                val desc = mObj.optString("longDescription", mObj.optString("shortDescription", ""))
                val genre = mObj.optString("primaryGenreName")
                if (tName.isNotBlank()) {
                    recs.add(
                        RecommendationItem(
                            id = "itunes_$i",
                            title = tName,
                            posterUrl = artwork,
                            rating = 8.5,
                            scoreSource = "iTunes Store",
                            synopsis = desc,
                            genres = if (genre.isNotBlank()) listOf(genre) else listOf("Movie"),
                            type = MediaType.MOVIE
                        )
                    )
                }
            }

            return PublicFetchResult(metadata, recs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCuratedPosterForTitle(title: String): MediaMetadata? {
        val lower = title.lowercase()
        return when {
            lower.contains("frieren") || lower.contains("sousou") || lower.contains("فریرن") -> MediaMetadata(
                titleEnglish = "Frieren: Beyond Journey's End",
                titleRomaji = "Sousou no Frieren",
                synopsis = "الف جادوگر، فریرن، پس از شکست دادن پادشاه شیاطین همراه با یارانش، شاهد گذر زمان و پیری دوستان انسان خود می‌شود. او سفری جدید برای درک بهتر معنای زندگی و روابط انسانی آغاز می‌کند.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n2L32924L43e.png",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-S9vN98qA619N.jpg",
                rating = 9.3,
                scoreSource = "AniList / MAL",
                genres = listOf("Adventure", "Drama", "Fantasy"),
                releaseYear = 2023,
                totalEpisodes = 28
            )
            lower.contains("attack on titan") || lower.contains("shingeki") || lower.contains("تایتان") -> MediaMetadata(
                titleEnglish = "Attack on Titan",
                titleRomaji = "Shingeki no Kyojin",
                synopsis = "پس از نابودی دیوار خارجی توسط تایتان‌های غول‌پیکر، ارن یگر و دوستانش تصمیم می‌گیرند به ارتش مبارزه با تایتان‌ها بپیوندند تا راز پیدایش آن‌ها را کشف و بشریت را نجات دهند.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx16498-C6FPmWm59CyP.jpg",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/16498-83pEda33418.jpg",
                rating = 9.1,
                scoreSource = "AniList / MAL",
                genres = listOf("Action", "Drama", "Fantasy"),
                releaseYear = 2013,
                totalEpisodes = 87
            )
            lower.contains("demon slayer") || lower.contains("kimetsu") || lower.contains("شیطان کش") -> MediaMetadata(
                titleEnglish = "Demon Slayer: Kimetsu no Yaiba",
                titleRomaji = "Kimetsu no Yaiba",
                synopsis = "تانجیرو کامادو پس از قتل‌عام خانواده‌اش و تبدیل شدن خواهرش نزوکو به یک شیطان، راهی سفری خطرناک می‌شود تا خواهرش را به انسان تبدیل کرده و انتقام خون خانواده‌اش را بگیرد.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101921-2E9M6pG0M5sX.png",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101921-1s5zI5s05N6s.jpg",
                rating = 8.9,
                scoreSource = "AniList",
                genres = listOf("Action", "Supernatural", "Animation"),
                releaseYear = 2019,
                totalEpisodes = 55
            )
            lower.contains("re zero") || lower.contains("re:zero") -> MediaMetadata(
                titleEnglish = "Re:Zero - Starting Life in Another World",
                titleRomaji = "Re:Zero kara Hajimeru Isekai Seikatsu",
                synopsis = "سوبارو ناتسوکی به ناگاه به دنیایی موازی منتقل می‌شود و قدرتی مرموز دریافت می‌کند: بازگشت از مرگ. او سعی می‌کند با آزمون و خطاهای دردناک، دوستان خود را از سرنوشت‌های شوم نجات دهد.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21355-32O8E8a9s10.jpg",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21355-30E821.jpg",
                rating = 8.8,
                scoreSource = "AniList / MAL",
                genres = listOf("Drama", "Fantasy", "Mystery", "Thriller"),
                releaseYear = 2016,
                totalEpisodes = 50
            )
            lower.contains("breaking bad") -> MediaMetadata(
                titleEnglish = "Breaking Bad",
                titleRomaji = "Breaking Bad",
                synopsis = "والتر وایت، معلم شیمی دبیرستان که مبتلا به سرطان ریه شده، برای تامین آینده مالی خانواده‌اش وارد مسیر تولید و فروش مت‌آمفتامین می‌شود و جسی پینکمن را به عنوان همکار همراه می‌کند.",
                posterUrl = "https://static.tvmaze.com/uploads/images/original_untouched/501/1253519.jpg",
                bannerUrl = "https://static.tvmaze.com/uploads/images/original_untouched/501/1253520.jpg",
                rating = 9.5,
                scoreSource = "TVMaze / IMDb",
                genres = listOf("Crime", "Drama", "Thriller"),
                releaseYear = 2008,
                totalEpisodes = 62
            )
            lower.contains("oppenheimer") -> MediaMetadata(
                titleEnglish = "Oppenheimer",
                titleRomaji = "Oppenheimer",
                synopsis = "داستان زندگی فیزیک‌دان برجسته آمریکایی جی. رابرت اوپنهایمر و نقش کلیدی او در پروژه منهتن و ساخت نخستین بمب اتمی در جریان جنگ جهانی دوم.",
                posterUrl = "https://is1-ssl.mzstatic.com/image/thumb/Video116/v4/a3/37/23/a3372336-7080-6927-4a00-11756ef24a1b/400000000020.jpg/600x900bb.jpg",
                bannerUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200",
                rating = 8.9,
                scoreSource = "TMDB / IMDb",
                genres = listOf("Biography", "Drama", "History"),
                releaseYear = 2023,
                totalEpisodes = 1
            )
            lower.contains("jujutsu") || lower.contains("جوجوتسو") -> MediaMetadata(
                titleEnglish = "Jujutsu Kaisen",
                titleRomaji = "Jujutsu Kaisen",
                synopsis = "یوجی ایتادوری پس از بلعیدن طلسمی باستانی که انگشت پادشاه نفرین‌هاست، به مدرسه‌ای جادویی می‌پیوندد تا باقی قطعات طلسم را یافته و دنیا را از شر نفرین‌های مرگبار پاک کند.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113415-97M2R83q3iTj.jpg",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113415-97M2R83q3iTj.jpg",
                rating = 8.6,
                scoreSource = "AniList",
                genres = listOf("Action", "Supernatural", "Fantasy"),
                releaseYear = 2020,
                totalEpisodes = 24
            )
            lower.contains("solo leveling") || lower.contains("تک روی") -> MediaMetadata(
                titleEnglish = "Solo Leveling",
                titleRomaji = "Ore dake Hairou na Ken",
                synopsis = "در دنیایی که دروازه‌های جادویی باز شده‌اند، سونگ جین‌وو ضعیف‌ترین شکارچی جهان، پس از بقا در یک سیاهچال کشنده، سیستمی منحصر‌به‌فرد دریافت می‌کند که به او اجازه افزایش سطح لایتناهی می‌دهد.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-S1M45a6WzR43.jpg",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-S1M45a6WzR43.jpg",
                rating = 8.5,
                scoreSource = "AniList",
                genres = listOf("Action", "Adventure", "Fantasy"),
                releaseYear = 2024,
                totalEpisodes = 12
            )
            lower.contains("cyberpunk") -> MediaMetadata(
                titleEnglish = "Cyberpunk: Edgerunners",
                titleRomaji = "Cyberpunk: Edgerunners",
                synopsis = "داستان نوجوانی به نام دیوید مارتیتز که در شهری آینده‌نگرانه و پر از فناوری‌های پیشرفته و نابرابری اجتماعی، تصمیم می‌گیرد به یک قانون‌شکن به نام اج‌رانر تبدیل شود.",
                posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx120377-5O7E6gA8a30p.jpg",
                bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/120377-5O7E6gA8a30p.jpg",
                rating = 8.7,
                scoreSource = "AniList",
                genres = listOf("Action", "Sci-Fi", "Cyberpunk"),
                releaseYear = 2022,
                totalEpisodes = 10
            )
            else -> MediaMetadata(
                titleEnglish = title,
                titleRomaji = title,
                synopsis = "مجموعه آرشیو شده «$title» دارای فایل‌های ویدیویی با کیفیت بالا و جدول قسمت‌های منظم. برای دریافت هوشمند اطلاعات و کاور کامل می‌توانید کلید API رایگان را در تنظیمات وارد کنید.",
                posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600",
                bannerUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200",
                rating = 8.5,
                scoreSource = "Local Media",
                genres = listOf("HD", "Video"),
                releaseYear = 2023,
                totalEpisodes = 1
            )
        }
    }

    fun getCuratedRecommendations(title: String): List<RecommendationItem> {
        val lower = title.lowercase()
        return when {
            lower.contains("frieren") || lower.contains("sousou") -> listOf(
                RecommendationItem(
                    id = "rec_1",
                    title = "Mushoku Tensei: Jobless Reincarnation",
                    posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx108465-R9kK91s1gT4a.png",
                    rating = 8.8,
                    scoreSource = "AniList",
                    synopsis = "A 34-year-old NEET gets reincarnated into a magical world.",
                    genres = listOf("Fantasy", "Adventure"),
                    type = MediaType.ANIME
                ),
                RecommendationItem(
                    id = "rec_2",
                    title = "Violet Evergarden",
                    posterUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21827-0g7R9aQ8s4Tz.jpg",
                    rating = 8.9,
                    scoreSource = "AniList",
                    synopsis = "An ex-soldier woman becomes an Auto Memory Doll typing letters for people.",
                    genres = listOf("Drama", "Fantasy"),
                    type = MediaType.ANIME
                )
            )
            lower.contains("breaking bad") -> listOf(
                RecommendationItem(
                    id = "rec_1",
                    title = "Better Call Saul",
                    posterUrl = "https://static.tvmaze.com/uploads/images/original_untouched/423/1058284.jpg",
                    rating = 8.9,
                    scoreSource = "TVMaze",
                    synopsis = "The trials and tribulations of criminal lawyer Jimmy McGill.",
                    genres = listOf("Crime", "Drama"),
                    type = MediaType.SERIES
                ),
                RecommendationItem(
                    id = "rec_2",
                    title = "The Wire",
                    posterUrl = "https://static.tvmaze.com/uploads/images/original_untouched/12/30522.jpg",
                    rating = 9.3,
                    scoreSource = "TVMaze",
                    synopsis = "The Baltimore drug scene, through the eyes of drug dealers and law enforcement.",
                    genres = listOf("Crime", "Drama"),
                    type = MediaType.SERIES
                )
            )
            else -> listOf(
                RecommendationItem(
                    id = "rec_1",
                    title = "Interstellar",
                    posterUrl = "https://is1-ssl.mzstatic.com/image/thumb/Video114/v4/bf/25/86/bf2586ff-937e-a0e2-66b9-9a101b0e008b/pr_source.lsr/600x900bb.jpg",
                    rating = 8.7,
                    scoreSource = "TMDB",
                    synopsis = "A team of explorers travel through a wormhole in space.",
                    genres = listOf("Sci-Fi", "Drama"),
                    type = MediaType.MOVIE
                )
            )
        }
    }

    suspend fun fetchFromTMDB(cleanTitle: String, apiKey: String, mediaType: MediaType = MediaType.MOVIE): PublicFetchResult? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val endpoint = if (mediaType == MediaType.SERIES) "tv" else "movie"
            val url = "https://api.themoviedb.org/3/search/$endpoint?api_key=${apiKey.trim()}&query=$encoded&language=en-US&page=1"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext null
                if (results.length() > 0) {
                    val item = results.getJSONObject(0)
                    val title = item.optString("title", item.optString("name", cleanTitle))
                    val overview = item.optString("overview", "")
                    val posterPath = item.optString("poster_path", "")
                    val backdropPath = item.optString("backdrop_path", "")
                    val rating = item.optDouble("vote_average", 8.4)
                    val releaseDate = item.optString("release_date", item.optString("first_air_date", ""))
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4).toIntOrNull() else 2023

                    val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else null
                    val bannerUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else null

                    val metadata = MediaMetadata(
                        titleEnglish = title,
                        titleRomaji = title,
                        synopsis = overview,
                        posterUrl = posterUrl,
                        bannerUrl = bannerUrl,
                        rating = rating,
                        scoreSource = "TMDB API",
                        genres = listOf("Movie", "TMDB"),
                        releaseYear = year ?: 2023
                    )

                    val recs = mutableListOf<RecommendationItem>()
                    for (i in 1 until minOf(results.length(), 6)) {
                        val rObj = results.getJSONObject(i)
                        val rTitle = rObj.optString("title", rObj.optString("name"))
                        val rPoster = rObj.optString("poster_path")
                        if (rTitle.isNotBlank()) {
                            recs.add(
                                RecommendationItem(
                                    id = "tmdb_$i",
                                    title = rTitle,
                                    posterUrl = if (rPoster.isNotBlank()) "https://image.tmdb.org/t/p/w500$rPoster" else null,
                                    rating = rObj.optDouble("vote_average", 8.0),
                                    scoreSource = "TMDB API",
                                    synopsis = rObj.optString("overview", ""),
                                    genres = listOf("Movie"),
                                    type = mediaType
                                )
                            )
                        }
                    }

                    return@withContext PublicFetchResult(metadata, recs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchFromOMDb(cleanTitle: String, apiKey: String): PublicFetchResult? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://www.omdbapi.com/?apikey=${apiKey.trim()}&t=$encoded"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                if (json.optString("Response") == "True") {
                    val title = json.optString("Title", cleanTitle)
                    val overview = json.optString("Plot", "")
                    val poster = json.optString("Poster", "")
                    val imdbRating = json.optString("imdbRating", "8.0").toDoubleOrNull() ?: 8.0
                    val yearStr = json.optString("Year", "2023")
                    val year = yearStr.take(4).toIntOrNull() ?: 2023

                    val posterUrl = if (poster.isNotBlank() && poster != "N/A") poster else null

                    val metadata = MediaMetadata(
                        titleEnglish = title,
                        titleRomaji = title,
                        synopsis = overview,
                        posterUrl = posterUrl,
                        bannerUrl = posterUrl,
                        rating = imdbRating,
                        scoreSource = "OMDb API",
                        genres = listOf("Movie", "OMDb"),
                        releaseYear = year
                    )

                    return@withContext PublicFetchResult(metadata, emptyList())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun testTmdbApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("کلید API TMDB نمی‌تواند خالی باشد."))
        try {
            val url = "https://api.themoviedb.org/3/authentication?api_key=${apiKey.trim()}"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful && body.contains("success")) {
                Result.success(true)
            } else {
                val json = try { JSONObject(body) } catch (e: Exception) { null }
                val msg = json?.optString("status_message") ?: "کلید API معتبر نیست"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در اتصال به TMDB: ${e.message}"))
        }
    }

    suspend fun testOmdbApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("کلید API OMDb نمی‌تواند خالی باشد."))
        try {
            val url = "https://www.omdbapi.com/?apikey=${apiKey.trim()}&s=Inception"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val json = try { JSONObject(body) } catch (e: Exception) { null }
            if (response.isSuccessful && json?.optString("Response") == "True") {
                Result.success(true)
            } else {
                val msg = json?.optString("Error") ?: "کلید OMDb معتبر نیست"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در اتصال به OMDb: ${e.message}"))
        }
    }
}
