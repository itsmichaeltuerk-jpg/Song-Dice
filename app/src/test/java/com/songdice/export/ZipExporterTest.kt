package com.songdice.export

import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.MusicalGenre
import com.example.songdice.data.model.SongArrangement
import com.songdice.data.model.SongBlueprint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ZipExporterTest {

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

    private val sampleBlueprint = SongBlueprint(
        title = "Neon Midnight",
        genre = MusicalGenre.SYNTHWAVE,
        bpm = 115,
        keySignature = "A Minor",
        progression = "i - VI - III - VII",
        arrangement = sampleArrangement,
        diceStates = mapOf(
            DiceParameter.KEY to DiceState(DiceParameter.KEY, "A Minor", isLocked = true),
            DiceParameter.PROGRESSION to DiceState(DiceParameter.PROGRESSION, "i - VI - III - VII", isLocked = false)
        )
    )

    private val zipExporter = ZipExporter()

    @Test
    fun testCreateZipBundleContainsExactlyFourCoreAssets() {
        val zipBytes = zipExporter.createZipBundle(sampleBlueprint)
        assertNotNull(zipBytes)
        assertTrue("Zip byte array should not be empty", zipBytes.isNotEmpty())

        val entryNames = mutableListOf<String>()
        val entryPayloads = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                val payload = zis.readBytes()
                entryPayloads[entry.name] = payload
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals("Zip bundle should contain exactly 4 entries", 4, entryNames.size)
        assertTrue("Contains song_idea.mid", entryNames.contains("song_idea.mid"))
        assertTrue("Contains preview.wav", entryNames.contains("preview.wav"))
        assertTrue("Contains LeadSheet.txt", entryNames.contains("LeadSheet.txt"))
        assertTrue("Contains AIPrompt.txt", entryNames.contains("AIPrompt.txt"))

        // Verify all entries are non-empty
        entryPayloads.forEach { (name, bytes) ->
            assertTrue("Payload for $name should be non-empty", bytes.isNotEmpty())
        }

        // Verify song_idea.mid starts with 'MThd' header bytes
        val midiPayload = entryPayloads["song_idea.mid"]!!
        val midiHeader = String(midiPayload.copyOfRange(0, 4), Charsets.US_ASCII)
        assertEquals("MThd", midiHeader)

        // Verify preview.wav starts with 'RIFF' header bytes
        val wavPayload = entryPayloads["preview.wav"]!!
        val wavHeader = String(wavPayload.copyOfRange(0, 4), Charsets.US_ASCII)
        assertEquals("RIFF", wavHeader)

        // Verify LeadSheet.txt text content
        val leadSheetStr = String(entryPayloads["LeadSheet.txt"]!!, Charsets.UTF_8)
        assertTrue(leadSheetStr.contains("SONG DICE • LEAD SHEET"))
        assertTrue(leadSheetStr.contains("Title: Neon Midnight"))

        // Verify AIPrompt.txt text content
        val aiPromptStr = String(entryPayloads["AIPrompt.txt"]!!, Charsets.UTF_8)
        assertTrue(aiPromptStr.contains("SONG DICE • AI PROMPT ASSET"))
        assertTrue(aiPromptStr.contains("Suno AI Prompt"))
    }

    @Test
    fun testGetBundleFileNameSanitizesIllegalCharacters() {
        val specialBlueprint = sampleBlueprint.copy(
            title = "Song / Idea #1 (Demo)",
            keySignature = "F# Minor",
            bpm = 120
        )

        val fileName = zipExporter.getBundleFileName(specialBlueprint)
        assertEquals("Song_Dice_Song_Idea_sharp1_(Demo)_Fsharp_Minor_120BPM.zip", fileName)
    }
}
