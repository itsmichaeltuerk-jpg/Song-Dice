package com.songdice.audio

import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavSynthesizerTest {

    @Test
    fun testWavHeaderStructure() {
        val sampleCount = 44100 // 1 second of stereo audio
        val dataSize = sampleCount * 2 * 2 // 2 channels, 16-bit (2 bytes per sample)
        val header = WavSynthesizer.createWavHeader(dataSize)

        assertEquals(44, header.size)

        // RIFF Chunk Marker
        val riffMarker = String(header, 0, 4)
        assertEquals("RIFF", riffMarker)

        // WAVE Header Marker
        val waveMarker = String(header, 8, 4)
        assertEquals("WAVE", waveMarker)

        // fmt Chunk Marker
        val fmtMarker = String(header, 12, 4)
        assertEquals("fmt ", fmtMarker)

        // data Chunk Marker
        val dataMarker = String(header, 36, 4)
        assertEquals("data", dataMarker)

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        val chunkSize = buffer.getInt(4)
        assertEquals(36 + dataSize, chunkSize)

        val audioFormat = buffer.getShort(20).toInt()
        assertEquals(1, audioFormat) // PCM

        val numChannels = buffer.getShort(22).toInt()
        assertEquals(2, numChannels)

        val sampleRate = buffer.getInt(24)
        assertEquals(44100, sampleRate)

        val bitsPerSample = buffer.getShort(34).toInt()
        assertEquals(16, bitsPerSample)

        val subchunk2Size = buffer.getInt(40)
        assertEquals(dataSize, subchunk2Size)
    }

    @Test
    fun testByteCalculationMath() {
        val bpm = 120
        val totalBeats = 16.0 // 4 bars in 4/4 time
        val secondsPerBeat = 60.0 / bpm // 0.5 s/beat
        val totalDurationSec = totalBeats * secondsPerBeat // 8.0 seconds

        val expectedSamples = (totalDurationSec * WavSynthesizer.SAMPLE_RATE).toInt()
        val expectedDataSize = expectedSamples * WavSynthesizer.NUM_CHANNELS * (WavSynthesizer.BITS_PER_SAMPLE / 8)
        val expectedTotalFileSize = 44 + expectedDataSize

        val tracks = listOf(
            InstrumentTrack("Chords", 0, listOf(MidiNote(60, 0.0, 4.0))),
            InstrumentTrack("Bass", 1, listOf(MidiNote(36, 0.0, 4.0))),
            InstrumentTrack("Lead", 2, listOf(MidiNote(72, 0.0, 2.0))),
            InstrumentTrack("Drums", 9, listOf(MidiNote(36, 0.0, 0.25)))
        )

        val wavBytes = WavSynthesizer.renderToWav(tracks, bpm, totalBeats)

        assertEquals(expectedTotalFileSize, wavBytes.size)

        val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
        val dataSizeHeader = buffer.getInt(40)
        assertEquals(expectedDataSize, dataSizeHeader)
    }

    @Test
    fun testNonEmptyAudioGenerationFromFourBarProgression() {
        val chordNotes = listOf(
            MidiNote(60, 0.0, 4.0), // C4
            MidiNote(64, 0.0, 4.0), // E4
            MidiNote(67, 0.0, 4.0), // G4

            MidiNote(57, 4.0, 4.0), // A3
            MidiNote(60, 4.0, 4.0), // C4
            MidiNote(64, 4.0, 4.0), // E4

            MidiNote(53, 8.0, 4.0), // F3
            MidiNote(57, 8.0, 4.0), // A3
            MidiNote(60, 8.0, 4.0), // C4

            MidiNote(55, 12.0, 4.0), // G3
            MidiNote(59, 12.0, 4.0), // B3
            MidiNote(62, 12.0, 4.0)  // D4
        )

        val bassNotes = listOf(
            MidiNote(36, 0.0, 4.0),
            MidiNote(33, 4.0, 4.0),
            MidiNote(29, 8.0, 4.0),
            MidiNote(31, 12.0, 4.0)
        )

        val leadNotes = listOf(
            MidiNote(72, 0.0, 1.0),
            MidiNote(74, 1.0, 1.0),
            MidiNote(76, 2.0, 2.0),
            MidiNote(77, 4.0, 2.0),
            MidiNote(76, 6.0, 2.0)
        )

        val drumNotes = mutableListOf<MidiNote>()
        for (beat in 0 until 16) {
            val b = beat.toDouble()
            if (beat % 4 == 0) drumNotes.add(MidiNote(36, b, 0.25)) // Kick
            if (beat % 4 == 2) drumNotes.add(MidiNote(38, b, 0.25)) // Snare
            drumNotes.add(MidiNote(42, b + 0.5, 0.25)) // Hat
        }

        val arrangement = SongArrangement(
            title = "Test 4-Bar Song",
            genre = "Synthwave",
            key = "C Major",
            bpm = 120,
            progression = "I - vi - IV - V",
            tracks = listOf(
                InstrumentTrack("Chords", 0, chordNotes),
                InstrumentTrack("Bass", 1, bassNotes),
                InstrumentTrack("Lead", 2, leadNotes),
                InstrumentTrack("Drums", 9, drumNotes)
            )
        )

        val wavBytes = AudioPreviewRenderer.renderPreview(arrangement, 16.0)

        assertTrue(wavBytes.size > 44)

        // Inspect PCM audio samples after header (44 bytes) to ensure non-zero signal synthesis
        var nonZeroSampleCount = 0
        var maxAmplitude = 0

        val sampleBuffer = ByteBuffer.wrap(wavBytes, 44, wavBytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
        while (sampleBuffer.hasRemaining()) {
            val sample = kotlin.math.abs(sampleBuffer.getShort().toInt())
            if (sample > 0) {
                nonZeroSampleCount++
            }
            if (sample > maxAmplitude) {
                maxAmplitude = sample
            }
        }

        val totalPcmSamples = (wavBytes.size - 44) / 2
        assertTrue("Expected non-zero samples in audio signal", nonZeroSampleCount > totalPcmSamples * 0.5)
        assertTrue("Expected peak amplitude to indicate active audio signal generation", maxAmplitude > 1000)
    }
}
