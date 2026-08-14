package com.example.songdice.data.repository

import com.example.BuildConfig
import com.example.songdice.data.model.DiceParameter
import com.example.songdice.data.model.DiceState
import com.example.songdice.data.model.MusicalGenre
import com.example.songdice.data.model.SongArrangement
import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.network.GeminiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SongDiceRepository {

    /**
     * Preset library of dice values for instant offline rolls or UI randomization.
     */
    val dicePresets: Map<DiceParameter, List<String>> = mapOf(
        DiceParameter.KEY to listOf("C Major", "A Minor", "F# Minor", "Eb Major", "G Major", "D Minor", "E Minor", "Bb Major", "C# Minor", "Ab Major"),
        DiceParameter.PROGRESSION to listOf("i - VI - III - VII", "I - V - vi - IV", "ii - V - I - vi", "i - iv - v - i", "I - vi - IV - V", "vi - IV - I - V", "i - VII - VI - V7"),
        DiceParameter.CHORD_RHYTHM to listOf("Sustained Whole Notes", "Offbeat Ska/Reggae Chop", "Syncopated 8th Push", "Four-on-the-Floor Pulse", "Arpeggiated 16ths", "Triplot Swing Strum"),
        DiceParameter.BASS_PATTERN to listOf("Driving Root 8ths", "Walking Octave Bass", "Funky Slap & Pop", "Sub-Bass Long Tones", "Arpeggiated Root-5th Line", "Syncopated Disco Bounce"),
        DiceParameter.DRUM_RHYTHM to listOf("Boom Bap 90s Groove", "Four-on-the-Floor House", "Trap Hi-Hat Rolls & 808", "Syncopated Afrobeat", "Classic Rock 2 & 4 Backbeat", "Lofi Dusty Half-Time"),
        DiceParameter.MELODY_CONTOUR to listOf("Rising Peak - High Energy", "Call & Response Lead", "Melancholic Pentatonic Hook", "Syncopated Arpeggio Motif", "Cinematic Crescendo Line", "Smooth Legato Glide")
    )

    /**
     * Rolls random values for unlocked dice, influenced by an optional user style prompt.
     */
    fun rollDice(
        currentDice: Map<DiceParameter, DiceState>,
        stylePrompt: String = ""
    ): Map<DiceParameter, DiceState> {
        return currentDice.mapValues { (param, state) ->
            if (state.isLocked) {
                state
            } else {
                val options = dicePresets[param] ?: listOf("Default")
                val selectedValue = if (stylePrompt.isNotBlank()) {
                    selectPresetForStyle(param, options, stylePrompt)
                } else {
                    options.random()
                }
                state.copy(value = selectedValue)
            }
        }
    }

    private fun selectPresetForStyle(
        param: DiceParameter,
        options: List<String>,
        prompt: String
    ): String {
        val lowerPrompt = prompt.lowercase()
        val matched = options.filter { option ->
            val optLower = option.lowercase()
            when {
                lowerPrompt.contains("dark") || lowerPrompt.contains("minor") || lowerPrompt.contains("sad") ->
                    optLower.contains("minor") || optLower.contains("sub") || optLower.contains("melancholic") || optLower.contains("dusty")
                lowerPrompt.contains("upbeat") || lowerPrompt.contains("dance") || lowerPrompt.contains("funk") || lowerPrompt.contains("happy") ->
                    optLower.contains("major") || optLower.contains("four-on-the-floor") || optLower.contains("disco") || optLower.contains("slap")
                lowerPrompt.contains("lofi") || lowerPrompt.contains("chill") || lowerPrompt.contains("relax") ->
                    optLower.contains("lofi") || optLower.contains("sustained") || optLower.contains("smooth")
                lowerPrompt.contains("rock") || lowerPrompt.contains("metal") || lowerPrompt.contains("heavy") ->
                    optLower.contains("rock") || optLower.contains("driving") || optLower.contains("peak")
                else -> optLower.split(" ").any { word -> word.length > 3 && lowerPrompt.contains(word) }
            }
        }
        return matched.randomOrNull() ?: options.random()
    }

    /**
     * Context-aware API call to generate a multi-track musical arrangement using Gemini.
     */
    suspend fun generateArrangement(
        genre: MusicalGenre,
        bpm: Int,
        diceStates: Map<DiceParameter, DiceState>,
        userStylePrompt: String = "",
        previousArrangement: SongArrangement?
    ): SongArrangement = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val lockedParams = diceStates.filter { it.value.isLocked }
        val unlockedParams = diceStates.filter { !it.value.isLocked }

        val prompt = buildContextAwarePrompt(genre, bpm, diceStates, lockedParams, unlockedParams, userStylePrompt, previousArrangement)

        try {
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                GeminiApiService.generateArrangement(apiKey, prompt)
            } else {
                generateProceduralArrangement(genre, bpm, diceStates)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateProceduralArrangement(genre, bpm, diceStates)
        }
    }

    private fun buildContextAwarePrompt(
        genre: MusicalGenre,
        bpm: Int,
        diceStates: Map<DiceParameter, DiceState>,
        lockedParams: Map<DiceParameter, DiceState>,
        unlockedParams: Map<DiceParameter, DiceState>,
        userStylePrompt: String,
        previousArrangement: SongArrangement?
    ): String {
        val sb = StringBuilder()
        sb.append("Generate a 4-bar (16 beats) high quality multi-track song arrangement for:\n")
        sb.append("- Genre: ${genre.displayName}\n")
        sb.append("- Tempo: $bpm BPM\n")
        if (userStylePrompt.isNotBlank()) {
            sb.append("- User Style / Vibe Preference: \"$userStylePrompt\"\n")
        }
        sb.append("- Dice Variables:\n")

        diceStates.forEach { (param, state) ->
            val lockLabel = if (state.isLocked) "[LOCKED]" else "[NEWLY ROLLED]"
            sb.append("  * ${param.displayName}: ${state.value} $lockLabel\n")
        }

        if (userStylePrompt.isNotBlank()) {
            sb.append("\nSTYLE PREFERENCE DIRECTIVE:\n")
            sb.append("Infuse the overall sonic aesthetic, rhythm complexity, chord voicing, and melodic contour with the style/vibe: \"$userStylePrompt\".\n")
        }

        if (lockedParams.isNotEmpty()) {
            sb.append("\nCONTEXT-AWARE ADAPTATION DIRECTIVE:\n")
            sb.append("The user has locked the following parameters: ")
            sb.append(lockedParams.entries.joinToString { "${it.key.displayName}=${it.value.value}" })
            sb.append(".\n")

            if (previousArrangement != null) {
                sb.append("Keep the musical foundation of these locked parameters intact. Adapt the newly rolled elements (")
                sb.append(unlockedParams.entries.joinToString { "${it.key.displayName}=${it.value.value}" })
                sb.append(") so they harmonically match, complement, and enhance the locked foundation.\n")
            }
        }

        sb.append("""
            Return a JSON object adhering STRICTLY to this JSON structure:
            {
              "title": "Song Title",
              "genre": "${genre.displayName}",
              "key": "${diceStates[DiceParameter.KEY]?.value ?: "C Major"}",
              "bpm": $bpm,
              "progression": "${diceStates[DiceParameter.PROGRESSION]?.value ?: "I - V - vi - IV"}",
              "tracks": [
                {
                  "trackName": "Drums",
                  "channel": 9,
                  "notes": [
                    {"pitch": 36, "startBeat": 0.0, "durationBeats": 0.5, "velocity": 110},
                    {"pitch": 38, "startBeat": 1.0, "durationBeats": 0.5, "velocity": 100}
                  ]
                },
                {
                  "trackName": "Bass",
                  "channel": 0,
                  "notes": [
                    {"pitch": 36, "startBeat": 0.0, "durationBeats": 1.0, "velocity": 105}
                  ]
                },
                {
                  "trackName": "Chords",
                  "channel": 1,
                  "notes": [
                    {"pitch": 60, "startBeat": 0.0, "durationBeats": 2.0, "velocity": 90},
                    {"pitch": 64, "startBeat": 0.0, "durationBeats": 2.0, "velocity": 90},
                    {"pitch": 67, "startBeat": 0.0, "durationBeats": 2.0, "velocity": 90}
                  ]
                },
                {
                  "trackName": "Melody",
                  "channel": 2,
                  "notes": [
                    {"pitch": 72, "startBeat": 0.0, "durationBeats": 1.0, "velocity": 100}
                  ]
                }
              ]
            }
        """.trimIndent())

        return sb.toString()
    }

    /**
     * Procedural local MIDI arrangement generator:
     * Constructs rich, harmonically pristine 4-part arrangements (Chords, Bass, Drums, Melody).
     */
    fun generateProceduralArrangement(
        genre: MusicalGenre,
        bpm: Int,
        diceStates: Map<DiceParameter, DiceState>
    ): SongArrangement {
        val keyStr = diceStates[DiceParameter.KEY]?.value ?: "C Major"
        val progressionStr = diceStates[DiceParameter.PROGRESSION]?.value ?: "i - VI - III - VII"
        val isMinor = keyStr.contains("Minor", ignoreCase = true) || keyStr.contains("m", ignoreCase = true)

        val rootPitch = when {
            keyStr.contains("C#") || keyStr.contains("Db") -> 37
            keyStr.contains("D#") || keyStr.contains("Eb") -> 39
            keyStr.contains("F#") || keyStr.contains("Gb") -> 42
            keyStr.contains("G#") || keyStr.contains("Ab") -> 44
            keyStr.contains("A#") || keyStr.contains("Bb") -> 46
            keyStr.contains("C") -> 36
            keyStr.contains("D") -> 38
            keyStr.contains("E") -> 40
            keyStr.contains("F") -> 41
            keyStr.contains("G") -> 43
            keyStr.contains("A") -> 45
            keyStr.contains("B") -> 47
            else -> 36
        }

        // Exact Diatonic Scale Intervals & Chord Formulas
        // Each entry in chordDefs: (Root semitone offset from key root, list of semitone offsets for triad/7th)
        val chordDefs: List<Pair<Int, List<Int>>> = if (isMinor) {
            when {
                progressionStr.contains("i - iv - v - i") -> listOf(
                    0 to listOf(0, 3, 7),     // i (Am)
                    5 to listOf(5, 8, 12),    // iv (Dm)
                    7 to listOf(7, 10, 14),   // v (Em)
                    0 to listOf(0, 3, 7)      // i (Am)
                )
                progressionStr.contains("i - VII - VI - V") -> listOf(
                    0 to listOf(0, 3, 7),     // i
                    10 to listOf(10, 14, 17), // VII
                    8 to listOf(8, 12, 15),   // VI
                    7 to listOf(7, 11, 14)    // V
                )
                else -> listOf(
                    0 to listOf(0, 3, 7, 10), // i (Am7)
                    8 to listOf(8, 12, 15),   // VI (F)
                    3 to listOf(3, 7, 10),    // III (C)
                    10 to listOf(10, 14, 17)  // VII (G)
                )
            }
        } else {
            when {
                progressionStr.contains("vi - IV - I - V") -> listOf(
                    9 to listOf(9, 12, 16),   // vi (Am)
                    5 to listOf(5, 9, 12),    // IV (F)
                    0 to listOf(0, 4, 7),     // I (C)
                    7 to listOf(7, 11, 14)    // V (G)
                )
                progressionStr.contains("I - V - vi - IV") -> listOf(
                    0 to listOf(0, 4, 7),     // I (C)
                    7 to listOf(7, 11, 14),   // V (G)
                    9 to listOf(9, 12, 16),   // vi (Am)
                    5 to listOf(5, 9, 12)     // IV (F)
                )
                progressionStr.contains("ii - V - I - vi") -> listOf(
                    2 to listOf(2, 5, 9),     // ii (Dm)
                    7 to listOf(7, 11, 14),   // V (G)
                    0 to listOf(0, 4, 7),     // I (C)
                    9 to listOf(9, 12, 16)    // vi (Am)
                )
                else -> listOf(
                    0 to listOf(0, 4, 7, 11), // I (Cmaj7)
                    9 to listOf(9, 12, 16),   // vi (Am)
                    5 to listOf(5, 9, 12),    // IV (F)
                    7 to listOf(7, 11, 14)    // V (G)
                )
            }
        }

        // 1. Drums Track (Kick=36, Snare=38, Hi-Hat=42/46)
        val drumNotes = mutableListOf<MidiNote>()
        for (beat in 0 until 16) {
            val b = beat.toDouble()
            // Kick on 1 and 3 of every bar
            if (beat % 4 == 0 || (beat % 4 == 2 && beat % 8 == 2)) {
                drumNotes.add(MidiNote(pitch = 36, startBeat = b, durationBeats = 0.5, velocity = 110))
            }
            // Snare on 2 and 4
            if (beat % 4 == 2) {
                drumNotes.add(MidiNote(pitch = 38, startBeat = b, durationBeats = 0.5, velocity = 100))
            }
            // Crisp Hi-Hats on 8th notes
            drumNotes.add(MidiNote(pitch = 42, startBeat = b, durationBeats = 0.25, velocity = 85))
            drumNotes.add(MidiNote(pitch = 42, startBeat = b + 0.5, durationBeats = 0.25, velocity = 70))
        }

        // 2. Bass Track: Musical bass patterns mapped directly to the BASS_PATTERN die
        val bassPatternStr = diceStates[DiceParameter.BASS_PATTERN]?.value ?: "Driving Root 8ths"
        val bassNotes = mutableListOf<MidiNote>()

        fun normalizeBassPitch(pitch: Int): Int {
            var p = pitch
            while (p < 33) p += 12 // Keep in solid bass register (A0 = 55Hz)
            while (p > 48) p -= 12 // Keep below C3 (130Hz)
            return p
        }

        for (bar in 0 until 4) {
            val barStart = (bar * 4).toDouble()
            val (chordRootOffset, _) = chordDefs[bar % chordDefs.size]
            val root = normalizeBassPitch(rootPitch + chordRootOffset)
            val fifth = normalizeBassPitch(root + 7)
            val octave = root + 12

            when {
                bassPatternStr.contains("Driving", ignoreCase = true) -> {
                    // Steady, driving 8th notes with dynamic velocity grooves
                    for (i in 0 until 8) {
                        val beatPos = barStart + (i * 0.5)
                        val isDownbeat = (i % 2 == 0)
                        val vel = if (isDownbeat) 112 else 88
                        bassNotes.add(MidiNote(pitch = root, startBeat = beatPos, durationBeats = 0.45, velocity = vel))
                    }
                }
                bassPatternStr.contains("Walking", ignoreCase = true) -> {
                    // Walking bassline moving smoothly between chord tones
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart, durationBeats = 0.9, velocity = 110))
                    bassNotes.add(MidiNote(pitch = root + 4, startBeat = barStart + 1.0, durationBeats = 0.9, velocity = 95))
                    bassNotes.add(MidiNote(pitch = fifth, startBeat = barStart + 2.0, durationBeats = 0.9, velocity = 105))
                    bassNotes.add(MidiNote(pitch = root + 2, startBeat = barStart + 3.0, durationBeats = 0.9, velocity = 90))
                }
                bassPatternStr.contains("Funky", ignoreCase = true) || bassPatternStr.contains("Slap", ignoreCase = true) -> {
                    // Punchy slap bass with syncopated octave pops
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart, durationBeats = 0.75, velocity = 118))
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart + 1.5, durationBeats = 0.35, velocity = 95))
                    bassNotes.add(MidiNote(pitch = octave, startBeat = barStart + 2.25, durationBeats = 0.45, velocity = 120))
                    bassNotes.add(MidiNote(pitch = fifth, startBeat = barStart + 3.0, durationBeats = 0.45, velocity = 100))
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart + 3.5, durationBeats = 0.45, velocity = 105))
                }
                bassPatternStr.contains("Sub-Bass", ignoreCase = true) || bassPatternStr.contains("Long", ignoreCase = true) -> {
                    // Deep, sustained sub-bass foundation
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart, durationBeats = 3.8, velocity = 115))
                }
                bassPatternStr.contains("Arpeggiated", ignoreCase = true) -> {
                    // Root - 5th - Octave - 5th arpeggio groove
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart, durationBeats = 0.85, velocity = 110))
                    bassNotes.add(MidiNote(pitch = fifth, startBeat = barStart + 1.0, durationBeats = 0.85, velocity = 98))
                    bassNotes.add(MidiNote(pitch = octave, startBeat = barStart + 2.0, durationBeats = 0.85, velocity = 105))
                    bassNotes.add(MidiNote(pitch = fifth, startBeat = barStart + 3.0, durationBeats = 0.85, velocity = 98))
                }
                bassPatternStr.contains("Disco", ignoreCase = true) -> {
                    // Classic pulsating disco octave bounce
                    for (i in 0 until 4) {
                        val beatPos = barStart + i.toDouble()
                        bassNotes.add(MidiNote(pitch = root, startBeat = beatPos, durationBeats = 0.45, velocity = 110))
                        bassNotes.add(MidiNote(pitch = octave, startBeat = beatPos + 0.5, durationBeats = 0.45, velocity = 95))
                    }
                }
                else -> {
                    // Punchy syncopated modern bass groove
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart, durationBeats = 1.4, velocity = 112))
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart + 1.75, durationBeats = 0.45, velocity = 95))
                    bassNotes.add(MidiNote(pitch = fifth, startBeat = barStart + 2.5, durationBeats = 0.75, velocity = 102))
                    bassNotes.add(MidiNote(pitch = root, startBeat = barStart + 3.5, durationBeats = 0.45, velocity = 98))
                }
            }
        }

        // 3. Chords Track: Lush warm E-Piano chord voicings in mid register (C4-B4)
        val chordNotes = mutableListOf<MidiNote>()
        for (bar in 0 until 4) {
            val barStart = (bar * 4).toDouble()
            val (_, chordIntervals) = chordDefs[bar % chordDefs.size]

            for (interval in chordIntervals) {
                val notePitch = rootPitch + 24 + interval
                // Strum beat 0
                chordNotes.add(MidiNote(pitch = notePitch, startBeat = barStart, durationBeats = 1.9, velocity = 88))
                // Syncopated pulse on beat 2
                chordNotes.add(MidiNote(pitch = notePitch, startBeat = barStart + 2.0, durationBeats = 1.8, velocity = 82))
            }
        }

        // 4. Melody Track: Harmonically aligned melodic motif that follows chord tones
        val melodyNotes = mutableListOf<MidiNote>()
        for (bar in 0 until 4) {
            val barStart = (bar * 4).toDouble()
            val (_, chordIntervals) = chordDefs[bar % chordDefs.size]

            val note1 = rootPitch + 36 + chordIntervals[0] // Root
            val note2 = rootPitch + 36 + chordIntervals[1 % chordIntervals.size] // 3rd
            val note3 = rootPitch + 36 + chordIntervals[2 % chordIntervals.size] // 5th

            melodyNotes.add(MidiNote(pitch = note1, startBeat = barStart, durationBeats = 0.8, velocity = 100))
            melodyNotes.add(MidiNote(pitch = note2, startBeat = barStart + 1.0, durationBeats = 0.8, velocity = 105))
            melodyNotes.add(MidiNote(pitch = note3, startBeat = barStart + 2.0, durationBeats = 1.6, velocity = 110))
        }

        return SongArrangement(
            title = "${genre.displayName} • ${keyStr}",
            genre = genre.displayName,
            key = keyStr,
            bpm = bpm,
            progression = progressionStr,
            tracks = listOf(
                InstrumentTrack(trackName = "Drums", channel = 9, notes = drumNotes),
                InstrumentTrack(trackName = "Bass", channel = 0, notes = bassNotes),
                InstrumentTrack(trackName = "Chords", channel = 1, notes = chordNotes),
                InstrumentTrack(trackName = "Melody", channel = 2, notes = melodyNotes)
            )
        )
    }
}
