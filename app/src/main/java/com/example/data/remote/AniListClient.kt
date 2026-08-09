package com.example.data.remote

import com.example.data.model.MediaMetadata
import com.example.data.model.RecommendationItem
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AniListClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val ANILIST_URL = "https://graphql.anilist.co"

    data class AniListResult(
        val metadata: MediaMetadata,
        val recommendations: List<RecommendationItem>
    )

    suspend fun fetchAnimeMetadata(title: String): AniListResult? = withContext(Dispatchers.IO) {
        try {
            val cleanedSearch = title
                .replace(Regex("(?i)\\b(season|s|episode|ep|specials|ova)\\s*\\d+\\b"), "")
                .replace(Regex("(?i)\\b(1080p|720p|4k|2160p|bluray|x264|x265|hevc|web-dl|aac|farsisub|sub)\\b"), "")
                .replace(Regex("[._\\-]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { title }

            val searchVar = "\$search"
            val query = """
                query ($searchVar: String) {
                  Media (search: $searchVar, type: ANIME) {
                    id
                    title {
                      romaji
                      english
                      native
                    }
                    description(asHtml: false)
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    meanScore
                    genres
                    startDate {
                      year
                    }
                    episodes
                    status
                    recommendations(page: 1, perPage: 6) {
                      nodes {
                        mediaRecommendation {
                          id
                          title {
                            userPreferred
                            english
                          }
                          coverImage {
                            large
                          }
                          meanScore
                          description(asHtml: false)
                          genres
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply {
                    put("search", cleanedSearch)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(ANILIST_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: return@withContext null

            val json = JSONObject(bodyString)
            if (!json.has("data") || json.isNull("data")) return@withContext null

            val data = json.getJSONObject("data")
            if (!data.has("Media") || data.isNull("Media")) return@withContext null

            val media = data.getJSONObject("Media")

            val titleObj = media.optJSONObject("title")
            val romaji = titleObj?.optString("romaji")
            val english = titleObj?.optString("english")
            val native = titleObj?.optString("native")

            val rawDesc = media.optString("description", "")
            val cleanDesc = rawDesc.replace(Regex("<[^>]*>"), "")

            val coverObj = media.optJSONObject("coverImage")
            val posterUrl = coverObj?.optString("extraLarge") ?: coverObj?.optString("large")
            val bannerUrl = media.optString("bannerImage", null)

            val meanScore = media.optInt("meanScore", 0)
            val rating = if (meanScore > 0) meanScore / 10.0 else null

            val genresList = mutableListOf<String>()
            val genresArr = media.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(i))
                }
            }

            val startDateObj = media.optJSONObject("startDate")
            val year = startDateObj?.optInt("year", 0)
            val releaseYear = if (year != null && year > 0) year else null

            val totalEpisodes = media.optInt("episodes", 0)
            val status = media.optString("status", null)

            val metadata = MediaMetadata(
                titleRomaji = romaji,
                titleEnglish = english,
                titleNative = native,
                synopsis = cleanDesc,
                posterUrl = posterUrl,
                bannerUrl = bannerUrl,
                rating = rating,
                scoreSource = "AniList",
                genres = genresList,
                releaseYear = releaseYear,
                totalEpisodes = if (totalEpisodes > 0) totalEpisodes else null,
                status = status
            )

            // Extract Recommendations
            val recommendationsList = mutableListOf<RecommendationItem>()
            val recsNode = media.optJSONObject("recommendations")
            val recsArr = recsNode?.optJSONArray("nodes")
            if (recsArr != null) {
                for (i in 0 until recsArr.length()) {
                    val recObj = recsArr.optJSONObject(i)?.optJSONObject("mediaRecommendation") ?: continue
                    val recId = recObj.optInt("id", i).toString()
                    val recTitleObj = recObj.optJSONObject("title")
                    val recTitle = recTitleObj?.optString("english")?.takeIf { it.isNotBlank() }
                        ?: recTitleObj?.optString("userPreferred") ?: "Recommended Anime"
                    val recPoster = recObj.optJSONObject("coverImage")?.optString("large")
                    val recScore = recObj.optInt("meanScore", 0)
                    val recRating = if (recScore > 0) recScore / 10.0 else 8.0
                    val recDesc = recObj.optString("description", "").replace(Regex("<[^>]*>"), "")

                    recommendationsList.add(
                        RecommendationItem(
                            id = recId,
                            title = recTitle,
                            posterUrl = recPoster,
                            rating = recRating,
                            scoreSource = "AniList",
                            synopsis = recDesc,
                            genres = genresList,
                            type = MediaType.ANIME
                        )
                    )
                }
            }

            AniListResult(metadata, recommendationsList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
