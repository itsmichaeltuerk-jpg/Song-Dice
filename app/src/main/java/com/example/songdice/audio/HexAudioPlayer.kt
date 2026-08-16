package com.example.songdice.audio

import android.util.Log

/**
 * HexAudioPlayer is a parallel zero-allocation C++ DSP engine implementation.
 * It uses Google's Oboe library and JNI to bypass the JVM Garbage Collector
 * on the real-time audio thread, guaranteeing zero-latency audio playback.
 *
 * This class is designed to run alongside the old `AudioSynthPlayer` until
 * it fully replaces it.
 */
class HexAudioPlayer {

    companion object {
        private const val TAG = "HexAudioPlayer"

        // Load the C++ JNI library compiled via CMake
        init {
            try {
                System.loadLibrary("hex-audio-jni")
                Log.i(TAG, "Successfully loaded hex-audio-jni library.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load hex-audio-jni library: ${e.message}")
            }
        }
    }

    // --- Native Methods ---
    private external fun initEngineNative(): Boolean
    private external fun startNative(): Boolean
    private external fun stopNative()
    private external fun setBpmNative(bpm: Double)
    private external fun releaseEngineNative()

    // --- Public API ---

    fun initialize(): Boolean {
        Log.d(TAG, "Initializing native HexAudioEngine...")
        return initEngineNative()
    }

    fun play() {
        Log.d(TAG, "Starting native Oboe audio stream...")
        val success = startNative()
        if (!success) {
            Log.e(TAG, "Failed to start native audio stream.")
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping native audio stream...")
        stopNative()
    }

    fun setTempo(bpm: Double) {
        setBpmNative(bpm)
    }

    fun release() {
        Log.d(TAG, "Releasing native HexAudioEngine...")
        releaseEngineNative()
    }
}
