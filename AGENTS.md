# Song Dice - Agent Guidelines & System Architecture

## 1. Project Overview & Architecture
Song Dice is a mobile music creation application built for Android using Kotlin, Jetpack Compose, and a native C++20 DSP synthesis engine (`HexAudioEngine`). The application features procedural multi-track arrangement generation, zero-allocation real-time audio playback, and export capabilities for standard MIDI files (SMF Format 1) and 16-bit PCM WAV audio.

Key Architectural Highlights:
- **UI & Presentation:** Jetpack Compose with Material Design 3 (Material You) dynamic theming.
- **Dual Audio Architecture:** Real-time C++20 `HexAudioEngine` via Google Oboe/JNI alongside pure Kotlin `MidiBuilder` and `WavSynthesizer` export drivers.
- **Harmonic Mapping:** Hexagonal grid coordinate system $(q, r, s)$ for spatial step sequencing and harmonic relationships.

---

## 2. Canonical Package Directory Hierarchy
The codebase is structured under `app/src/main/java/com/songdice/` and `app/src/main/cpp/`:

```
com.songdice/
├── audio/            # Real-time audio interfaces, playback managers, and JNI bindings (HexAudioPlayer)
├── data/             # Core domain models, state management, persistence repositories, and API interfaces
├── engine/           # Algorithmic song generation, dice rolling mechanics, and procedural sequence generators
├── export/           # Pure Kotlin MIDI SMF Type 1 exporter (MidiBuilder) and PCM renderer (WavSynthesizer)
├── ui/               # Jetpack Compose UI components, Material 3 screens, view models, and themes
└── billing/          # Google Play In-App Billing, subscriptions, and feature licensing

app/src/main/cpp/     # Native C++20 audio engine core, Google Oboe driver, JNI bridge (jni_bridge.cpp), CMakeLists.txt
```

---

## 3. UI & Styling Guidelines (Material Design 3 / Material You)
All user interface elements must adhere to Material Design 3 (Material You) guidelines. Legacy neon, cyberpunk, or custom hardcoded color schemes are deprecated.

Key UI Requirements:
- **Dynamic Theming:** Support Material You dynamic color palettes on Android 12+ (API 31+) using `dynamicDarkColorScheme()` and `dynamicLightColorScheme()`.
- **Fallback Theme:** Fall back gracefully to `darkColorScheme()` using standard Material Design 3 color roles (`primary`, `surface`, `surfaceVariant`, `outline`).
- **Navigation:** Modern flat UI structured with Material 3 `NavigationBar` and `TopAppBar`.
- **Components:** Use standard Material 3 components (`Card`, `Button`, `IconButton`, `Slider`, `Text`) with standardized M3 typography tokens.

---

## 4. Dual Audio Architecture
Song Dice utilizes a hybrid dual-engine architecture for audio generation and export:

### Pure Kotlin Subsystem
- **`MidiBuilder` (SMF Type 1):** Standalone Kotlin implementation for constructing Standard MIDI Files (Type 1 multi-track). Generates header chunks, track chunks, delta times, tempo/key meta-events, and channel message events (Note On, Note Off, Control Change).
- **`WavSynthesizer`:** Pure Kotlin offline renderer that synthesizes 16-bit PCM stereo WAV files directly from `SongArrangement` models for offline export and software playback without JVM real-time GC overhead constraints.

### Native C++20 `HexAudioEngine` Subsystem
- **Real-time DSP Core:** Written in standard C++20 to guarantee zero-allocation real-time audio thread execution. Eliminates JVM Garbage Collector pauses, buffer underruns, and audio clicks/pops.
- **Google Oboe Driver & JNI Bridge:** Connects to Android audio via Google Oboe (`app/src/main/cpp/jni_bridge.cpp` and `HexAudioPlayer.kt`).
- **Hexagonal Cube Coordinates ($q, r, s$):** Uses 3D cube coordinates satisfying $q + r + s = 0$ to map hexagonal grid sequencer steps, harmonic intervals, and spatial voice panning.
- **Zero-Allocation Execution:** Pre-allocated voice object pools, lock-free ring buffers, and fast LCG (Linear Congruential Generator) noise generators.

---

## 5. Reference Data Model Schemas
Core data classes driving arrangement generation, sequence data, and audio export:

### `SongBlueprint`
```kotlin
data class SongBlueprint(
    val id: String,
    val title: String,
    val genre: MusicalGenre,
    val bpm: Int,
    val keySignature: String,
    val scaleMode: String,
    val sections: List<SongSection>,
    val rollSettings: RollSettings
)
```

### `SongSection`
```kotlin
data class SongSection(
    val sectionType: String,      // e.g., "Intro", "Verse", "Chorus", "Bridge", "Outro"
    val lengthInBeats: Double,    // e.g., 16.0, 32.0
    val energyLevel: Float,       // 0.0f to 1.0f
    val chordProgression: List<ChordVoicing>,
    val tracks: List<InstrumentTrack>
)
```

### `MidiNoteEvent`
```kotlin
data class MidiNoteEvent(
    val pitch: Int,               // MIDI note number (0-127, e.g., 60 = C4)
    val startBeat: Double,        // Absolute start position in quarter-note beats
    val durationBeats: Double,   // Duration in beats (e.g., 0.5 = 8th note, 1.0 = quarter note)
    val velocity: Int = 100,      // Velocity (1-127)
    val channel: Int = 0,         // MIDI Channel (0-15; Channel 9 reserved for drums)
    val pan: Double = 0.5         // Stereo panning from 0.0 (Left) to 1.0 (Right)
)
```

### `ChordVoicing`
```kotlin
data class ChordVoicing(
    val rootNote: Int,            // Base MIDI pitch of root note
    val pitchClass: String,       // e.g., "Maj7", "Min9", "7dom"
    val inversion: Int = 0,       // Inversion index (0 = Root, 1 = 1st, 2 = 2nd)
    val intervals: List<Int>,     // Pitch intervals relative to root
    val voicePitches: List<Int>   // Resolved absolute MIDI pitches
)
```

### `RollSettings`
```kotlin
data class RollSettings(
    val lockedParameters: Set<DiceParameter>,
    val activeParameters: Set<DiceParameter>,
    val randomnessFactor: Float,  // 0.0f (strict pattern) to 1.0f (wild generation)
    val density: Float            // Note event density multiplier
)
```

---

## 6. Build, Development & JVM Unit Testing Standards
Standard Gradle commands for building, linting, and executing unit tests:

- **Build Debug APK:** `./gradlew assembleDebug`
- **Run JVM Unit Tests:** `./gradlew test` (or `./gradlew testDebugUnitTest`)
- **Run Linting & Verification:** `./gradlew check` / `./gradlew lint`
- **Native C++ Standalone Build & Test:** `mkdir -p build && cd build && cmake ../hex-audio-engine && make && ./hex-engine-test`

### Unit Testing Guidelines
- All domain logic (`MidiBuilder`, `WavSynthesizer`, engine generators, data schemas, and repositories) must be covered by JVM unit tests in `app/src/test/`.
- Every code change must be verified by executing `./gradlew test` to ensure 100% pass rate before submitting.

---

## 7. Rules for Real-Time DSP & Audio Scheduling
1. **Background Execution:** All audio synthesis, processing, and playback loops must run on a dedicated dispatcher (`Dispatchers.Default`) or native C++ thread. Never invoke audio synthesis on the UI thread.
2. **Zero Allocation in Hotpath:** The real-time audio callback thread must never allocate objects on the JVM or C++ heap to eliminate Garbage Collection pauses and thread priority inversions.
3. **Master Soft Limiting:** Always apply transparent master soft clipping (`tanh(mix * scale)`) to prevent digital clipping / distortion.
4. **Envelope Smoothing:** Apply ADSR (Attack, Decay, Sustain, Release) envelopes to prevent popping and clicking artifacts at note boundaries.

---

## 8. Rules for MIDI Export & Storage Implementation
1. **Standard MIDI Channel Mapping:** Channel 10 (0-indexed Channel 9) is strictly reserved for Drum/Percussion tracks. Melodic and harmonic instruments (Bass, Chords, Lead) must be assigned to channels 1-8.
2. **PPQ Resolution:** Standard Pulses Per Quarter Note (PPQ = 480) resolution must be used for converting fractional beat timestamps (`startBeat`, `durationBeats`) into precise MIDI tick counts.
3. **Storage Access Framework (SAF):** File saving must use Android's `ACTION_CREATE_DOCUMENT` intent SAF flow to allow safe destination selection.
4. **Off-Thread File Rendering:** File encoding and I/O operations must run off the main UI thread using Coroutines or background threads.
