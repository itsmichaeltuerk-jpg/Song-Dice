package com.songdice.audio

import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class WavSynthesizerQualityTest {

    @Test
    fun testMasterBusPeakAmplitudeBoundedBelowHeadroomLimit() {
        // Create dense polyphonic arrangement with all 4 channels playing concurrently
        val chordNotes = listOf(
            MidiNote(60, 0.0, 4.0, velocity = 127), // C4
            MidiNote(64, 0.0, 4.0, velocity = 127), // E4
            MidiNote(67, 0.0, 4.0, velocity = 127), // G4
            MidiNote(71, 0.0, 4.0, velocity = 127)  // B4
        )
        val bassNotes = listOf(MidiNote(36, 0.0, 4.0, velocity = 127))
        val leadNotes = listOf(MidiNote(72, 0.0, 4.0, velocity = 127))
        val drumNotes = listOf(
            MidiNote(36, 0.0, 0.5, velocity = 127),
            MidiNote(38, 0.0, 0.5, velocity = 127)
        )

        val arrangement = SongArrangement(
            title = "Polyphonic Loudness Test",
            genre = "EDM",
            key = "C Major",
            bpm = 128,
            progression = "I",
            tracks = listOf(
                InstrumentTrack("Chords", 0, chordNotes),
                InstrumentTrack("Bass", 1, bassNotes),
                InstrumentTrack("Lead", 2, leadNotes),
                InstrumentTrack("Drums", 9, drumNotes)
            )
        )

        val wavBytes = WavSynthesizer.renderToWav(arrangement, 4.0)
        assertTrue(wavBytes.size > 44)

        var maxSampleValue = 0
        val pcmBuffer = ByteBuffer.wrap(wavBytes, 44, wavBytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)

        while (pcmBuffer.hasRemaining()) {
            val sample = abs(pcmBuffer.getShort().toInt())
            if (sample > maxSampleValue) {
                maxSampleValue = sample
            }
        }

        // Peak amplitude must not exceed 0.95 * 32767 = ~31129
        val maxAllowedAmplitude = (32767 * 0.95).toInt()
        assertTrue(
            "Peak amplitude ($maxSampleValue) must be bounded below peak limit ($maxAllowedAmplitude)",
            maxSampleValue <= maxAllowedAmplitude
        )
    }

    @Test
    fun testEnvelopeSmoothingPreventsDCBreachesAtSampleBoundaries() {
        val singleNoteTrack = listOf(
            InstrumentTrack(
                "Lead",
                2,
                listOf(MidiNote(60, 0.0, 1.0, velocity = 100))
            )
        )

        val wavBytes = WavSynthesizer.renderToWav(singleNoteTrack, bpm = 120, totalBeats = 2.0)
        assertTrue(wavBytes.size > 44)

        val pcmBuffer = ByteBuffer.wrap(wavBytes, 44, wavBytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
        val firstSampleL = abs(pcmBuffer.getShort().toInt())
        val firstSampleR = abs(pcmBuffer.getShort().toInt())

        // Smooth attack ensures initial frame starts near zero amplitude without DC pop
        assertTrue("First left sample ($firstSampleL) must be near zero", firstSampleL < 100)
        assertTrue("First right sample ($firstSampleR) must be near zero", firstSampleR < 100)
    }

    @Test
    fun testHeaderFieldExactnessAndSubchunkSizes() {
        val totalBeats = 16.0
        val bpm = 120
        val secondsPerBeat = 60.0 / bpm
        val totalDurationSec = totalBeats * secondsPerBeat // 8.0s
        val expectedSamples = (totalDurationSec * WavSynthesizer.SAMPLE_RATE).toInt()
        val expectedDataSize = expectedSamples * WavSynthesizer.NUM_CHANNELS * (WavSynthesizer.BITS_PER_SAMPLE / 8)

        val arrangement = SongArrangement(
            title = "Header Exactness Test",
            genre = "Pop",
            key = "C Major",
            bpm = bpm,
            progression = "I - V - vi - IV",
            tracks = listOf(
                InstrumentTrack("Chords", 0, listOf(MidiNote(60, 0.0, 12.0)))
            )
        )

        val wavBytes = WavSynthesizer.renderToWav(arrangement, totalBeats)
        val headerBuffer = ByteBuffer.wrap(wavBytes, 0, 44).order(ByteOrder.LITTLE_ENDIAN)

        // Verify RIFF header chunk
        val riffTag = String(wavBytes, 0, 4)
        assertEquals("RIFF", riffTag)
        val fileSize = headerBuffer.getInt(4)
        assertEquals(36 + expectedDataSize, fileSize)

        val waveTag = String(wavBytes, 8, 4)
        assertEquals("WAVE", waveTag)

        // Verify fmt subchunk
        val fmtTag = String(wavBytes, 12, 4)
        assertEquals("fmt ", fmtTag)
        val audioFormat = headerBuffer.getShort(20).toInt()
        assertEquals(1, audioFormat) // PCM format = 1
        val numChannels = headerBuffer.getShort(22).toInt()
        assertEquals(2, numChannels)
        val sampleRate = headerBuffer.getInt(24)
        assertEquals(44100, sampleRate)

        // Verify data subchunk
        val dataTag = String(wavBytes, 36, 4)
        assertEquals("data", dataTag)
        val subchunk2Size = headerBuffer.getInt(40)
        assertEquals(expectedDataSize, subchunk2Size)
    }
}
