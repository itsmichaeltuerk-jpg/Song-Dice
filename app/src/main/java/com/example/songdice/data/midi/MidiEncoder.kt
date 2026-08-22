package com.example.songdice.data.midi

import com.example.songdice.data.model.SongArrangement
import core.midi.builder.MidiBuilder
import core.midi.model.toMidiSong
import java.io.File
import java.io.FileOutputStream

/**
 * Utility to encode a [SongArrangement] into a standard Type 1 Multi-Track MIDI file (.mid).
 * Strictly complies with Standard MIDI File (SMF 1.0) spec for full DAW compatibility (FL Studio, Ableton, Logic, Pro Tools).
 */
object MidiEncoder {

    const val TICKS_PER_QUARTER_NOTE = 480

    /**
     * Encodes the [arrangement] and saves it as a standard .mid file in the specified output file.
     * Returns the generated File object.
     */
    fun createMidiFile(arrangement: SongArrangement, outputFile: File): File {
        val songBytes = MidiBuilder.buildMidiFile(arrangement.toMidiSong(TICKS_PER_QUARTER_NOTE))
        FileOutputStream(outputFile).use { fos ->
            fos.write(songBytes)
        }
        return outputFile
    }
}
