package com.example.data.parser

import com.example.data.model.MediaType
import com.example.data.model.ParsedFileInfo
import java.util.regex.Pattern

object FileNameParser {

    // Regex patterns for quality, resolutions, codecs, containers, tags, and download sites
    private val BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]|\\((.*?)\\)")
    private val WEBSITE_DOMAIN_REGEX = "(?i)\\b(www\\.)?[a-zA-Z0-9-]+\\.(ir|com|net|org|co|info|biz|tv|cc|io|me|in|site|xyz|moe|app)\\b".toRegex()
    private val SITE_NOISE_TAGS = "(?i)\\b(AioFilm|Film2Media|AnimeList|Soft98|DigiMoviez|ZarFilm|FilmBaran|Film2Movie|Bi2Media|MovieSub|300MB|FarsiSub|FaSub|Dubbed|Persian|Farsi|DualAudio|MultiSub|SoftSub|HardSub|DownloadedFrom|Telegram|Channel|t\\.me/[a-zA-Z0-9_]+|@[a-zA-Z0-9_]+)\\b".toRegex()
    private val RESOLUTION_REGEX = "(?i)\\b(2160p|1080p|720p|480p|360p|4k|uhd|hd|sd)\\b".toRegex()
    private val CODEC_REGEX = "(?i)\\b(x264|x265|hevc|avc|h264|h265|xvid|divx|av1|10bit|8bit|aac|flac|dts|truehd|ac3|5\\.1|7\\.1)\\b".toRegex()
    private val SOURCE_REGEX = "(?i)\\b(bluray|bdrip|brrip|web-dl|webdl|webrip|hdtv|dvdrip|remux|hdrip)\\b".toRegex()
    private val YEAR_REGEX = "\\b(19\\d\\d|20\\d\\d)\\b".toRegex()
    
    // Season & Episode Regex Patterns
    // S01E05, S1E2, S1 Ep2, S01 Ep05, Season 1 Episode 2, 01x05, E12, EP05, Episode 12
    private val SEASON_EPISODE_REGEX = "(?i)\\bS(\\d{1,2})\\s*(?:E|EP|EPISODE)?\\s*(\\d{1,3})\\b".toRegex()
    private val SEASON_WORD_EPISODE_REGEX = "(?i)\\bSeason\\s*(\\d{1,2})\\s*(?:Episode|Ep|E)?\\s*(\\d{1,3})\\b".toRegex()
    private val SEASON_ONLY_REGEX = "(?i)\\b(?:Season|S)\\s*(\\d{1,2})\\b".toRegex()
    private val ALT_SEASON_EP_REGEX = "(\\d{1,2})x(\\d{1,3})".toRegex()
    private val EPISODE_TAG_REGEX = "(?i)\\b(?:E|EP|Episode|Ep\\.)\\s*(\\d{1,3})\\b".toRegex()
    private val SEASON_TEXT_REGEX = "(?i)\\b(\\d{1,2}(st|nd|rd|th)?\\s*)?(season|cour|part|arc)\\b".toRegex()
    private val FINAL_SEASON_REGEX = "(?i)\\b(the\\s*)?final\\s*season\\b".toRegex()
    private val TRAILING_EPISODE_REGEX = "(?i)\\s*-\\s*\\d{1,3}\\b|\\b\\d{1,3}\\s*$".toRegex()

    fun parse(fileName: String): ParsedFileInfo {
        // Strip extension (e.g. .mkv, .mp4)
        val nameWithoutExt = fileName.substringBeforeLast('.')
        
        // Normalize delimiters (. and _) to spaces early for clean regex matching
        var workingName = nameWithoutExt.replace('.', ' ').replace('_', ' ')
        var extractedGroup: String? = null
        var season: Int? = null
        var episode: Int? = null
        var year: Int? = null
        var resolution: String? = null
        var codec: String? = null

        // 1. Extract Release Group from leading bracket, e.g. [SubGroup]
        val bracketMatcher = BRACKET_PATTERN.matcher(workingName)
        if (bracketMatcher.find()) {
            val groupCandidate = bracketMatcher.group(1) ?: bracketMatcher.group(2)
            if (groupCandidate != null && !groupCandidate.contains("1080") && !groupCandidate.contains("720")) {
                extractedGroup = groupCandidate.trim()
            }
        }

        // 2. Extract Resolution
        RESOLUTION_REGEX.find(workingName)?.let {
            resolution = it.value.uppercase()
        }

        // 3. Extract Codec
        CODEC_REGEX.find(workingName)?.let {
            codec = it.value.uppercase()
        }

        // 4. Extract Season & Episode
        SEASON_EPISODE_REGEX.find(workingName)?.let { match ->
            season = match.groupValues[1].toIntOrNull()
            episode = match.groupValues[2].toIntOrNull()
        } ?: run {
            SEASON_WORD_EPISODE_REGEX.find(workingName)?.let { match ->
                season = match.groupValues[1].toIntOrNull()
                episode = match.groupValues[2].toIntOrNull()
            } ?: run {
                ALT_SEASON_EP_REGEX.find(workingName)?.let { match ->
                    season = match.groupValues[1].toIntOrNull()
                    episode = match.groupValues[2].toIntOrNull()
                } ?: run {
                    EPISODE_TAG_REGEX.find(workingName)?.let { match ->
                        episode = match.groupValues[2].toIntOrNull()
                    }
                }
            }
        }

        if (season == null) {
            SEASON_ONLY_REGEX.find(workingName)?.let { match ->
                season = match.groupValues[1].toIntOrNull()
            }
        }

        // 5. Extract Year
        YEAR_REGEX.findAll(workingName).forEach { match ->
            val candidate = match.value.toIntOrNull()
            if (candidate != null && candidate in 1920..2030) {
                year = candidate
            }
        }

        // 6. Clean Title: Replace website domains, site noise tags, bracket contents, and season/episode identifiers
        var cleanTitle = BRACKET_PATTERN.matcher(workingName).replaceAll(" ")
        cleanTitle = WEBSITE_DOMAIN_REGEX.replace(cleanTitle, " ")
        cleanTitle = SITE_NOISE_TAGS.replace(cleanTitle, " ")
        
        // Remove resolution, codec, source, year, and SxxExx
        cleanTitle = RESOLUTION_REGEX.replace(cleanTitle, " ")
        cleanTitle = CODEC_REGEX.replace(cleanTitle, " ")
        cleanTitle = SOURCE_REGEX.replace(cleanTitle, " ")
        if (year != null) {
            cleanTitle = cleanTitle.replace(year.toString(), " ")
        }
        cleanTitle = SEASON_EPISODE_REGEX.replace(cleanTitle, " ")
        cleanTitle = SEASON_WORD_EPISODE_REGEX.replace(cleanTitle, " ")
        cleanTitle = ALT_SEASON_EP_REGEX.replace(cleanTitle, " ")
        cleanTitle = EPISODE_TAG_REGEX.replace(cleanTitle, " ")
        cleanTitle = SEASON_ONLY_REGEX.replace(cleanTitle, " ")
        cleanTitle = SEASON_TEXT_REGEX.replace(cleanTitle, " ")
        cleanTitle = FINAL_SEASON_REGEX.replace(cleanTitle, " ")
        cleanTitle = TRAILING_EPISODE_REGEX.replace(cleanTitle, " ")

        // Replace hyphens or duplicate spaces
        cleanTitle = cleanTitle.replace('-', ' ')

        // Strip remaining site tags or website domains after expansion
        cleanTitle = WEBSITE_DOMAIN_REGEX.replace(cleanTitle, " ")
        cleanTitle = SITE_NOISE_TAGS.replace(cleanTitle, " ")

        // Trim duplicate whitespaces
        cleanTitle = cleanTitle.replace("\\s+".toRegex(), " ").trim()

        // Normalize anime franchise names (e.g., Attack on Titan, Re Zero, Sousou no Frieren)
        cleanTitle = normalizeFranchiseTitle(cleanTitle)

        // 7. Determine Media Type
        val isAnime = isAnimeCandidate(fileName) || extractedGroup != null
        val detectedType = when {
            isAnime -> MediaType.ANIME
            season != null || episode != null -> MediaType.SERIES
            year != null || resolution != null -> MediaType.MOVIE
            else -> MediaType.UNKNOWN
        }

        return ParsedFileInfo(
            cleanTitle = if (cleanTitle.isBlank()) nameWithoutExt else cleanTitle,
            season = season,
            episode = episode,
            year = year,
            resolution = resolution,
            codec = codec,
            releaseGroup = extractedGroup,
            detectedType = detectedType
        )
    }

    fun isAnimeCandidate(titleOrFilename: String): Boolean {
        val lower = titleOrFilename.lowercase()
        val animeKeywords = listOf(
            "sub", "farsisub", "softsub", "hardsub", "dualaudio", "japanese", "jap",
            "ova", "ona", "special", "gekijouban", "movie", "animelist", "anime",
            "sousou", "jujutsu", "naruto", "one piece", "bleach", "attack on titan",
            "shingeki", "re zero", "re:zero", "frieren", "kimetsu", "demon slayer",
            "solo leveling", "chainsaw", "death note", "spy x family", "edgerunners",
            "dragon ball", "hero academia", "boku no", "kaisen", "yaiba", "hunter x",
            "fullmetal", "tokyo ghoul", "haikyuu", "steins gate", "code geass",
            "overlord", "sword art", "sao", "no game no life", "vinland", "dr stone",
            "oshi no ko", "mushoku tensei", "slime", "shield hero", "black clover"
        )
        if (animeKeywords.any { lower.contains(it) }) return true
        if (lower.contains("[") && lower.contains("]")) return true
        return false
    }

    private fun normalizeFranchiseTitle(title: String): String {
        var t = title
        // Strip trailing episode numbers or solitary digits if left over
        t = t.replace("(?i)\\b(episode|ep)?\\s*\\d{1,3}$".toRegex(), "").trim()
        
        // Match known franchise root names (both English and Persian transliterations)
        val lower = t.lowercase()
        return when {
            lower.contains("attack on titan") || lower.contains("shingeki no kyojin") || lower.contains("shingeki") || lower.contains("اتک آن تایتان") -> "Attack on Titan"
            lower.contains("re zero") || lower.contains("re:zero") -> "Re:Zero - Starting Life in Another World"
            lower.contains("frieren") || lower.contains("sousou no frieren") || lower.contains("فریرن") -> "Sousou no Frieren"
            lower.contains("jujutsu kaisen") || lower.contains("jujutsu") || lower.contains("جوجوتسو") -> "Jujutsu Kaisen"
            lower.contains("kimetsu no yaiba") || lower.contains("demon slayer") || lower.contains("شیطان کش") -> "Demon Slayer: Kimetsu no Yaiba"
            lower.contains("one piece") || lower.contains("وان پیس") -> "One Piece"
            lower.contains("naruto") || lower.contains("ناروتو") -> "Naruto"
            lower.contains("bleach") || lower.contains("بلیچ") -> "Bleach"
            lower.contains("solo leveling") || lower.contains("تک روی") -> "Solo Leveling"
            lower.contains("chainsaw man") || lower.contains("مرد اره ای") -> "Chainsaw Man"
            lower.contains("death note") || lower.contains("دفترچه مرگ") -> "Death Note"
            lower.contains("spy x family") || lower.contains("spy family") || lower.contains("اسپای خانواده") -> "Spy x Family"
            lower.contains("breaking bad") -> "Breaking Bad"
            lower.contains("cyberpunk") -> "Cyberpunk: Edgerunners"
            else -> t
        }
    }
}
