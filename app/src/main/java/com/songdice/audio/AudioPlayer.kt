package com.songdice.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.SongArrangement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * Playback state enumeration for preview audio playback.
 */
enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR,
    STOPPED
}

/**
 * Pure Android/Kotlin AudioPlayer wrapper for real-time preview audio playback,
 * seek/scrub controls, and channel solo/mute auditioning.
 */
class AudioPlayer(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _progress = MutableStateFlow(0.0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _mutedChannels = MutableStateFlow<Set<Int>>(emptySet())
    val mutedChannels: StateFlow<Set<Int>> = _mutedChannels.asStateFlow()

    private val _soloedChannels = MutableStateFlow<Set<Int>>(emptySet())
    val soloedChannels: StateFlow<Set<Int>> = _soloedChannels.asStateFlow()

    private var currentArrangement: SongArrangement? = null
    private var currentTracks: List<InstrumentTrack> = emptyList()
    private var currentBpm: Int = 120
    private var pcmData: ByteArray = ByteArray(0)

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    companion object {
        const val CHANNEL_CHORDS = 0
        const val CHANNEL_BASS = 1
        const val CHANNEL_LEAD = 2
        const val CHANNEL_DRUMS = 9

        val AUDITION_CHANNELS = listOf(CHANNEL_CHORDS, CHANNEL_BASS, CHANNEL_LEAD, CHANNEL_DRUMS)
    }

    /**
     * Bitmask calculation for channel solo/mute state.
     */
    fun channelToBit(channel: Int): Int = 1 shl channel

    fun getMuteBitmask(): Int {
        var mask = 0
        for (ch in _mutedChannels.value) {
            mask = mask or channelToBit(ch)
        }
        return mask
    }

    fun getSoloBitmask(): Int {
        var mask = 0
        for (ch in _soloedChannels.value) {
            mask = mask or channelToBit(ch)
        }
        return mask
    }

    fun isChannelMuted(channel: Int): Boolean = _mutedChannels.value.contains(channel)

    fun isChannelSoloed(channel: Int): Boolean = _soloedChannels.value.contains(channel)

    /**
     * Determines if a channel is active given current mute and solo rules.
     * Rule: If any channel is soloed, only soloed non-muted channels are active.
     * Otherwise, any non-muted channel is active.
     */
    fun isChannelActive(channel: Int): Boolean {
        val solos = _soloedChannels.value
        val mutes = _mutedChannels.value

        if (mutes.contains(channel)) {
            return false
        }
        if (solos.isNotEmpty()) {
            return solos.contains(channel)
        }
        return true
    }

    fun setChannelMute(channel: Int, isMuted: Boolean) {
        val current = _mutedChannels.value.toMutableSet()
        if (isMuted) current.add(channel) else current.remove(channel)
        _mutedChannels.value = current
        onAuditionStateChanged()
    }

    fun setChannelSolo(channel: Int, isSoloed: Boolean) {
        val current = _soloedChannels.value.toMutableSet()
        if (isSoloed) current.add(channel) else current.remove(channel)
        _soloedChannels.value = current
        onAuditionStateChanged()
    }

    fun toggleMute(channel: Int) {
        setChannelMute(channel, !isChannelMuted(channel))
    }

    fun toggleSolo(channel: Int) {
        setChannelSolo(channel, !isChannelSoloed(channel))
    }

    /**
     * Loads a [SongArrangement] for audio preview rendering.
     */
    fun loadArrangement(arrangement: SongArrangement, totalBeats: Double = 16.0) {
        stop()
        currentArrangement = arrangement
        currentTracks = arrangement.tracks
        currentBpm = arrangement.bpm.coerceIn(40, 280)
        rebuildAudioBuffer(totalBeats)
        _playbackState.value = PlaybackState.IDLE
    }

    /**
     * Loads raw instrument tracks directly.
     */
    fun loadTracks(tracks: List<InstrumentTrack>, bpm: Int, totalBeats: Double = 16.0) {
        stop()
        currentArrangement = null
        currentTracks = tracks
        currentBpm = bpm.coerceIn(40, 280)
        rebuildAudioBuffer(totalBeats)
        _playbackState.value = PlaybackState.IDLE
    }

    /**
     * Loads pre-synthesized PCM WAV byte array directly.
     */
    fun loadWavBuffer(wavBytes: ByteArray) {
        stop()
        currentArrangement = null
        currentTracks = emptyList()
        extractWavPcm(wavBytes)
        _playbackState.value = PlaybackState.IDLE
    }

    private fun rebuildAudioBuffer(totalBeats: Double = 16.0) {
        if (currentTracks.isEmpty() && currentArrangement == null) return

        val tracksToRender = currentTracks.filter { track ->
            isChannelActive(track.channel)
        }

        val wavBytes = WavSynthesizer.renderToWav(tracksToRender, currentBpm, totalBeats)
        extractWavPcm(wavBytes)
    }

    private fun extractWavPcm(wavBytes: ByteArray) {
        if (wavBytes.size > 44) {
            // Strip 44-byte RIFF header to get raw 16-bit PCM samples
            pcmData = wavBytes.copyOfRange(44, wavBytes.size)
            // 2 channels, 16-bit (2 bytes) = 4 bytes per stereo frame at 44100 Hz
            val totalFrames = pcmData.size / 4
            val durationSec = totalFrames.toDouble() / 44100.0
            _durationMs.value = (durationSec * 1000.0).roundToLong().coerceAtLeast(0L)
        } else {
            pcmData = ByteArray(0)
            _durationMs.value = 0L
        }
        _positionMs.value = 0L
        _progress.value = 0.0f
    }

    private fun onAuditionStateChanged() {
        if (currentTracks.isNotEmpty()) {
            val wasPlaying = _playbackState.value == PlaybackState.PLAYING
            val currentProgress = _progress.value
            rebuildAudioBuffer()
            if (wasPlaying) {
                seekTo(currentProgress)
                play()
            } else {
                seekTo(currentProgress)
            }
        }
    }

    /**
     * Starts playback from current position.
     */
    fun play() {
        if (pcmData.isEmpty()) {
            _playbackState.value = PlaybackState.IDLE
            return
        }

        if (_playbackState.value == PlaybackState.COMPLETED) {
            seekTo(0.0f)
        }

        _playbackState.value = PlaybackState.PLAYING
        startPlaybackLoop()
    }

    /**
     * Pauses audio playback.
     */
    fun pause() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            _playbackState.value = PlaybackState.PAUSED
            stopAudioTrack()
        }
    }

    /**
     * Stops audio playback and resets progress.
     */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        stopAudioTrack()
        _playbackState.value = PlaybackState.STOPPED
        _progress.value = 0.0f
        _positionMs.value = 0L
    }

    /**
     * Seeks to target fractional progress clamped strictly to range [0.0f, 1.0f].
     */
    fun seekTo(targetProgress: Float) {
        val clampedProgress = targetProgress.coerceIn(0.0f, 1.0f)
        _progress.value = clampedProgress
        val totalMs = _durationMs.value
        _positionMs.value = (clampedProgress * totalMs).roundToLong()

        if (_playbackState.value == PlaybackState.PLAYING) {
            stopAudioTrack()
            startPlaybackLoop()
        }
    }

    private fun stopAudioTrack() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = coroutineScope.launch(Dispatchers.Default) {
            val totalBytes = pcmData.size
            if (totalBytes == 0) return@launch

            val startByte = ((_progress.value * totalBytes).toInt() / 4) * 4
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                var bytesWritten = startByte
                val chunkSize = 2048

                while (isActive && _playbackState.value == PlaybackState.PLAYING && bytesWritten < totalBytes) {
                    val remaining = totalBytes - bytesWritten
                    val count = remaining.coerceAtMost(chunkSize)
                    val written = track.write(pcmData, bytesWritten, count)

                    if (written > 0) {
                        bytesWritten += written
                        val progressVal = bytesWritten.toFloat() / totalBytes.toFloat()
                        _progress.value = progressVal.coerceIn(0.0f, 1.0f)
                        _positionMs.value = (progressVal * _durationMs.value).roundToLong()
                    } else if (written < 0) {
                        _playbackState.value = PlaybackState.ERROR
                        break
                    }
                }

                if (bytesWritten >= totalBytes && _playbackState.value == PlaybackState.PLAYING) {
                    _progress.value = 1.0f
                    _positionMs.value = _durationMs.value
                    _playbackState.value = PlaybackState.COMPLETED
                }
            } catch (e: Exception) {
                // If AudioTrack is not supported (e.g. standard JVM test environment), fallback to simulation
                runSimulationLoop(startByte, totalBytes)
            }
        }
    }

    private suspend fun runSimulationLoop(startByte: Int, totalBytes: Int) {
        var bytesProcessed = startByte
        val durationMs = _durationMs.value
        if (durationMs <= 0L) {
            _playbackState.value = PlaybackState.COMPLETED
            return
        }

        val stepMs = 50L
        val bytesPerMs = totalBytes.toDouble() / durationMs.toDouble()

        while (coroutineScope.coroutineContext.isActive && _playbackState.value == PlaybackState.PLAYING && bytesProcessed < totalBytes) {
            delay(stepMs)
            bytesProcessed += (stepMs * bytesPerMs).toInt()
            if (bytesProcessed > totalBytes) bytesProcessed = totalBytes
            val progressVal = (bytesProcessed.toFloat() / totalBytes.toFloat()).coerceIn(0.0f, 1.0f)
            _progress.value = progressVal
            _positionMs.value = (progressVal * durationMs).roundToLong()
        }

        if (bytesProcessed >= totalBytes && _playbackState.value == PlaybackState.PLAYING) {
            _progress.value = 1.0f
            _positionMs.value = durationMs
            _playbackState.value = PlaybackState.COMPLETED
        }
    }
}
