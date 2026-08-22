package core.midi.model

import com.example.songdice.data.model.SongArrangement

/**
 * Data structure representing a single MIDI Note event in tick time.
 */
data class MidiNote(
    val pitch: Int,
    val startTick: Long,
    val durationTicks: Long,
    val velocity: Int = 100
)

/**
 * Data structure representing time signature metadata (e.g. 4/4).
 */
data class TimeSignature(
    val numerator: Int = 4,
    val denominator: Int = 4
)

/**
 * Data structure representing a single MIDI Instrument Track.
 */
data class MidiTrack(
    val name: String,
    val channel: Int,
    val notes: List<MidiNote> = emptyList()
)

/**
 * Data structure representing a full MIDI Song/Sequence.
 */
data class MidiSong(
    val tempoBpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature(),
    val ppq: Int = 480,
    val tracks: List<MidiTrack> = emptyList(),
    val title: String = ""
)

/**
 * Extension function to convert a [SongArrangement] model into a [MidiSong].
 */
fun SongArrangement.toMidiSong(ppq: Int = 480): MidiSong {
    val midiTracks = tracks.map { track ->
        val mappedNotes = track.notes.map { note ->
            val startTick = (note.startBeat * ppq).toLong().coerceAtLeast(0)
            val durationTicks = (note.durationBeats * ppq).toLong().coerceAtLeast(1)
            MidiNote(
                pitch = note.pitch.coerceIn(0, 127),
                startTick = startTick,
                durationTicks = durationTicks,
                velocity = note.velocity.coerceIn(1, 127)
            )
        }
        MidiTrack(
            name = track.trackName,
            channel = track.channel.coerceIn(0, 15),
            notes = mappedNotes
        )
    }

    return MidiSong(
        tempoBpm = bpm,
        timeSignature = TimeSignature(4, 4),
        ppq = ppq,
        tracks = midiTracks,
        title = title
    )
}
