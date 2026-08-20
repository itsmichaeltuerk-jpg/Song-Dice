package com.example.songdice.audio

import android.util.Log
import com.example.songdice.data.model.SongArrangement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    // --- State ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgressBeats = MutableStateFlow(0f)
    val playbackProgressBeats: StateFlow<Float> = _playbackProgressBeats.asStateFlow()

    private var progressJob: Job? = null

    // --- Native Methods ---
    private external fun initEngineNative(): Boolean
    private external fun startNative(): Boolean
    private external fun stopNative()
    private external fun setBpmNative(bpm: Double)
    private external fun releaseEngineNative()
    private external fun clearTracksNative()
    private external fun addTrackNative(id: String, name: String, type: Int, gain: Float)
    private external fun addStepNative(trackId: String, stepIndex: Int, velocity: Float, pitchOffset: Float)

    // --- Public API ---

    fun initialize(): Boolean {
        Log.d(TAG, "Initializing native HexAudioEngine...")
        return initEngineNative()
    }

    fun play(arrangement: SongArrangement, scope: CoroutineScope) {
        if (!initialize()) return

        // Block: Always stop the active stream before modifying the underlying C++ track lists
        // to prevent concurrent modification segfaults.
        stop()

        Log.d(TAG, "Loading arrangement into native HexAudioEngine...")
        setTempo(arrangement.bpm.toDouble())
        clearTracksNative()

        // Push tracks and steps to native engine
        arrangement.tracks.forEachIndexed { trackIndex, track ->
            val trackId = "track_$trackIndex"

            // Map Kotlin track names to C++ VoiceType Enums
            // 0=Kick, 1=Snare, 2=HiHat, 3=PercHigh, 4=PercLow, 5=Bass, 6=Chord, 7=Melody
            val typeEnum = when {
                track.trackName.contains("Kick", ignoreCase = true) -> 0
                track.trackName.contains("Snare", ignoreCase = true) || track.trackName.contains("Clap", ignoreCase = true) -> 1
                track.trackName.contains("Hat", ignoreCase = true) -> 2
                track.trackName.contains("Perc", ignoreCase = true) -> 3
                track.trackName.contains("Bass", ignoreCase = true) -> 5
                track.trackName.contains("Chord", ignoreCase = true) || track.trackName.contains("Keys", ignoreCase = true) -> 6
                track.trackName.contains("Melody", ignoreCase = true) || track.trackName.contains("Lead", ignoreCase = true) -> 7
                // If it's a generic "Drums" track, map to Kick as a crude fallback for now
                track.trackName.contains("Drum", ignoreCase = true) -> 0
                else -> 4 // PercLow default fallback
            }

            val gain = 1.0f // We don't have individual track volumes on the model yet
            addTrackNative(trackId, track.trackName, typeEnum, gain)

            // Convert MIDI notes to HexSteps
            track.notes.forEach { note ->
                // The C++ sequencer currently assumes 16th notes.
                // startBeat is in quarter notes (0.0, 0.25, 0.5, etc.)
                val stepIndex = (note.startBeat * 4).roundToInt()

                // C++ Engine uses C4 (MIDI 60) as baseline 0 for pitchOffset
                val pitchOffset = (note.pitch - 60).toFloat()
                val velocity = note.velocity / 127f

                addStepNative(trackId, stepIndex, velocity, pitchOffset)
            }
        }

        Log.d(TAG, "Starting native Oboe audio stream...")
        val success = startNative()
        if (success) {
            _isPlaying.value = true
            _playbackProgressBeats.value = 0f

            // Start a mock progress ticker so the UI progress bar still moves
            progressJob?.cancel()
            progressJob = scope.launch {
                // Hardcode loop length for now since HexEngine currently loops 16 steps (4 beats)
                val totalBeats = 4f
                val msPerBeat = (60_000f / arrangement.bpm).toLong()
                val refreshRateMs = 16L // ~60fps

                while (isActive && _isPlaying.value) {
                    delay(refreshRateMs)
                    val addedBeats = refreshRateMs.toFloat() / msPerBeat.toFloat()
                    var current = _playbackProgressBeats.value + addedBeats
                    if (current >= totalBeats) current = 0f // Loop
                    _playbackProgressBeats.value = current
                }
            }
        } else {
            Log.e(TAG, "Failed to start native audio stream.")
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping native audio stream...")
        stopNative()
        _isPlaying.value = false
        progressJob?.cancel()
        _playbackProgressBeats.value = 0f
    }

    fun setTempo(bpm: Double) {
        setBpmNative(bpm)
    }

    fun release() {
        Log.d(TAG, "Releasing native HexAudioEngine...")
        releaseEngineNative()
    }

    // --- Mute / Solo / Volume / FX Stubs ---
    // These methods exist so the ViewModel doesn't crash when using HexAudioPlayer,
    // they will be fully implemented in C++ later.
    fun setTrackMute(trackName: String, isMuted: Boolean) {}
    fun setTrackSolo(trackName: String, isSoloed: Boolean) {}
    fun setTrackVolume(trackName: String, volume: Float) {}
    fun setMasterReverb(volume: Float) {}
    fun setMasterDelay(volume: Float) {}
}
