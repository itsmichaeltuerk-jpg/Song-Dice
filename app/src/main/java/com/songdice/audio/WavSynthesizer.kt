package com.songdice.audio

import com.example.songdice.data.model.InstrumentTrack
import com.example.songdice.data.model.MidiNote
import com.example.songdice.data.model.SongArrangement
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Pure Kotlin 16-bit PCM WAV Synthesizer and AudioPreviewRenderer.
 *
 * Synthesizes a 16-bit PCM stereo WAV audio buffer (44.1 kHz) from generated song tracks
 * (Chords, Bass, Lead, Drums) for preview playback and export bundling.
 */
object WavSynthesizer {

    const val SAMPLE_RATE = 44100
    const val NUM_CHANNELS = 2
    const val BITS_PER_SAMPLE = 16
    const val HEADROOM_SCALE = 0.8

    enum class VoiceType { CHORDS, BASS, LEAD, DRUMS }

    /**
     * Synthesizes a 16-bit PCM stereo WAV buffer from a [SongArrangement].
     */
    fun renderToWav(
        arrangement: SongArrangement,
        totalBeats: Double = 16.0
    ): ByteArray {
        val bpm = arrangement.bpm.coerceIn(40, 280)
        return renderToWav(arrangement.tracks, bpm, totalBeats)
    }

    /**
     * Synthesizes a 16-bit PCM stereo WAV buffer from a list of [InstrumentTrack]s.
     */
    fun renderToWav(
        tracks: List<InstrumentTrack>,
        bpm: Int,
        totalBeats: Double = 16.0
    ): ByteArray {
        val secondsPerBeat = 60.0 / bpm.toDouble().coerceIn(40.0, 280.0)

        var maxBeat = totalBeats
        for (track in tracks) {
            for (note in track.notes) {
                val end = note.startBeat + note.durationBeats + 0.5 // allow release tail
                if (end > maxBeat) {
                    maxBeat = end
                }
            }
        }

        val totalSamples = (maxBeat * secondsPerBeat * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val dataSize = totalSamples * NUM_CHANNELS * (BITS_PER_SAMPLE / 8)
        val totalFileSize = 44 + dataSize

        val leftBuffer = DoubleArray(totalSamples)
        val rightBuffer = DoubleArray(totalSamples)

        for (track in tracks) {
            val voiceType = determineVoiceType(track.channel, track.trackName)
            for (note in track.notes) {
                renderNoteToBuffers(
                    note = note,
                    voiceType = voiceType,
                    secondsPerBeat = secondsPerBeat,
                    totalSamples = totalSamples,
                    leftBuffer = leftBuffer,
                    rightBuffer = rightBuffer
                )
            }
        }

        val wavBytes = ByteArray(totalFileSize)
        val header = createWavHeader(dataSize)
        System.arraycopy(header, 0, wavBytes, 0, 44)

        var byteIdx = 44
        val maxShort = 32767.0
        for (i in 0 until totalSamples) {
            val sampleL = leftBuffer[i]
            val sampleR = rightBuffer[i]

            // Apply master bus headroom scaling (0.8 max amplitude) and soft clipping
            val masteredL = tanh(sampleL) * HEADROOM_SCALE
            val masteredR = tanh(sampleR) * HEADROOM_SCALE

            val shortL = (masteredL * maxShort).toInt().coerceIn(-32768, 32767).toShort()
            val shortR = (masteredR * maxShort).toInt().coerceIn(-32768, 32767).toShort()

            // 16-bit Little Endian stereo PCM
            wavBytes[byteIdx++] = (shortL.toInt() and 0xFF).toByte()
            wavBytes[byteIdx++] = ((shortL.toInt() shr 8) and 0xFF).toByte()

            wavBytes[byteIdx++] = (shortR.toInt() and 0xFF).toByte()
            wavBytes[byteIdx++] = ((shortR.toInt() shr 8) and 0xFF).toByte()
        }

        return wavBytes
    }

    /**
     * Generates standard 44-byte RIFF/WAVE header fields:
     * PCM format 1, 2 channels, 44100 Hz sample rate, 16-bit depth.
     */
    fun createWavHeader(dataSize: Int): ByteArray {
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF Chunk
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(36 + dataSize)
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt Chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // PCM Subchunk1Size
        buffer.putShort(1.toShort()) // AudioFormat PCM = 1
        buffer.putShort(NUM_CHANNELS.toShort()) // 2 Channels
        buffer.putInt(SAMPLE_RATE) // 44100 Hz
        val byteRate = SAMPLE_RATE * NUM_CHANNELS * (BITS_PER_SAMPLE / 8)
        buffer.putInt(byteRate) // ByteRate
        val blockAlign = (NUM_CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()
        buffer.putShort(blockAlign) // BlockAlign = 4
        buffer.putShort(BITS_PER_SAMPLE.toShort()) // BitsPerSample = 16

        // data Chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(dataSize)

        return buffer.array()
    }

    private fun determineVoiceType(
        channel: Int,
        trackName: String
    ): VoiceType {
        return when {
            channel == 0 -> VoiceType.CHORDS
            channel == 1 -> VoiceType.BASS
            channel == 2 -> VoiceType.LEAD
            channel == 9 -> VoiceType.DRUMS
            trackName.contains("Chord", ignoreCase = true) || trackName.contains("Key", ignoreCase = true) -> VoiceType.CHORDS
            trackName.contains("Bass", ignoreCase = true) -> VoiceType.BASS
            trackName.contains("Lead", ignoreCase = true) || trackName.contains("Melody", ignoreCase = true) -> VoiceType.LEAD
            trackName.contains("Drum", ignoreCase = true) || trackName.contains("Perc", ignoreCase = true) -> VoiceType.DRUMS
            else -> VoiceType.CHORDS
        }
    }

    private fun renderNoteToBuffers(
        note: MidiNote,
        voiceType: VoiceType,
        secondsPerBeat: Double,
        totalSamples: Int,
        leftBuffer: DoubleArray,
        rightBuffer: DoubleArray
    ) {
        val noteStartSec = note.startBeat * secondsPerBeat
        val noteDurSec = note.durationBeats * secondsPerBeat

        val startFrame = (noteStartSec * SAMPLE_RATE).toInt().coerceIn(0, totalSamples)
        val endFrame = ((noteStartSec + noteDurSec + 0.5) * SAMPLE_RATE).toInt().coerceIn(0, totalSamples)

        if (startFrame >= totalSamples || startFrame >= endFrame) return

        val velGain = (note.velocity / 127.0).coerceIn(0.1, 1.0)
        val pan = note.pan.coerceIn(0.0, 1.0)
        val leftGain = kotlin.math.cos(pan * PI / 2.0)
        val rightGain = kotlin.math.sin(pan * PI / 2.0)

        val freq = midiPitchToFreq(note.pitch)

        val random = Random(note.pitch * 31 + startFrame)

        for (frame in startFrame until endFrame) {
            val tSec = (frame - startFrame).toDouble() / SAMPLE_RATE

            val (rawL, rawR) = when (voiceType) {
                VoiceType.CHORDS -> synthesizeChordsSample(freq, tSec, noteDurSec)
                VoiceType.BASS -> synthesizeBassSample(freq, tSec, noteDurSec)
                VoiceType.LEAD -> synthesizeLeadSample(freq, tSec, noteDurSec)
                VoiceType.DRUMS -> synthesizeDrumSample(note.pitch, tSec, random)
            }

            leftBuffer[frame] += rawL * velGain * leftGain
            rightBuffer[frame] += rawR * velGain * rightGain
        }
    }

    /**
     * Ch 0 (Chords): Polyphonic soft sine/triangle oscillators with ADSR envelope.
     */
    private fun synthesizeChordsSample(freq: Double, tSec: Double, noteDurSec: Double): Pair<Double, Double> {
        val sine = sin(2.0 * PI * freq * tSec)
        val triPhase = (freq * tSec) % 1.0
        val triangle = 2.0 * abs(2.0 * (triPhase - floor(triPhase + 0.5))) - 1.0

        val oscSignal = 0.6 * sine + 0.4 * triangle
        val env = calculateAdsr(tSec, noteDurSec, attackSec = 0.02, decaySec = 0.20, sustainLevel = 0.60, releaseSec = 0.25)

        val sample = oscSignal * env * 0.5
        return Pair(sample, sample)
    }

    /**
     * Ch 1 (Bass): Saturated low-end saw/square wave.
     */
    private fun synthesizeBassSample(freq: Double, tSec: Double, noteDurSec: Double): Pair<Double, Double> {
        val sawPhase = (freq * tSec) % 1.0
        val saw = 2.0 * sawPhase - 1.0
        val square = if (sawPhase < 0.5) 1.0 else -1.0

        val rawBass = 0.5 * saw + 0.5 * square
        val saturated = tanh(rawBass * 2.2)

        val env = calculateAdsr(tSec, noteDurSec, attackSec = 0.008, decaySec = 0.18, sustainLevel = 0.80, releaseSec = 0.12)
        val sample = saturated * env * 0.65
        return Pair(sample, sample)
    }

    /**
     * Ch 2 (Lead): Pulse wave lead with subtle vibrato.
     */
    private fun synthesizeLeadSample(freq: Double, tSec: Double, noteDurSec: Double): Pair<Double, Double> {
        val vibratoRate = 5.5
        val vibratoDepth = 0.008 // subtle ~8 cents vibrato
        val modFreq = freq * (1.0 + vibratoDepth * sin(2.0 * PI * vibratoRate * tSec))

        val pulsePhase = (modFreq * tSec) % 1.0
        val pulse = if (pulsePhase < 0.35) 0.8 else -0.8

        val env = calculateAdsr(tSec, noteDurSec, attackSec = 0.012, decaySec = 0.15, sustainLevel = 0.70, releaseSec = 0.18)
        val sample = pulse * env * 0.50
        return Pair(sample, sample)
    }

    /**
     * Ch 9 (Drums): Synthesized kick, snare noise, and hat ticks.
     */
    private fun synthesizeDrumSample(pitch: Int, tSec: Double, random: Random): Pair<Double, Double> {
        var left = 0.0
        var right = 0.0

        when (pitch) {
            35, 36 -> { // Kick
                if (tSec < 0.25) {
                    val fSweep = 45.0 + 95.0 * exp(-tSec * 45.0)
                    val kickBody = sin(2.0 * PI * fSweep * tSec)
                    val click = (random.nextDouble() * 2.0 - 1.0) * exp(-tSec * 120.0)
                    val env = exp(-tSec * 16.0)
                    val kick = (kickBody + 0.2 * click) * env * 0.85
                    left = kick
                    right = kick
                }
            }
            38, 40 -> { // Snare
                if (tSec < 0.22) {
                    val tone = sin(2.0 * PI * 185.0 * tSec)
                    val noise = random.nextDouble() * 2.0 - 1.0
                    val env = exp(-tSec * 18.0)
                    val snare = (0.4 * tone + 0.6 * noise) * env * 0.75
                    left = snare
                    right = snare
                }
            }
            42, 44 -> { // Closed Hi-Hat
                if (tSec < 0.08) {
                    val noise = random.nextDouble() * 2.0 - 1.0
                    val metal = sin(2.0 * PI * 7000.0 * tSec)
                    val env = exp(-tSec * 55.0)
                    val hat = (0.7 * noise + 0.3 * metal) * env * 0.35
                    left = hat * 0.4
                    right = hat * 0.6
                }
            }
            46 -> { // Open Hi-Hat
                if (tSec < 0.28) {
                    val noise = random.nextDouble() * 2.0 - 1.0
                    val metal = sin(2.0 * PI * 6500.0 * tSec)
                    val env = exp(-tSec * 14.0)
                    val hat = (0.7 * noise + 0.3 * metal) * env * 0.45
                    left = hat * 0.45
                    right = hat * 0.55
                }
            }
            else -> { // Other percussion
                if (tSec < 0.15) {
                    val freq = midiPitchToFreq(pitch).coerceIn(100.0, 3000.0)
                    val tone = sin(2.0 * PI * freq * tSec)
                    val env = exp(-tSec * 22.0)
                    val perc = tone * env * 0.45
                    left = perc
                    right = perc
                }
            }
        }
        return Pair(left, right)
    }

    private fun calculateAdsr(
        tSec: Double,
        noteDurSec: Double,
        attackSec: Double,
        decaySec: Double,
        sustainLevel: Double,
        releaseSec: Double
    ): Double {
        val totalSec = noteDurSec + releaseSec
        if (tSec < 0.0 || tSec > totalSec) return 0.0

        return when {
            tSec < attackSec && attackSec > 0.0 -> {
                tSec / attackSec
            }
            tSec < (attackSec + decaySec) && decaySec > 0.0 -> {
                val decayProgress = (tSec - attackSec) / decaySec
                1.0 - decayProgress * (1.0 - sustainLevel)
            }
            tSec < noteDurSec -> {
                sustainLevel
            }
            else -> {
                val relTime = tSec - noteDurSec
                if (releaseSec > 0.0) {
                    val relProgress = (relTime / releaseSec).coerceIn(0.0, 1.0)
                    sustainLevel * (1.0 - relProgress).pow(1.5)
                } else 0.0
            }
        }
    }

    private fun midiPitchToFreq(pitch: Int): Double {
        return 440.0 * 2.0.pow((pitch - 69) / 12.0)
    }
}

/**
 * Renderer interface for rendering preview.wav files.
 */
object AudioPreviewRenderer {
    /**
     * Renders a 16-bit PCM stereo WAV buffer (preview.wav, 44.1 kHz) from a [SongArrangement].
     */
    fun renderPreview(arrangement: SongArrangement, totalBeats: Double = 16.0): ByteArray {
        return WavSynthesizer.renderToWav(arrangement, totalBeats)
    }

    /**
     * Renders a 16-bit PCM stereo WAV buffer (preview.wav, 44.1 kHz) from instrument tracks.
     */
    fun renderPreview(tracks: List<InstrumentTrack>, bpm: Int, totalBeats: Double = 16.0): ByteArray {
        return WavSynthesizer.renderToWav(tracks, bpm, totalBeats)
    }
}
