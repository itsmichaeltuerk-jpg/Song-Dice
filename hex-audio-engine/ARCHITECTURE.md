# HexAudioEngine Architecture & Comparison

## Overview

The `HexAudioEngine` is a new, cross-platform DSP (Digital Signal Processing) library designed to eventually replace the audio generation backend of "Song Dice" and power future applications like "HexBeat".

It is written in standard C++ to guarantee deterministic memory management, zero-allocation real-time audio threads, and cross-platform compatibility (Android via JNI/Oboe, Web via WebAssembly, iOS via CoreAudio).

## Comparison: Song Dice (Kotlin) vs. HexAudioEngine (C++)

### 1. The Song Dice Engine (Kotlin)
The current engine in Song Dice relies on Android's native `AudioTrack` API and is written in Kotlin.

**Pros:**
*   Deeply integrated into the Android ecosystem.
*   Easy to maintain alongside UI code.

**Cons (The "Lag" Problem):**
*   **Garbage Collection (GC):** Kotlin runs on the JVM. In an audio context, creating objects (like pairs, arrays, or iterators) on the audio thread triggers the Garbage Collector. When the GC runs, it halts execution briefly. In audio synthesis, a pause of even a few milliseconds causes buffer underruns, resulting in audible clicks, pops, and "laggy" playback.
*   **Platform Lock-in:** The Kotlin implementation is tied to Android (`AudioTrack`). It cannot be easily shared with an iOS app or a Web browser.
*   **Performance Overhead:** JVM bounds checking and thread scheduling are not ideal for strict real-time deadlines.

### 2. The Web Audio Prototype (TypeScript)
The TypeScript prototype demonstrated procedural analog-style synthesis (kick, snare, hi-hat) and a lookahead scheduler using the Web Audio API (`AudioContext`, `OscillatorNode`, `BiquadFilterNode`).

**Pros:**
*   Great proof of concept for procedural drum synthesis.
*   The lookahead scheduler is the correct conceptual model for sequencing.
*   Introduced the core `HexCoordinate` step mapping system.

**Cons:**
*   Tied to the Web Audio API and browser environment.
*   We do not have granular control over the raw DSP buffer generation (the browser handles the actual math).

### 3. The New Approach: HexAudioEngine (C++)
This new engine takes the concepts from the TS prototype (procedural synthesis, hex scheduling) and implements them in pure C++.

**Pros:**
*   **Zero GC / Zero Lag:** We allocate memory strictly during initialization. The real-time audio loop simply iterates over pre-allocated arrays, ensuring the buffer is filled continuously without pauses.
*   **Cross-Platform:** The C++ code knows nothing about Android or Web. It just takes a request for a buffer and fills it. We can wrap this core code with JNI (for Android's Oboe), WebAssembly (for Web Audio Worklets), or AU/VST wrappers for DAWs.
*   **Custom DSP:** Because we are writing the oscillators and filters from scratch, we have total control over the sound.

## Architecture

The engine is structured as a static C++ library:

1.  **HexMath (`HexMath.h`, `HexMath.cpp`):** Handles the unique hexagonal grid coordinate system (cube/axial conversions) used for sequencer step mapping.
2.  **DataTypes (`DataTypes.h`):** Defines the core structures: `HexCoordinate`, `HexStep`, and `TrackConfig`.
3.  **DSP Core (`DSP.h`, `DSP.cpp`):** Since C++ lacks built-in `OscillatorNode` or `BiquadFilterNode`, we implement custom DSP primitives (Sine, Triangle, Noise generators, Envelopes, and basic Filters) to achieve the analog procedural drum sounds defined in the prototype.
4.  **HexEngine (`HexEngine.h`, `HexEngine.cpp`):** The main scheduler and coordinator. It maintains the lookahead time, advances the 16th-note steps, calculates BPM, and triggers the DSP voices based on active hex steps.

## Next Steps for Integration
1.  **DSP Refinement:** Complete the mathematical implementations of the oscillators and filters inside the DSP modules.
2.  **JNI Wrapper:** Create a C++ file that exposes `HexEngine` methods to Kotlin using Java Native Interface (JNI).
3.  **Oboe Integration:** In the Android app, use Google's Oboe library to pull raw floating-point audio buffers from this C++ engine and pass them to the device speaker with ultra-low latency.