package com.songdice.export

import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAssetGeneratorTest {

    private val sampleArrangement = SongArrangement(
        title = "Neon Midnight",
        genre = "Synthwave / Cyberpunk",
        key = "A Minor",
        bpm = 115,
        progression = "i - VI - III - VII",
        tracks = listOf(
            InstrumentTrack(
                trackName = "Drums",
                channel = 9,
                notes = listOf(MidiNote(pitch = 36, startBeat = 0.0, durationBeats = 0.5))
            ),
            InstrumentTrack(
                trackName = "Bass",
                channel = 0,
                notes = listOf(MidiNote(pitch = 33, startBeat = 0.0, durationBeats = 1.0))
            ),
            InstrumentTrack(
                trackName = "Chords",
                channel = 1,
                notes = listOf(
                    MidiNote(pitch = 57, startBeat = 0.0, durationBeats = 2.0),
                    MidiNote(pitch = 60, startBeat = 0.0, durationBeats = 2.0),
                    MidiNote(pitch = 64, startBeat = 0.0, durationBeats = 2.0)
                )
            )
        )
    )

    private val sampleDice = mapOf(
        DiceParameter.KEY to DiceState(DiceParameter.KEY, "A Minor", isLocked = true),
        DiceParameter.PROGRESSION to DiceState(DiceParameter.PROGRESSION, "i - VI - III - VII", isLocked = false),
        DiceParameter.DRUM_RHYTHM to DiceState(DiceParameter.DRUM_RHYTHM, "Four-on-the-Floor House", isLocked = false),
        DiceParameter.BASS_PATTERN to DiceState(DiceParameter.BASS_PATTERN, "Funky Slap & Pop", isLocked = false)
    )

    @Test
    fun testLeadSheetFormatterOutputContainsExpectedSectionsAndMetadata() {
        val leadSheet = LeadSheetFormatter.formatLeadSheet(sampleArrangement, sampleDice)

        // Verify section tags
        assertTrue("Lead sheet should contain SONG METADATA tag", leadSheet.contains("[SONG METADATA]"))
        assertTrue("Lead sheet should contain SONG STRUCTURE tag", leadSheet.contains("[SONG STRUCTURE & CHORD PROGRESSION]"))
        assertTrue("Lead sheet should contain VOICINGS tag", leadSheet.contains("[VOICINGS & ASCII KEYBOARD DIAGRAMS]"))

        // Verify metadata values
        assertTrue("Lead sheet should contain title", leadSheet.contains("Title: Neon Midnight"))
        assertTrue("Lead sheet should contain genre", leadSheet.contains("Genre: Synthwave / Cyberpunk"))
        assertTrue("Lead sheet should contain key", leadSheet.contains("Key: A Minor"))
        assertTrue("Lead sheet should contain BPM", leadSheet.contains("Tempo: 115 BPM"))
        assertTrue("Lead sheet should contain progression", leadSheet.contains("Chord Progression: i - VI - III - VII"))

        // Verify voicing lines and ASCII keyboard rendering
        assertTrue("Lead sheet should contain LH Bass lines", leadSheet.contains("Left-Hand (LH) Bass:"))
        assertTrue("Lead sheet should contain RH Chord lines", leadSheet.contains("Right-Hand (RH) Chord:"))
        assertTrue("Lead sheet should contain ASCII keyboard diagram label", leadSheet.contains("ASCII Keyboard Diagram:"))
        assertTrue("Lead sheet should render ASCII keyboard frame", leadSheet.contains("+---+---+---+---+---+---+---+"))
        assertTrue("Lead sheet should contain LH indicator row", leadSheet.contains("LH: |"))
        assertTrue("Lead sheet should contain RH indicator row", leadSheet.contains("RH: |"))
    }

    @Test
    fun testLeadSheetFormatterPitchToNoteNameAndVoicings() {
        assertEquals("C4", LeadSheetFormatter.pitchToNoteName(60))
        assertEquals("A0", LeadSheetFormatter.pitchToNoteName(21))
        assertEquals("G#4", LeadSheetFormatter.pitchToNoteName(68))

        val parsedChords = LeadSheetFormatter.parseProgressionChords("i - VI - III - VII", "A Minor")
        assertEquals(4, parsedChords.size)
        assertEquals("Am", parsedChords[0])
        assertEquals("F", parsedChords[1])

        val voicings = LeadSheetFormatter.generateVoicings(parsedChords, "A Minor")
        assertEquals(4, voicings.size)
        assertEquals("Am", voicings[0].chordName)
        assertTrue(voicings[0].lhNotes.contains("A2"))
        assertTrue(voicings[0].rhNotes.contains("A4"))
        assertTrue(voicings[0].rhNotes.contains("C4"))
        assertTrue(voicings[0].rhNotes.contains("E4"))
    }

    @Test
    fun testAIPromptFormatterOutputContainsDecoupledTagsAndPlatformPrompts() {
        val aiPrompt = AIPromptFormatter.formatAIPrompt(
            arrangement = sampleArrangement,
            diceStates = sampleDice,
            userStylePrompt = "dark retro synthwave"
        )

        // Verify section tags
        assertTrue("AI Prompt should contain STYLE & PRODUCTION TAGS tag", aiPrompt.contains("[STYLE & PRODUCTION TAGS]"))
        assertTrue("AI Prompt should contain PLATFORM PROMPTS tag", aiPrompt.contains("[PLATFORM PROMPTS]"))
        assertTrue("AI Prompt should contain STRUCTURAL LYRICS tag", aiPrompt.contains("[STRUCTURAL LYRICS]"))

        // Verify platform specific headers
        assertTrue("AI Prompt should contain Suno header", aiPrompt.contains("--- Suno AI Prompt ---"))
        assertTrue("AI Prompt should contain Udio header", aiPrompt.contains("--- Udio Prompt Tags ---"))
        assertTrue("AI Prompt should contain Google Flow Music header", aiPrompt.contains("--- Google Flow Music Prompt ---"))

        // Verify Suno tag format
        assertTrue("Suno prompt should use [Style: ...] syntax", aiPrompt.contains("[Style: Synthwave / Cyberpunk"))

        // Verify Google Flow syntax
        assertTrue("Google Flow prompt should use pipe delimiter", aiPrompt.contains("Genre: Synthwave / Cyberpunk | Tempo: 115 BPM"))

        // Verify structural lyrics tags
        assertTrue("Lyrics should contain Verse 1 tag", aiPrompt.contains("[Verse 1]"))
        assertTrue("Lyrics should contain Chorus tag", aiPrompt.contains("[Chorus]"))
        assertTrue("Lyrics should contain Outro tag", aiPrompt.contains("[Outro]"))
    }

    @Test
    fun testAIPromptFormatterCustomLyricsFormatting() {
        val rawLyrics = "Line 1 in the city\nLine 2 in the light\nLine 3 feeling ready\nLine 4 late at night"
        val formatted = AIPromptFormatter.formatLyrics(rawLyrics)

        assertTrue(formatted.contains("[Verse 1]"))
        assertTrue(formatted.contains("[Chorus]"))
        assertTrue(formatted.contains("Line 1 in the city"))
    }

    @Test
    fun testAIPromptFormatterDecoupledStyleTagExtraction() {
        val tags = AIPromptFormatter.extractStyleTags(
            genre = "Synthwave",
            bpm = 115,
            diceStates = sampleDice,
            userStylePrompt = "dark melancholic vibe"
        )

        assertEquals("Synthwave", tags.genre)
        assertEquals("115 BPM", tags.tempo)
        assertTrue(tags.instruments.contains("Analog Synthesizer"))
        assertTrue(tags.instruments.contains("Slap Bass"))
        assertTrue(tags.mood.contains("Melancholic"))
    }
}
