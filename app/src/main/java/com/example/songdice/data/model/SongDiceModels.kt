package com.example.songdice.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Independent Musical Variables ("Dice") driving arrangement generation.
 */
enum class DiceParameter(
    val displayName: String,
    val description: String,
    val iconName: String
) {
    KEY("Musical Key", "Tonal center & scale mode", "Key"),
    PROGRESSION("Chord Progression", "Harmonic framework & movement", "FormatListNumbered"),
    CHORD_RHYTHM("Chord Rhythm", "Harmonic strumming & pulse style", "Speed"),
    BASS_PATTERN("Bass Line Pattern", "Low-end groove & bass articulation", "GraphicEq"),
    DRUM_RHYTHM("Drum Rhythm", "Percussive pattern & backbeat", "Drumstick"),
    MELODY_CONTOUR("Melody Contour", "Lead hook shape & structural trope", "MusicNote")
}

/**
 * Represents the current rolled state and lock status of an individual die.
 */
data class DiceState(
    val parameter: DiceParameter,
    val value: String,
    val isLocked: Boolean = false
)

/**
 * Available Musical Genres for arrangement styling.
 */
enum class MusicalGenre(val displayName: String, val defaultBpm: Int) {
    SYNTHWAVE("Synthwave / Cyberpunk", 115),
    LOFI("Lofi Hip-Hop", 85),
    POP("Modern Pop", 120),
    ROCK("Alternative Rock", 132),
    EDM("EDM / House", 128),
    CINEMATIC("Cinematic Score", 90),
    FUNK("Funk & Soul", 108),
    AFROBEAT("Afrobeat / Tropical", 105)
}

/**
 * Individual MIDI Note event inside an instrument track.
 */
@JsonClass(generateAdapter = true)
data class MidiNote(
    @Json(name = "pitch") val pitch: Int,             // MIDI note number (0-127, e.g. 60 = C4, 36 = Kick)
    @Json(name = "startBeat") val startBeat: Double,  // Start position in quarter-note beats (e.g. 0.0, 0.5, 1.0)
    @Json(name = "durationBeats") val durationBeats: Double, // Duration in beats (e.g. 0.5 = 8th note, 1.0 = quarter)
    @Json(name = "velocity") val velocity: Int = 100   // Velocity (1-127)
)

/**
 * Instrument Track containing isolated musical parts.
 */
@JsonClass(generateAdapter = true)
data class InstrumentTrack(
    @Json(name = "trackName") val trackName: String, // "Drums", "Bass", "Chords", "Melody"
    @Json(name = "channel") val channel: Int,         // MIDI Channel: 9 for Drums (Ch 10), 0 for Bass (Ch 1), 1 for Chords (Ch 2), 2 for Melody (Ch 3)
    @Json(name = "notes") val notes: List<MidiNote>
)

/**
 * Multi-track musical arrangement returned by Gemini API.
 */
@JsonClass(generateAdapter = true)
data class SongArrangement(
    @Json(name = "title") val title: String,
    @Json(name = "genre") val genre: String,
    @Json(name = "key") val key: String,
    @Json(name = "bpm") val bpm: Int,
    @Json(name = "progression") val progression: String,
    @Json(name = "tracks") val tracks: List<InstrumentTrack>
)
