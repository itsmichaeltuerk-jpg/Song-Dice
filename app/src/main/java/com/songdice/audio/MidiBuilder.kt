package com.songdice.audio

import core.midi.builder.MidiBuilder as CoreMidiBuilder

/**
 * Pure Kotlin MidiBuilder wrapper/class for com.songdice.audio package.
 */
class MidiBuilder {
    fun buildMidiFile(song: core.midi.model.MidiSong): ByteArray {
        return CoreMidiBuilder.buildMidiFile(song)
    }

    fun writeVariableLengthQuantity(value: Long, output: java.io.OutputStream) {
        CoreMidiBuilder.writeVariableLengthQuantity(value, output)
    }
}
