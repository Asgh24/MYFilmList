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
    // Fansub-style trailing episode: "Title - 08", "Title - 08v2"
    private val TRAILING_DASH_EPISODE_REGEX = "(?i)\\s*-\\s*(\\d{1,3})(?:v\\d+)?\\b".toRegex()
    private val SEASON_TEXT_REGEX = "(?i)\\b(\\d{1,2}(st|nd|rd|th)?\\s*)?(season|cour|part|arc)\\b".toRegex()
    private val FINAL_SEASON_REGEX = "(?i)\\b(the\\s*)?final\\s*season\\b".toRegex()
    private val TRAILING_EPISODE_REGEX = "(?i)\\s*-\\s*\\d{1,3}\\b|\\b\\d{1,3}\\s*$".toRegex()

    // Heuristic tags that strongly indicate a fansub / anime release
    private val ANIME_SUB_TAG_REGEX =
        "(?i)\\b(farsisub|farsi sub|softsub|soft-sub|hardsub|hard-sub|dualaudio|dual-audio|japanese|jap|ova|ona|gekijouban|gekijoban|dub|subbed)\\b".toRegex()

    // Fansub groups that usually appear in the leading bracket of anime releases
    private val FANSUB_GROUP_HINTS = listOf(
        "subsplease", "erai-raws", "horriblesubs", "fansub", "subgroup", "ohys-raws",
        "sny", "conclave", "mawen", "nzb", "dame desu yo", "okay-sub", "nyaasi", "nya"
    )

    // ---------------------------------------------------------------------
    // Known anime / animation franchises used for BOTH smart anime detection
    // and canonical title normalization. Keys: canonical franchise name.
    // Values: distinctive keywords (English, Romaji, and Persian variants).
    // ---------------------------------------------------------------------
    private val ANIME_FRANCHISES: Map<String, List<String>> = linkedMapOf(
        "Attack on Titan" to listOf("attack on titan", "shingeki no kyojin", "shingeki", "اتک آن تایتان", "تایتان"),
        "Jujutsu Kaisen" to listOf("jujutsu kaisen", "jujutsu", "جوجوتسو کایسن", "جوجوتسو"),
        "Demon Slayer: Kimetsu no Yaiba" to listOf("demon slayer", "kimetsu no yaiba", "kimetsu", "yaiba", "شیطان کش"),
        "One Piece" to listOf("one piece", "وان پیس"),
        "Naruto" to listOf("naruto shippuden", "naruto", "ناروتو"),
        "Boruto" to listOf("boruto"),
        "Bleach" to listOf("bleach tybw", "bleach"),
        "Solo Leveling" to listOf("solo leveling", "ore dake level up", "تک روی"),
        "Chainsaw Man" to listOf("chainsaw man", "چینساو من", "مرد اره ای"),
        "Death Note" to listOf("death note", "دفترچه مرگ"),
        "Spy x Family" to listOf("spy x family", "spy family", "اسپای فمیلی", "خانواده جاسوسی"),
        "Sousou no Frieren" to listOf("sousou no frieren", "sousou", "frieren", "فریرن"),
        "Re:Zero - Starting Life in Another World" to listOf("re zero", "re:zero", "rezero"),
        "Sword Art Online" to listOf("sword art online", "swordart", "sao", "سفیدپوشان"),
        "One Punch Man" to listOf("one punch man", "onepunch", "opm", "وان پانچ من"),
        "My Hero Academia" to listOf("my hero academia", "boku no hero", "hero academia", "آکادمی قهرمان من"),
        "Vinland Saga" to listOf("vinland saga", "vinland", "وینلند"),
        "Code Geass" to listOf("code geass", "geass", "کد گیاس"),
        "Fullmetal Alchemist" to listOf("fullmetal alchemist", "full metal alchemist", "brotherhood", "fma"),
        "Steins;Gate" to listOf("steins gate", "steins;gate", "stein gate"),
        "Tokyo Ghoul" to listOf("tokyo ghoul", "غول توکیو"),
        "Haikyuu!!" to listOf("haikyuu", "haikyu", "هایکیو"),
        "Hunter x Hunter" to listOf("hunter x hunter", "hunterxhunter", "hunter hxh", "hxh", "هانتر ایکس هانتر"),
        "Dragon Ball" to listOf("dragon ball", "dragonball", "dbz", "dbs", "دراگون بال"),
        "Dr. Stone" to listOf("dr stone", "dr.stone", "دکتر استون"),
        "Mushoku Tensei" to listOf("mushoku tensei", "jobless reincarnation", "mushoku"),
        "That Time I Got Reincarnated as a Slime" to listOf(
            "that time i got reincarnated as a slime", "tensura", "tensei shitara slime", "slime"
        ),
        "No Game No Life" to listOf("no game no life", "ngnl"),
        "Overlord" to listOf("overlord"),
        "Black Clover" to listOf("black clover", "بلک کلاور"),
        "Made in Abyss" to listOf("made in abyss"),
        "Kaguya-sama: Love Is War" to listOf("kaguya-sama", "kaguya sama", "kaguya"),
        "Bocchi the Rock!" to listOf("bocchi the rock", "bocchi"),
        "Mob Psycho 100" to listOf("mob psycho", "mobpsycho"),
        "The Promised Neverland" to listOf("promised neverland"),
        "Death Parade" to listOf("death parade"),
        "Your Name" to listOf("your name", "kimi no na wa", "kiminonawa"),
        "Spirited Away" to listOf("spirited away", "sen to chihiro"),
        "My Neighbor Totoro" to listOf("my neighbor totoro", "tonari no totoro", "totoro"),
        "Howl's Moving Castle" to listOf("howl's moving castle", "howls moving castle", "hauro no ugoku shiro", "howl"),
        "Princess Mononoke" to listOf("princess mononoke", "mononoke"),
        "Grave of the Fireflies" to listOf("grave of the fireflies", "hotaru no haka"),
        "Akira" to listOf("akira 1988", "akira film", "akira (1988)"),
        "Ghost in the Shell" to listOf("ghost in the shell"),
        "Neon Genesis Evangelion" to listOf("neon genesis evangelion", "evangelion"),
        "Cowboy Bebop" to listOf("cowboy bebop"),
        "Samurai Champloo" to listOf("samurai champloo", "champloo"),
        "Trigun" to listOf("trigun"),
        "Gurren Lagann" to listOf("gurren lagann", "ttgl"),
        "Kill la Kill" to listOf("kill la kill", "killlakill"),
        "Parasyte" to listOf("parasyte", "kiseijuu"),
        "Erased" to listOf("boku dake ga inai machi", "erased"),
        "Berserk" to listOf("berserk"),
        "Tokyo Revengers" to listOf("tokyo revengers"),
        "Fruits Basket" to listOf("fruits basket"),
        "Beastars" to listOf("beastars"),
        "Odd Taxi" to listOf("odd taxi"),
        "Vivy" to listOf("vivy"),
        "Lycoris Recoil" to listOf("lycoris recoil", "lycoris"),
        "86 Eighty-Six" to listOf("eighty six", "86 eightysix", "86 (eighty"),
        "Blue Lock" to listOf("blue lock", "blue lock"),
        "Assassination Classroom" to listOf("assassination classroom", "ansatsu kyoushitsu"),
        "Great Teacher Onizuka" to listOf("great teacher onizuka", "gto"),
        "The Disastrous Life of Saiki K." to listOf("saiki"),
        "Toradora!" to listOf("toradora"),
        "Clannad" to listOf("clannad"),
        "Violet Evergarden" to listOf("violet evergarden"),
        "Classroom of the Elite" to listOf("classroom of the elite", "youkoso jitsuryoku", "youjitsu"),
        "Horimiya" to listOf("horimiya"),
        "Ranking of Kings" to listOf("ranking of kings", "ousama ranking"),
        "To Your Eternity" to listOf("to your eternity", "fumetsu no anata e"),
        "Fire Force" to listOf("fire force", "en'en no shouboutai"),
        "Soul Eater" to listOf("soul eater"),
        "Fairy Tail" to listOf("fairy tail"),
        "Anohana" to listOf("anohana"),
        "Angel Beats!" to listOf("angel beats"),
        "Plastic Memories" to listOf("plastic memories"),
        "Your Lie in April" to listOf("your lie in april", "shigatsu wa kimi no uso"),
        "A Silent Voice" to listOf("a silent voice", "koe no katachi"),
        "Weathering with You" to listOf("weathering with you", "tenki no ko"),
        "Suzume" to listOf("suzume"),
        "The Boy and the Heron" to listOf("boy and the heron", "kimitachi wa dou ikiru"),
        "KonoSuba" to listOf("konosuba", "kono subarashii"),
        "Goblin Slayer" to listOf("goblin slayer"),
        "The Rising of the Shield Hero" to listOf("shield hero", "tate no yuusha"),
        "DanMachi" to listOf("danmachi", "is it wrong to try"),
        "The Saga of Tanya the Evil" to listOf("tanya the evil", "youjo senki"),
        "Log Horizon" to listOf("log horizon"),
        "Grimgar" to listOf("grimgar"),
        "Re:Creators" to listOf("re creators"),
        "Fate" to listOf("fate/stay night", "fate stay night", "fate zero", "fate/grand order", "fate grand order", "unlimited blade works", "fgo"),
        "Monogatari" to listOf("monogatari", "bakemonogatari"),
        "Oshi no Ko" to listOf("oshi no ko"),
        "Bungo Stray Dogs" to listOf("bungo"),
        "Noragami" to listOf("noragami"),
        "Banana Fish" to listOf("banana fish"),
        "Kaiju No. 8" to listOf("kaiju no 8", "kaiju no 8", "kaiju"),
        "Mashle" to listOf("mashle"),
        "The Apothecary Diaries" to listOf("apothecary diaries", "kusuriya no hitorigoto"),
        "Delicious in Dungeon" to listOf("delicious in dungeon", "dungeon meshi"),
        "The Eminence in Shadow" to listOf("eminence in shadow", "kage no jitsuryokusha"),
        "Hell's Paradise" to listOf("hell's paradise", "jigokuraku"),
        "Heavenly Delusion" to listOf("heavenly delusion", "tengoku daimakyou"),
        "A Place Further than the Universe" to listOf("a place further than the universe", "sora yori mo tooi basho"),
        "K-On!" to listOf("k-on", "k on", "keion"),
        "Love Live!" to listOf("love live"),
        "Sound! Euphonium" to listOf("sound euphonium", "hibike"),
        "Detective Conan" to listOf("detective conan", "meitantei konan", "conan", "کارآگاه کونان"),
        "Pokemon" to listOf("pokemon", "pocket monsters", "پوکمون"),
        "Crayon Shin-chan" to listOf("shin chan", "crayon shin-chan"),
        "Doraemon" to listOf("doraemon"),
        "Cyberpunk: Edgerunners" to listOf("cyberpunk edgerunners", "cyberpunk", "edgerunners"),
        "The Ancient Magus' Bride" to listOf("ancient magus bride", "mahoutsukai no yome"),
        "The Seven Deadly Sins" to listOf("seven deadly sins", "nanatsu no taizai")
    )

    // Non-anime franchise normalizations (western series / movies)
    private val WESTERN_FRANCHISES: Map<String, List<String>> = linkedMapOf(
        "Breaking Bad" to listOf("breaking bad"),
        "Better Call Saul" to listOf("better call saul"),
        "Game of Thrones" to listOf("game of thrones", "got"),
        "House of the Dragon" to listOf("house of the dragon"),
        "The Walking Dead" to listOf("walking dead"),
        "Stranger Things" to listOf("stranger things"),
        "The Boys" to listOf("the boys"),
        "The Witcher" to listOf("the witcher"),
        "Peaky Blinders" to listOf("peaky blinders"),
        "The Office" to listOf("the office"),
        "Friends" to listOf("friends s", "friends (tv"),
        "Sherlock" to listOf("sherlock"),
        "Dark" to listOf("dark s"),
        "Money Heist" to listOf("money heist", "la casa de papel"),
        "Squid Game" to listOf("squid game"),
        "Wednesday" to listOf("wednesday"),
        "The Last of Us" to listOf("last of us"),
        "Oppenheimer" to listOf("oppenheimer"),
        "Interstellar" to listOf("interstellar"),
        "Inception" to listOf("inception"),
        "The Dark Knight" to listOf("dark knight"),
        "The Lord of the Rings" to listOf("lord of the rings", "lotr"),
        "The Hobbit" to listOf("the hobbit"),
        "Star Wars" to listOf("star wars"),
        "Harry Potter" to listOf("harry potter"),
        "Avengers" to listOf("avengers"),
        "Spider-Man" to listOf("spider-man", "spiderman"),
        "Batman" to listOf("batman"),
        "Superman" to listOf("superman"),
        "John Wick" to listOf("john wick"),
        "The Matrix" to listOf("the matrix"),
        "Pulp Fiction" to listOf("pulp fiction"),
        "The Godfather" to listOf("godfather"),
        "Titanic" to listOf("titanic"),
        "Forrest Gump" to listOf("forrest gump"),
        "The Shawshank Redemption" to listOf("shawshank"),
        "Fight Club" to listOf("fight club"),
        "The Social Network" to listOf("social network"),
        "Joker" to listOf("joker"),
        "Dune" to listOf("dune"),
        "Top Gun" to listOf("top gun")
    )

    private val ALL_FRANCHISES: Map<String, List<String>> = ANIME_FRANCHISES + WESTERN_FRANCHISES

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

        // 1. Extract Release Group from bracket tags, skipping resolution/codec-like values
        val bracketMatcher = BRACKET_PATTERN.matcher(workingName)
        while (bracketMatcher.find()) {
            val groupCandidate = (bracketMatcher.group(1) ?: bracketMatcher.group(2))?.trim()
            if (!groupCandidate.isNullOrBlank() && !looksLikeTechnicalTag(groupCandidate)) {
                extractedGroup = groupCandidate
                break
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
                        episode = match.groupValues[1].toIntOrNull()
                    } ?: run {
                        TRAILING_DASH_EPISODE_REGEX.find(workingName)?.let { match ->
                            val candidate = match.groupValues[1].toIntOrNull()
                            // Guard against misreading years (e.g. "Movie - 2001")
                            if (candidate != null && candidate !in 1920..2030) {
                                episode = candidate
                            }
                        }
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
        val isAnime = isAnimeCandidate(fileName) || (extractedGroup != null && extractedGroup.isFansubGroup())
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

    /**
     * Best-effort canonical title for a raw file name / title. Used by smart
     * grouping so different spellings of the same franchise collapse into one.
     */
    fun canonicalizeTitle(title: String): String = parse(title).cleanTitle

    fun isAnimeCandidate(titleOrFilename: String): Boolean {
        val lower = titleOrFilename.lowercase()

        // 1. Strong fansub / subtitle / anime-specific tags
        if (ANIME_SUB_TAG_REGEX.containsMatchIn(titleOrFilename)) return true

        // 2. Known fansub group names
        if (FANSUB_GROUP_HINTS.any { lower.contains(it) }) return true

        // 3. Known anime franchise keywords (English / Romaji / Persian)
        if (ANIME_FRANCHISES.values.any { keywords -> keywords.any { lower.contains(it) } }) return true

        return false
    }

    private fun normalizeFranchiseTitle(title: String): String {
        var t = title
        // Strip trailing episode numbers or solitary digits if left over
        t = t.replace("(?i)\\b(episode|ep)?\\s*\\d{1,3}$".toRegex(), "").trim()

        if (t.isBlank()) return title

        val lower = t.lowercase()

        // 1. Anime franchise database
        ANIME_FRANCHISES.forEach { (canonical, keywords) ->
            if (keywords.any { lower.contains(it) }) return canonical
        }

        // 2. Western series / movie database
        WESTERN_FRANCHISES.forEach { (canonical, keywords) ->
            if (keywords.any { lower.contains(it) }) return canonical
        }

        return t
    }

    private fun String.isFansubGroup(): Boolean {
        val lower = lowercase()
        if (lower.contains("sub")) return true
        return FANSUB_GROUP_HINTS.any { lower.contains(it) }
    }

    private fun looksLikeTechnicalTag(value: String): Boolean {
        val lower = value.lowercase()
        if (RESOLUTION_REGEX.containsMatchIn(value)) return true
        if (CODEC_REGEX.containsMatchIn(value)) return true
        if (SOURCE_REGEX.containsMatchIn(value)) return true
        if (lower.contains("mb") || lower.contains("gb") || lower.contains("size")) return true
        if (lower.contains("yts") || lower.contains("yify") || lower.contains("rarbg") ||
            lower.contains("mkvcage") || lower.contains("fgt") || lower.contains("psa") ||
            lower.contains("evo") || lower.contains("amzn") || lower.contains("web") ||
            lower.contains("hd4fun") || lower.contains("dvd")
        ) return true
        return false
    }
}
