package com.example

import com.example.data.model.MediaType
import com.example.data.parser.FileNameParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
