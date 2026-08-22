package core.midi.builder

import core.midi.model.MidiSong
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Pure Kotlin MIDI File Builder class compliant with Standard MIDI File (SMF 1.0) Specification Type 1.
 * Operates without external Java/Android framework dependencies for use in unit tests and background coroutines.
 */
object MidiBuilder {

    private data class MidiEvent(
        val tick: Long,
        val type: EventType,
        val channel: Int,
        val pitch: Int,
        val velocity: Int
    ) : Comparable<MidiEvent> {
        // NOTE_OFF comes before NOTE_ON when ticks are identical to prevent cutoffs
        enum class EventType { NOTE_OFF, NOTE_ON }

        override fun compareTo(other: MidiEvent): Int {
            if (this.tick != other.tick) return this.tick.compareTo(other.tick)
            if (this.type != other.type) return this.type.compareTo(other.type)
            return this.pitch.compareTo(other.pitch)
        }
    }

    /**
     * Builds a standard Type 1 SMF byte array from a [MidiSong].
     */
    fun buildMidiFile(song: MidiSong): ByteArray {
        val bos = ByteArrayOutputStream()

        val totalTracks = song.tracks.size + 1 // Track 0 = Conductor track + instrument tracks

        // 1. MThd Header Chunk
        bos.write("MThd".toByteArray(Charsets.US_ASCII))
        bos.writeInt32(6) // Length of header data
        bos.writeInt16(1) // SMF Format 1
        bos.writeInt16(totalTracks)
        bos.writeInt16(song.ppq)

        // 2. Track 0: Conductor / Tempo & Time Signature Track
        val track0Data = ByteArrayOutputStream().apply {
            // Track Name
            val trackName = if (song.title.isNotEmpty()) song.title else "Tempo Track"
            writeMetaEvent(0x03, trackName.toByteArray(Charsets.UTF_8))

            // Set Tempo Meta Event (microseconds per quarter note)
            val bpm = song.tempoBpm.coerceIn(1, 1000)
            val usPerQuarter = 60_000_000 / bpm
            writeVariableLengthQuantity(0, this)
            write(0xFF)
            write(0x51)
            write(0x03)
            write((usPerQuarter ushr 16) and 0xFF)
            write((usPerQuarter ushr 8) and 0xFF)
            write(usPerQuarter and 0xFF)

            // Time Signature Meta Event
            val num = song.timeSignature.numerator.coerceIn(1, 32)
            val den = song.timeSignature.denominator.coerceIn(1, 32)
            // Log2 of denominator (e.g. 4 -> 2 since 2^2 = 4)
            val denLog2 = (ln(den.toDouble()) / ln(2.0)).roundToInt().coerceIn(0, 7)

            writeVariableLengthQuantity(0, this)
            write(0xFF)
            write(0x58)
            write(0x04)
            write(num)
            write(denLog2)
            write(24) // Clocks per metronome click
            write(8)  // 32nd notes per 24 MIDI clocks

            // End of Track Meta Event
            writeVariableLengthQuantity(0, this)
            write(0xFF)
            write(0x2F)
            write(0x00)
        }.toByteArray()

        writeTrackChunk(bos, track0Data)

        // 3. Instrument Tracks
        for (track in song.tracks) {
            val trackData = ByteArrayOutputStream().apply {
                // Track Name Meta Event
                writeMetaEvent(0x03, track.name.toByteArray(Charsets.UTF_8))

                val channel = track.channel.coerceIn(0, 15)

                // Program Change (GM Instrument Patch selection at tick 0)
                val programPatch = when {
                    track.name.equals("Drums", ignoreCase = true) || channel == 9 -> 0
                    track.name.equals("Bass", ignoreCase = true) || channel == 0 -> 33 // Finger Bass
                    track.name.equals("Chords", ignoreCase = true) || channel == 1 -> 4  // Electric Piano
                    else -> 81 // Lead 2 (sawtooth)
                }
                writeVariableLengthQuantity(0, this)
                write(0xC0 or channel)
                write(programPatch and 0x7F)

                // Collect Note On / Note Off events
                val events = mutableListOf<MidiEvent>()
                for (note in track.notes) {
                    val startTick = note.startTick.coerceAtLeast(0)
                    val durationTicks = note.durationTicks.coerceAtLeast(1)
                    val endTick = startTick + durationTicks
                    val pitch = note.pitch.coerceIn(0, 127)
                    val vel = note.velocity.coerceIn(1, 127)

                    events.add(MidiEvent(startTick, MidiEvent.EventType.NOTE_ON, channel, pitch, vel))
                    events.add(MidiEvent(endTick, MidiEvent.EventType.NOTE_OFF, channel, pitch, 0))
                }

                // Sort events strictly by absolute tick time, then type (Note Off before Note On)
                events.sort()

                // Delta-time encoding
                var lastTick = 0L
                for (event in events) {
                    val deltaTime = event.tick - lastTick
                    lastTick = event.tick

                    writeVariableLengthQuantity(deltaTime, this)
                    val statusByte = if (event.type == MidiEvent.EventType.NOTE_ON) {
                        0x90 or channel
                    } else {
                        0x80 or channel
                    }
                    write(statusByte)
                    write(event.pitch and 0x7F)
                    write(event.velocity and 0x7F)
                }

                // End of Track Meta Event
                writeVariableLengthQuantity(0, this)
                write(0xFF)
                write(0x2F)
                write(0x00)
            }.toByteArray()

            writeTrackChunk(bos, trackData)
        }

        return bos.toByteArray()
    }

    /**
     * Writes a value encoded in Standard MIDI Variable-Length Quantity (VLQ) format to [output].
     */
    fun writeVariableLengthQuantity(value: Long, output: OutputStream) {
        var v = value.coerceAtLeast(0)
        val buffer = mutableListOf<Byte>()
        buffer.add((v and 0x7F).toByte())
        v = v ushr 7
        while (v > 0) {
            buffer.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        for (i in buffer.size - 1 downTo 0) {
            output.write(buffer[i].toInt() and 0xFF)
        }
    }

    private fun writeTrackChunk(bos: ByteArrayOutputStream, trackData: ByteArray) {
        bos.write("MTrk".toByteArray(Charsets.US_ASCII))
        bos.writeInt32(trackData.size)
        bos.write(trackData)
    }

    private fun ByteArrayOutputStream.writeMetaEvent(type: Int, data: ByteArray) {
        writeVariableLengthQuantity(0, this)
        write(0xFF)
        write(type)
        writeVariableLengthQuantity(data.size.toLong(), this)
        write(data)
    }

    private fun ByteArrayOutputStream.writeInt32(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArrayOutputStream.writeInt16(value: Int) {
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }
}
