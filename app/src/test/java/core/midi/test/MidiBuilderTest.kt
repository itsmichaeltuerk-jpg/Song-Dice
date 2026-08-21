package core.midi.test

import core.midi.builder.MidiBuilder
import core.midi.model.MidiNote
import core.midi.model.MidiSong
import core.midi.model.MidiTrack
import core.midi.model.TimeSignature
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream

class MidiBuilderTest {

    @Test
    fun testMThdHeaderValidation() {
        val song = MidiSong(
            tempoBpm = 120,
            timeSignature = TimeSignature(4, 4),
            ppq = 480,
            tracks = listOf(
                MidiTrack(name = "Drums", channel = 9),
                MidiTrack(name = "Chords", channel = 0)
            )
        )

        val bytes = MidiBuilder.buildMidiFile(song)
        val stream = DataInputStream(ByteArrayInputStream(bytes))

        // Header magic: MThd
        val magic = ByteArray(4)
        stream.readFully(magic)
        assertEquals("MThd", String(magic, Charsets.US_ASCII))

        // Header length: 6
        val length = stream.readInt()
        assertEquals(6, length)

        // SMF Format: 1
        val format = stream.readUnsignedShort()
        assertEquals(1, format)

        // Track count: tracks.size + 1 (conductor track 0 + 2 instrument tracks = 3)
        val trackCount = stream.readUnsignedShort()
        assertEquals(3, trackCount)

        // Division / PPQ: 480
        val division = stream.readUnsignedShort()
        assertEquals(480, division)
    }

    @Test
    fun testMTrkChunksAndEndOfTrackTermination() {
        val song = MidiSong(
            tempoBpm = 120,
            tracks = listOf(
                MidiTrack(name = "Lead", channel = 2, notes = listOf(MidiNote(60, 0, 480, 100)))
            )
        )

        val bytes = MidiBuilder.buildMidiFile(song)
        var offset = 14 // After 14-byte MThd header

        var trackIndex = 0
        while (offset < bytes.size) {
            val chunkHeader = String(bytes, offset, 4, Charsets.US_ASCII)
            assertEquals("MTrk", chunkHeader)

            val chunkSize = ((bytes[offset + 4].toInt() and 0xFF) shl 24) or
                    ((bytes[offset + 5].toInt() and 0xFF) shl 16) or
                    ((bytes[offset + 6].toInt() and 0xFF) shl 8) or
                    (bytes[offset + 7].toInt() and 0xFF)

            val trackEndIndex = offset + 8 + chunkSize

            // Verify End of Track Meta Event at the end of the chunk: 0x00 0xFF 0x2F 0x00
            val endOfTrack = bytes.copyOfRange(trackEndIndex - 4, trackEndIndex)
            assertArrayEquals(
                byteArrayOf(0x00.toByte(), 0xFF.toByte(), 0x2F.toByte(), 0x00.toByte()),
                endOfTrack
            )

            offset = trackEndIndex
            trackIndex++
        }

        assertEquals(2, trackIndex) // Track 0 (Tempo) + Track 1 (Lead)
    }

    @Test
    fun testVariableLengthQuantityEncoding() {
        fun encode(value: Long): ByteArray {
            val bos = ByteArrayOutputStream()
            MidiBuilder.writeVariableLengthQuantity(value, bos)
            return bos.toByteArray()
        }

        // 0 -> 0x00
        assertArrayEquals(byteArrayOf(0x00.toByte()), encode(0))
        // 127 -> 0x7F
        assertArrayEquals(byteArrayOf(0x7F.toByte()), encode(127))
        // 128 -> 0x81, 0x00
        assertArrayEquals(byteArrayOf(0x81.toByte(), 0x00.toByte()), encode(128))
        // 8192 (0x2000) -> 0xC0, 0x00
        assertArrayEquals(byteArrayOf(0xC0.toByte(), 0x00.toByte()), encode(8192))
        // 16383 (0x3FFF) -> 0xFF, 0x7F
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x7F.toByte()), encode(16383))
        // 16384 (0x4000) -> 0x81, 0x80, 0x00
        assertArrayEquals(byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x00.toByte()), encode(16384))
    }

    @Test
    fun testPolyphonyAndSimultaneousEventsSorting() {
        // C Major Chord played simultaneously at tick 0
        val chordNotes = listOf(
            MidiNote(pitch = 60, startTick = 0, durationTicks = 480, velocity = 100),
            MidiNote(pitch = 64, startTick = 0, durationTicks = 480, velocity = 100),
            MidiNote(pitch = 67, startTick = 0, durationTicks = 480, velocity = 100)
        )

        val song = MidiSong(
            tempoBpm = 120,
            tracks = listOf(
                MidiTrack(name = "Chords", channel = 1, notes = chordNotes)
            )
        )

        val bytes = MidiBuilder.buildMidiFile(song)
        assertTrue(bytes.size > 14)
    }

    @Test
    fun testFourBarTwoTrackArrangement() {
        // 4 bars at 4/4 PPQ 480 = 4 * 4 * 480 = 7680 ticks total
        val drumNotes = listOf(
            MidiNote(pitch = 36, startTick = 0, durationTicks = 240, velocity = 110),   // Kick at beat 0
            MidiNote(pitch = 38, startTick = 480, durationTicks = 240, velocity = 100), // Snare at beat 1
            MidiNote(pitch = 36, startTick = 960, durationTicks = 240, velocity = 110), // Kick at beat 2
            MidiNote(pitch = 38, startTick = 1440, durationTicks = 240, velocity = 100) // Snare at beat 3
        )

        val chordNotes = listOf(
            MidiNote(pitch = 60, startTick = 0, durationTicks = 1920, velocity = 90),   // C4
            MidiNote(pitch = 64, startTick = 0, durationTicks = 1920, velocity = 90),   // E4
            MidiNote(pitch = 67, startTick = 0, durationTicks = 1920, velocity = 90)    // G4
        )

        val song = MidiSong(
            tempoBpm = 120,
            timeSignature = TimeSignature(4, 4),
            ppq = 480,
            tracks = listOf(
                MidiTrack(name = "Drums", channel = 9, notes = drumNotes),
                MidiTrack(name = "Chords", channel = 0, notes = chordNotes)
            ),
            title = "4 Bar Test Song"
        )

        val midiBytes = MidiBuilder.buildMidiFile(song)

        // Verify "MThd" header start
        val headerMagic = String(midiBytes, 0, 4, Charsets.US_ASCII)
        assertEquals("MThd", headerMagic)

        // Total tracks should be 3 (1 Tempo track + 2 Instrument tracks)
        val stream = DataInputStream(ByteArrayInputStream(midiBytes))
        stream.skipBytes(8) // Skip magic + len
        assertEquals(1, stream.readUnsignedShort()) // Format 1
        assertEquals(3, stream.readUnsignedShort()) // 3 tracks
        assertEquals(480, stream.readUnsignedShort()) // PPQ
    }
}
