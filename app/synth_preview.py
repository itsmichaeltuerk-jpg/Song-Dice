#!/usr/bin/env python3
"""
Song Dice - Advanced Multi-Track Audio Preview Synthesizer
-----------------------------------------------------------
Features:
1. Additive & Subtractive synthesis using NumPy.
2. ADSR (Attack, Decay, Sustain, Release) envelope generator.
3. Per-channel distinct synthesis characteristics:
   - Plucked/decaying additive harmonics for Chords/Keys.
   - Sub-bass low-pass filtered waveform for Bass.
   - Pitch-swept sine & filtered noise bursts for Drum transients.
4. Stereo 16-bit PCM WAV generation with spatial panning and soft-limiting.
"""

import sys
import math
import struct
import io
import wave
import numpy as np

SAMPLE_RATE = 44100

def generate_adsr_envelope(num_samples, sample_rate, attack_sec, decay_sec, sustain_level, release_sec, duration_sec):
    """
    Computes an ADSR envelope numpy array over time.
    """
    t = np.linspace(0, duration_sec, num_samples, endpoint=False)
    envelope = np.zeros(num_samples, dtype=np.float32)
    
    attack_samples = int(attack_sec * sample_rate)
    decay_samples = int(decay_sec * sample_rate)
    duration_samples = int(duration_sec * sample_rate)
    release_samples = int(release_sec * sample_rate)
    
    sustain_samples = max(0, duration_samples - attack_samples - decay_samples)
    
    for i in range(num_samples):
        if i < attack_samples and attack_samples > 0:
            // Attack: linear ramp 0.0 -> 1.0
            envelope[i] = i / float(attack_samples)
        elif i < (attack_samples + decay_samples) and decay_samples > 0:
            // Decay: 1.0 -> sustain_level
            progress = (i - attack_samples) / float(decay_samples)
            envelope[i] = 1.0 - progress * (1.0 - sustain_level)
        elif i < duration_samples:
            // Sustain: hold at sustain_level
            envelope[i] = sustain_level
        else:
            // Release: sustain_level -> 0.0
            rel_idx = i - duration_samples
            if release_samples > 0 and rel_idx < release_samples:
                envelope[i] = sustain_level * (1.0 - (rel_idx / float(release_samples)))
            else:
                envelope[i] = 0.0
                
    return envelope

def lowpass_filter(signal, cutoff_hz, sample_rate):
    """
    Single-pole lowpass subtractive filter implementation using NumPy.
    """
    rc = 1.0 / (2.0 * math.pi * cutoff_hz)
    dt = 1.0 / sample_rate
    alpha = dt / (rc + dt)
    
    filtered = np.zeros_like(signal)
    last_val = 0.0
    for i in range(len(signal)):
        last_val += alpha * (signal[i] - last_val)
        filtered[i] = last_val
    return filtered

def highpass_filter(signal, cutoff_hz, sample_rate):
    """
    Single-pole highpass subtractive filter implementation using NumPy.
    """
    lp = lowpass_filter(signal, cutoff_hz, sample_rate)
    return signal - lp

def midi_to_freq(pitch):
    return 440.0 * (2.0 ** ((pitch - 69) / 12.0))

def synthesize_chords_keys(pitch, duration_sec, velocity=100, sample_rate=SAMPLE_RATE):
    """
    Chords/Keys: Plucked/decaying envelope with additive saw/triangle harmonics & lowpass filtering.
    """
    freq = midi_to_freq(pitch)
    total_sec = duration_sec + 0.2 // include release decay
    num_samples = int(total_sec * sample_rate)
    t = np.linspace(0, total_sec, num_samples, endpoint=False)
    
    // Additive synthesis: Fundamental + 2nd, 3rd, 4th harmonics
    wave_fundamental = np.sin(2 * np.pi * freq * t)
    wave_h2 = 0.5 * np.sin(2 * np.pi * (2 * freq) * t)
    wave_h3 = 0.25 * np.sin(2 * np.pi * (3 * freq) * t)
    wave_h4 = 0.125 * np.sin(2 * np.pi * (4 * freq) * t)
    
    raw_additive = wave_fundamental + wave_h2 + wave_h3 + wave_h4
    
    // Subtractive synthesis: Warm lowpass filter at 1800 Hz
    filtered = lowpass_filter(raw_additive, 1800.0, sample_rate)
    
    // Plucked ADSR Envelope
    adsr = generate_adsr_envelope(
        num_samples=num_samples,
        sample_rate=sample_rate,
        attack_sec=0.01,
        decay_sec=0.25,
        sustain_level=0.3,
        release_sec=0.18,
        duration_sec=duration_sec
    )
    
    amp = (velocity / 127.0) * 0.4
    return filtered * adsr * amp

def synthesize_bass(pitch, duration_sec, velocity=100, sample_rate=SAMPLE_RATE):
    """
    Bass: Sub-bass low-pass filtered wave (sine fundamental + sub octave saw).
    """
    freq = midi_to_freq(pitch)
    total_sec = duration_sec + 0.15
    num_samples = int(total_sec * sample_rate)
    t = np.linspace(0, total_sec, num_samples, endpoint=False)
    
    // Sub-bass fundamental sine + saw wave
    sub_sine = np.sin(2 * np.pi * freq * t)
    saw = 2.0 * (freq * t - np.floor(0.5 + freq * t))
    
    combined = 0.7 * sub_sine + 0.3 * saw
    
    // Subtractive lowpass filter at 280 Hz for deep sub bass tone
    filtered_bass = lowpass_filter(combined, 280.0, sample_rate)
    
    // ADSR Envelope for punchy bass
    adsr = generate_adsr_envelope(
        num_samples=num_samples,
        sample_rate=sample_rate,
        attack_sec=0.015,
        decay_sec=0.12,
        sustain_level=0.8,
        release_sec=0.1,
        duration_sec=duration_sec
    )
    
    amp = (velocity / 127.0) * 0.6
    return filtered_bass * adsr * amp

def synthesize_drum_transient(pitch, sample_rate=SAMPLE_RATE):
    """
    Drums: Pitch-swept sine & filtered noise bursts for drum transients (Kick, Snare, Hi-Hat).
    """
    if pitch == 36: // Kick Drum
        duration_sec = 0.2
        num_samples = int(duration_sec * sample_rate)
        t = np.linspace(0, duration_sec, num_samples, endpoint=False)
        
        // Pitch sweep from 150 Hz down to 40 Hz exponentially
        freq_sweep = 40.0 + (110.0 * np.exp(-t * 35.0))
        phase = 2 * np.pi * np.cumsum(freq_sweep) / sample_rate
        kick_body = np.sin(phase)
        
        // Click transient
        click = highpass_filter(np.random.uniform(-1.0, 1.0, num_samples), 1000.0, sample_rate) * np.exp(-t * 100.0)
        
        envelope = generate_adsr_envelope(num_samples, sample_rate, 0.002, 0.15, 0.0, 0.05, duration_sec)
        return (kick_body + 0.2 * click) * envelope * 0.85

    elif pitch == 38: // Snare Drum
        duration_sec = 0.22
        num_samples = int(duration_sec * sample_rate)
        t = np.linspace(0, duration_sec, num_samples, endpoint=False)
        
        // Tonal body at 180 Hz
        tone = np.sin(2 * np.pi * 180.0 * t) * np.exp(-t * 25.0)
        
        // Filtered noise burst for snare wires
        noise = np.random.uniform(-1.0, 1.0, num_samples)
        filtered_noise = highpass_filter(noise, 800.0, sample_rate)
        
        envelope = generate_adsr_envelope(num_samples, sample_rate, 0.001, 0.18, 0.0, 0.04, duration_sec)
        return (0.4 * tone + 0.6 * filtered_noise) * envelope * 0.7

    elif pitch in (42, 46): // Hi-Hat (Closed/Open)
        duration_sec = 0.08 if pitch == 42 else 0.25
        num_samples = int(duration_sec * sample_rate)
        
        noise = np.random.uniform(-1.0, 1.0, num_samples)
        filtered_hat = highpass_filter(noise, 4000.0, sample_rate) // Bright sizzle
        
        envelope = generate_adsr_envelope(num_samples, sample_rate, 0.001, duration_sec * 0.7, 0.0, 0.02, duration_sec)
        return filtered_hat * envelope * 0.45

    else: // Toms / Cymbals
        duration_sec = 0.25
        num_samples = int(duration_sec * sample_rate)
        t = np.linspace(0, duration_sec, num_samples, endpoint=False)
        tone = np.sin(2 * np.pi * 120.0 * t) * np.exp(-t * 15.0)
        noise = highpass_filter(np.random.uniform(-1.0, 1.0, num_samples), 1500.0, sample_rate)
        envelope = generate_adsr_envelope(num_samples, sample_rate, 0.002, 0.2, 0.0, 0.05, duration_sec)
        return (0.5 * tone + 0.5 * noise) * envelope * 0.5

def render_preview_wav(bpm=120, total_beats=16.0):
    """
    Renders multi-track arrangement into a 16-bit PCM WAV stereo file buffer.
    """
    seconds_per_beat = 60.0 / bpm
    total_duration_sec = (total_beats * seconds_per_beat) + 0.5
    total_samples = int(total_duration_sec * SAMPLE_RATE)
    
    // Stereo buffers (Left & Right channels)
    left_channel = np.zeros(total_samples, dtype=np.float32)
    right_channel = np.zeros(total_samples, dtype=np.float32)
    
    // Sample track demo notes
    // Drums
    for beat in [0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0]:
        // Kick on 1 & 3, Snare on 2 & 4
        start_idx = int(beat * seconds_per_beat * SAMPLE_RATE)
        if beat % 2.0 == 0: // Kick
            kick = synthesize_drum_transient(36)
            end_idx = min(total_samples, start_idx + len(kick))
            left_channel[start_idx:end_idx] += kick[:end_idx - start_idx] * 0.5 // Center
            right_channel[start_idx:end_idx] += kick[:end_idx - start_idx] * 0.5
        else: // Snare
            snare = synthesize_drum_transient(38)
            end_idx = min(total_samples, start_idx + len(snare))
            left_channel[start_idx:end_idx] += snare[:end_idx - start_idx] * 0.6 // Slightly Left
            right_channel[start_idx:end_idx] += snare[:end_idx - start_idx] * 0.4
            
        // Hi-Hat on every beat
        hat = synthesize_drum_transient(42)
        end_idx = min(total_samples, start_idx + len(hat))
        left_channel[start_idx:end_idx] += hat[:end_idx - start_idx] * 0.3 // Panned Right
        right_channel[start_idx:end_idx] += hat[:end_idx - start_idx] * 0.7

    // Bass
    for (beat, pitch) in [(0.0, 36), (1.5, 36), (2.0, 39), (3.5, 41), (4.0, 36), (6.0, 43)]:
        start_idx = int(beat * seconds_per_beat * SAMPLE_RATE)
        bass_note = synthesize_bass(pitch, duration_sec=0.8 * seconds_per_beat)
        end_idx = min(total_samples, start_idx + len(bass_note))
        left_channel[start_idx:end_idx] += bass_note[:end_idx - start_idx] * 0.5 // Center Bass
        right_channel[start_idx:end_idx] += bass_note[:end_idx - start_idx] * 0.5

    // Chords / Keys
    for (beat, pitches) in [(0.0, [60, 63, 67]), (4.0, [58, 62, 65]), (8.0, [63, 67, 70]), (12.0, [58, 63, 67])]:
        start_idx = int(beat * seconds_per_beat * SAMPLE_RATE)
        for pitch in pitches:
            chord_note = synthesize_chords_keys(pitch, duration_sec=3.5 * seconds_per_beat)
            end_idx = min(total_samples, start_idx + len(chord_note))
            left_channel[start_idx:end_idx] += chord_note[:end_idx - start_idx] * 0.6 // Wide stereo
            right_channel[start_idx:end_idx] += chord_note[:end_idx - start_idx] * 0.4

    // Soft-clipping master limiter
    combined = np.stack([left_channel, right_channel], axis=1)
    max_val = np.max(np.abs(combined))
    if max_val > 0.95:
        combined = (combined / max_val) * 0.95
        
    // Convert to 16-bit PCM integer values
    pcm16 = (combined * 32767.0).astype(np.int16)
    
    // Output WAV stereo buffer
    buffer = io.BytesIO()
    with wave.open(buffer, 'wb') as wav_file:
        wav_file.setnchannels(2) // Stereo
        wav_file.setsampwidth(2) // 16-bit PCM (2 bytes)
        wav_file.setframerate(SAMPLE_RATE)
        wav_file.writeframes(pcm16.tobytes())
        
    return buffer.getvalue()

if __name__ == '__main__':
    wav_bytes = render_preview_wav()
    print(f"Generated 16-bit PCM Stereo WAV Preview Buffer ({len(wav_bytes)} bytes)")
    with open("preview_output.wav", "wb") as f:
        f.write(wav_bytes)
