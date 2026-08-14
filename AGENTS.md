# Song Dice - Project Agents Guide

## Project Overview
Song Dice is an Android application built using Kotlin and Jetpack Compose. It features a built-in studio-grade high-fidelity synthesizer and multi-track mixer that generates audio directly on the device. The app utilizes Android's `AudioTrack` API to synthesize distinct instruments, including punchy studio drums, analog sub-bass, lush Rhodes/E-piano chords, and an expressive melody lead.

The ultimate goal of the application is to act as a mobile music creation tool, eventually allowing users to export their generated patterns as MIDI files that can be loaded into mobile or desktop Digital Audio Workstations (DAWs) for further production.

## Directory Structure
- `app/`
  - `src/main/java/com/example/`
    - `MainActivity.kt`: The main entry point of the Android application.
    - `songdice/`
      - `audio/`: Contains the core synthesis engine (`AudioSynthPlayer.kt`) which handles real-time audio generation and multitrack playback.
      - `data/`: Contains data models such as `SongArrangement`, `MidiNote`, and tracking structures.
      - `ui/`: Contains the Jetpack Compose UI components for the app's interface.
  - `build.gradle.kts`: The app-level Gradle build configuration.
- `build.gradle.kts`: The project-level Gradle build configuration.
- `settings.gradle.kts`: The project settings.

## Build, Dev, & Test Scripts (Gradle)
Since this is an Android project utilizing Gradle, standard package scripts are replaced with Gradle wrapper commands:
- **Build App (Debug):** `./gradlew assembleDebug`
- **Run Unit Tests:** `./gradlew testDebugUnitTest`
- **Clean Project:** `./gradlew clean`
- **Lint Code:** `./gradlew lint`

## Rules for Android Audio Scheduling & Playback
To maintain high-fidelity audio without dropouts or UI lag:
1. **Background Execution:** All audio synthesis and playback loops must run on a dedicated background thread or coroutine dispatcher (e.g., `Dispatchers.Default`). Never run audio generation on the main UI thread.
2. **Buffer Management:** Always determine the buffer size dynamically using `AudioTrack.getMinBufferSize()` based on the sample rate (e.g., 44100Hz), channel configuration (Stereo), and encoding (16-bit PCM).
3. **Time/Beat Calculation:** Carefully compute the correlation between beats and frames:
   - `secondsPerBeat = 60.0 / bpm`
   - Calculate sample limits based on total beats, sample rate, and `secondsPerBeat`.
4. **Master Limiting:** Always apply transparent soft clipping (e.g., using `tanh(mix * scale)`) on the master mix output to prevent harsh digital clipping before writing to the byte buffer.
5. **Envelopes (ADSR):** Apply Smooth Attack, Decay, Sustain, Release envelopes to avoid popping/clicking artifacts at the start and end of notes.

## Rules for MIDI Export Implementation
When implementing the upcoming MIDI export functionality, adhere to the following rules:
1. **MIDI Channel Standardization:** Adhere to general MIDI conventions. Specifically, always map Drum/Percussion tracks to MIDI Channel 10 (0-indexed Channel 9). Other instruments (Bass, Chords, Melody) should be mapped to distinct channels (e.g., 1, 2, 3).
2. **Timing & Resolution (PPQ):** Calculate and define a standard PPQ (Pulses Per Quarter Note, e.g., 480 or 960) to convert the app's fractional `startBeat` and `durationBeats` into precise MIDI ticks.
3. **Storage Access Framework (SAF):** Use Android's `ACTION_CREATE_DOCUMENT` intent to allow the user to choose where to save the generated `.mid` file on their device. This is crucial for seamless import into other mobile DAWs.
4. **Background Export:** The actual MIDI file generation and saving process should be handled off the main UI thread using Coroutines to avoid ANRs (Application Not Responding errors).
5. **Metadata:** Include relevant MIDI meta-events like Tempo (BPM) and Time Signature at the beginning of the exported file.
