package com.example

import com.example.data.model.MediaType
import com.example.data.parser.FileNameParser
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameParserTest {

    @Test
    fun testAnimeParsing() {
        val fileName = "[SubGroup] Jujutsu_Kaisen_S02E12_1080p.mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Jujutsu Kaisen", parsed.cleanTitle)
        assertEquals(2, parsed.season)
        assertEquals(12, parsed.episode)
        assertEquals("1080P", parsed.resolution)
        assertEquals("SubGroup", parsed.releaseGroup)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testMovieParsing() {
        val fileName = "Oppenheimer.2023.2160p.HEVC.mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Oppenheimer", parsed.cleanTitle)
        assertEquals(2023, parsed.year)
        assertEquals("2160P", parsed.resolution)
        assertEquals("HEVC", parsed.codec)
        assertEquals(MediaType.MOVIE, parsed.detectedType)
    }

    @Test
    fun testFrierenEpisodeParsing() {
        val fileName = "Sousou_no_Frieren_E28.mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Sousou no Frieren", parsed.cleanTitle)
        assertEquals(28, parsed.episode)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testPersianSiteNoiseTagsDetectAnime() {
        val fileName = "[AioFilm] One Piece S10E5 1080p.mp4"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("One Piece", parsed.cleanTitle)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testPersianSubtitleTagDetectAnime() {
        val fileName = "Jujutsu.Kaisen.S01E01.1080p.FarsiSub.mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Jujutsu Kaisen", parsed.cleanTitle)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testSoft98Anime() {
        val fileName = "Attack on Titan S04E01 1080p Soft98.mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Attack on Titan", parsed.cleanTitle)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testSubsPleaseStyleRelease() {
        val fileName = "[SubsPlease] One Piece 1101 (1080p) [1F3C2A1D].mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("One Piece", parsed.cleanTitle)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testTrailingDashEpisode() {
        val fileName = "[Erai-raws] Vinland Saga - 08 [720p][Multiple Subtitle].mkv"
        val parsed = FileNameParser.parse(fileName)

        assertEquals("Vinland Saga", parsed.cleanTitle)
        assertEquals(8, parsed.episode)
        assertEquals(MediaType.ANIME, parsed.detectedType)
    }

    @Test
    fun testOvaAndSpecialsDetectAnime() {
        val ova = FileNameParser.parse("Attack on Titan OVA S01E05.mkv")
        assertEquals("Attack on Titan", ova.cleanTitle)
        assertEquals(MediaType.ANIME, ova.detectedType)

        val special = FileNameParser.parse("Demon Slayer Kimetsu no Yaiba Special 01.mkv")
        assertEquals("Demon Slayer: Kimetsu no Yaiba", special.cleanTitle)
        assertEquals(MediaType.ANIME, special.detectedType)
    }

    @Test
    fun testFranchiseNormalization() {
        assertEquals(
            "Sousou no Frieren",
            FileNameParser.parse("Frieren - Beyond Journey's End S01E03 1080p.mkv").cleanTitle
        )
        assertEquals(
            "Attack on Titan",
            FileNameParser.parse("Shingeki no Kyojin E02.mkv").cleanTitle
        )
        assertEquals(
            "Naruto",
            FileNameParser.parse("Naruto Shippuden S01E01.mkv").cleanTitle
        )
        assertEquals(
            "Re:Zero - Starting Life in Another World",
            FileNameParser.parse("Re Zero kara Hajimeru Isekai Seikatsu S02E05.mkv").cleanTitle
        )
        assertEquals(
            "Hunter x Hunter",
            FileNameParser.parse("Hunter x Hunter 2011 E01.mkv").cleanTitle
        )
    }

    @Test
    fun testWesternSeriesNotDetectedAsAnime() {
        val friends = FileNameParser.parse("Friends S01E02 1080p.mkv")
        assertEquals("Friends", friends.cleanTitle)
        assertEquals(MediaType.SERIES, friends.detectedType)

        val breakingBad = FileNameParser.parse("Breaking Bad S01E01.mkv")
        assertEquals("Breaking Bad", breakingBad.cleanTitle)
        assertEquals(MediaType.SERIES, breakingBad.detectedType)
    }

    @Test
    fun testCanonicalizationForGrouping() {
        assertEquals(
            FileNameParser.canonicalizeTitle("Frieren - Beyond Journey's End 1080p.mkv"),
            FileNameParser.canonicalizeTitle("Sousou no Frieren E01 720p.mkv")
        )
        assertEquals(
            FileNameParser.canonicalizeTitle("Attack on Titan S01E01 1080p.mkv"),
            FileNameParser.canonicalizeTitle("Shingeki no Kyojin E02.mkv")
        )
    }
}
