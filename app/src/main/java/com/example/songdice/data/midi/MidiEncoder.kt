package com.example.songdice.data.midi

import com.example.songdice.data.model.SongArrangement
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Utility to encode a [SongArrangement] into a standard Type 1 Multi-Track MIDI file (.mid).
 * Strictly complies with Standard MIDI File (SMF 1.0) spec for full DAW compatibility (FL Studio, Ableton, Logic, Pro Tools).
 */
object MidiEncoder {

    const val TICKS_PER_QUARTER_NOTE = 480

    private data class MidiEvent(
        val tick: Long,
        val type: EventType,
        val channel: Int,
        val pitch: Int,
        val velocity: Int
    ) : Comparable<MidiEvent> {
        // Critical: NOTE_OFF must come BEFORE NOTE_ON when ticks are identical (ordinal 0 vs 1)
        enum class EventType { NOTE_OFF, NOTE_ON }

        override fun compareTo(other: MidiEvent): Int {
            if (this.tick != other.tick) return this.tick.compareTo(other.tick)
            if (this.type != other.type) return this.type.compareTo(other.type)
            return this.pitch.compareTo(other.pitch)
        }
    }

    /**
     * Encodes the [arrangement] and saves it as a standard .mid file in the specified output file.
     * Returns the generated File object.
     */
    fun createMidiFile(arrangement: SongArrangement, outputFile: File): File {
        val bos = ByteArrayOutputStream()

        val numTracks = arrangement.tracks.size + 1 // +1 for Tempo/Meta Track 0

        // 1. MThd Header Chunk (Standard MIDI Header)
        bos.write("MThd".toByteArray(Charsets.US_ASCII))
        bos.writeInt32(6) // Header chunk data length = 6
        bos.writeInt16(1) // Format Type 1 (Multi-track synchronous)
        bos.writeInt16(numTracks) // Number of tracks
        bos.writeInt16(TICKS_PER_QUARTER_NOTE) // Division (480 ticks per quarter note)

        // 2. Track 0: Conductor / Tempo & Time Signature Meta Track
        val track0Data = ByteArrayOutputStream().apply {
            // Track Name Meta Event
            writeMetaEvent(0x03, "Tempo Track".toByteArray(Charsets.UTF_8))

            // Set Tempo Meta Event (Microseconds per quarter note)
            val bpm = arrangement.bpm.coerceIn(40, 280)
            val usPerQuarter = 60_000_000 / bpm
            writeVlq(0) // Delta time 0
            write(0xFF)
            write(0x51)
            write(0x03)
            write((usPerQuarter ushr 16) and 0xFF)
            write((usPerQuarter ushr 8) and 0xFF)
            write(usPerQuarter and 0xFF)

            // Time Signature Meta Event (4/4)
            writeVlq(0) // Delta time 0
            write(0xFF)
            write(0x58)
            write(0x04)
            write(0x04) // Numerator 4
            write(0x02) // Denominator 2^2 = 4
            write(24)   // Clocks per metronome click
            write(8)    // 32nd notes per 24 MIDI clocks

            // End of Track Meta Event
            writeVlq(0)
            write(0xFF)
            write(0x2F)
            write(0x00)
        }.toByteArray()

        writeTrackChunk(bos, track0Data)

        // 3. Instrument Tracks (Track 1..N)
        for (track in arrangement.tracks) {
            val trackData = ByteArrayOutputStream().apply {
                // Track Name Meta Event
                writeMetaEvent(0x03, track.trackName.toByteArray(Charsets.UTF_8))

                val channel = track.channel.coerceIn(0, 15)

                // Add Program Change (GM Instrument Patch selection at tick 0)
                val programPatch = when {
                    track.trackName.equals("Drums", ignoreCase = true) || channel == 9 -> 0
                    track.trackName.equals("Bass", ignoreCase = true) || channel == 0 -> 33 // Finger Bass
                    track.trackName.equals("Chords", ignoreCase = true) || channel == 1 -> 4  // Electric Piano
                    else -> 81 // Lead 2 (sawtooth)
                }
                writeVlq(0) // Delta time 0
                write(0xC0 or channel) // Program Change status
                write(programPatch and 0x7F)

                // Collect and sort all Note On and Note Off events
                val events = mutableListOf<MidiEvent>()

                for (note in track.notes) {
                    val startTick = (note.startBeat * TICKS_PER_QUARTER_NOTE).toLong().coerceAtLeast(0)
                    val durationTicks = (note.durationBeats * TICKS_PER_QUARTER_NOTE).toLong().coerceAtLeast(1)
                    val endTick = startTick + durationTicks
                    val pitch = note.pitch.coerceIn(0, 127)
                    val vel = note.velocity.coerceIn(1, 127)

                    events.add(MidiEvent(startTick, MidiEvent.EventType.NOTE_ON, channel, pitch, vel))
                    events.add(MidiEvent(endTick, MidiEvent.EventType.NOTE_OFF, channel, pitch, 0))
                }

                events.sort()

                // Output events with relative delta-times
                var lastTick = 0L
                for (event in events) {
                    val deltaTime = event.tick - lastTick
                    lastTick = event.tick

                    writeVlq(deltaTime)
                    // Standard Note On / Note Off status byte (0x90 with velocity 0 is standard Note Off)
                    val statusByte = 0x90 or channel
                    val eventVel = if (event.type == MidiEvent.EventType.NOTE_ON) {
                        event.velocity.coerceIn(1, 127)
                    } else {
                        0
                    }
                    write(statusByte)
                    write(event.pitch and 0x7F)
                    write(eventVel and 0x7F)
                }

                // End of Track Meta Event
                writeVlq(0)
                write(0xFF)
                write(0x2F)
                write(0x00)
            }.toByteArray()

            writeTrackChunk(bos, trackData)
        }

        // Save to target output file
        FileOutputStream(outputFile).use { fos ->
            bos.writeTo(fos)
        }

        return outputFile
    }

    private fun writeTrackChunk(bos: ByteArrayOutputStream, trackData: ByteArray) {
        bos.write("MTrk".toByteArray(Charsets.US_ASCII))
        bos.writeInt32(trackData.size)
        bos.write(trackData)
    }

    private fun ByteArrayOutputStream.writeMetaEvent(type: Int, data: ByteArray) {
        writeVlq(0) // Delta time 0
        write(0xFF)
        write(type)
        writeVlq(data.size.toLong())
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

    private fun ByteArrayOutputStream.writeVlq(value: Long) {
        var v = value.coerceAtLeast(0)
        val buffer = mutableListOf<Byte>()
        buffer.add((v and 0x7F).toByte())
        v = v ushr 7
        while (v > 0) {
            buffer.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        for (i in buffer.size - 1 downTo 0) {
            write(buffer[i].toInt() and 0xFF)
        }
    }
}
