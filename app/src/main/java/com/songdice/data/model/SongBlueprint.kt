package com.songdice.data.model

import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.MusicalGenre
import com.example.songdice.data.model.SongArrangement

/**
 * Domain representation of a complete Song Blueprint containing metadata, arrangement,
 * locked parameter states, and song structure specifications.
 */
data class SongBlueprint(
    val id: String = "",
    val title: String = "Untitled Song",
    val genre: MusicalGenre = MusicalGenre.POP,
    val bpm: Int = 120,
    val keySignature: String = "C Major",
    val scaleMode: String = "Major",
    val sections: List<SongSection> = emptyList(),
    val rollSettings: RollSettings = RollSettings(),
    val progression: String = "I - V - vi - IV",
    val arrangement: SongArrangement? = null,
    val diceStates: Map<DiceParameter, DiceState> = emptyMap()
)

data class SongSection(
    val sectionType: String,
    val lengthInBeats: Double,
    val energyLevel: Float,
    val chordProgression: List<String> = emptyList()
)

data class RollSettings(
    val lockedParameters: Set<DiceParameter> = emptySet(),
    val activeParameters: Set<DiceParameter> = emptySet(),
    val randomnessFactor: Float = 0.5f,
    val density: Float = 1.0f
)
