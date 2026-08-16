#include "DSP.h"
#include <cmath>
#include <cstdlib>

namespace HexAudio {

const double PI = 3.14159265358979323846;

// --- Envelope ---

Envelope::Envelope() : m_isActive(false), m_startTime(0), m_duration(0) {}

void Envelope::trigger(double startTime, double duration) {
    m_startTime = startTime;
    m_duration = duration;
    m_isActive = true;
}

void Envelope::process(float* buffer, int numFrames, double sampleRate, double currentTime) {
    // Stub: In a real implementation, apply exponential decay logic here.
}

// --- KickDSP ---

KickDSP::KickDSP() : m_triggerTime(0), m_gain(0), m_pan(0), m_pitchOffset(0), m_active(false), m_phase(0) {}

void KickDSP::trigger(double time, float gain, float pan, float pitchOffset) {
    m_triggerTime = time;
    m_gain = gain;
    m_pan = pan;
    m_pitchOffset = pitchOffset;
    m_active = true;
    m_phase = 0.0;
}

bool KickDSP::render(float* buffer, int numFrames, double sampleRate, double currentTime) {
    if (!m_active) return false;

    // Total kick duration mapped from JS
    double duration = 0.32;

    if (currentTime > m_triggerTime + duration) {
        m_active = false;
        return false;
    }

    // Process synthesis: Fast frequency drop + exponential volume decay
    for (int i = 0; i < numFrames; ++i) {
        double t = currentTime + (i / sampleRate);

        // Skip if not started yet
        if (t < m_triggerTime) continue;

        double timeSinceTrigger = t - m_triggerTime;

        // Pitch envelope: drops rapidly
        double freq = (150 * std::pow(2.0, m_pitchOffset / 12.0)) * std::exp(-timeSinceTrigger * 15.0);
        if (freq < 32.0) freq = 32.0;

        // Amplitude envelope
        double amp = m_gain * std::exp(-timeSinceTrigger * 12.0);

        // Sine wave gen
        m_phase += (2.0 * PI * freq) / sampleRate;
        if (m_phase >= 2.0 * PI) m_phase -= 2.0 * PI;
        float sample = (float)(std::sin(m_phase) * amp);

        // Equal power panning (pan is -1.0 to 1.0)
        float panVal = (m_pan + 1.0f) * 0.5f; // pan 0.0 to 1.0
        float gainL = std::cos(panVal * PI * 0.5f);
        float gainR = std::sin(panVal * PI * 0.5f);

        // Write to stereo interleaved buffer (accumulate)
        buffer[i * 2]     += sample * gainL; // L
        buffer[i * 2 + 1] += sample * gainR; // R
    }

    return true;
}

// --- OnePoleFilter ---

OnePoleFilter::OnePoleFilter() : m_a0(1.0), m_b1(0.0), m_z1(0.0), m_isHighpass(false) {}

void OnePoleFilter::setHighpass(double cutoff, double sampleRate) {
    m_isHighpass = true;
    double x = std::exp(-2.0 * PI * cutoff / sampleRate);
    m_a0 = (1.0 + x) / 2.0;
    m_b1 = -x;
}

void OnePoleFilter::setBandpass(double cutoff, double sampleRate) {
    // Simple 1-pole bandpass approx (actually lowpass for this basic implementation,
    // to keep it fast and stable without a full biquad).
    m_isHighpass = false;
    double x = std::exp(-2.0 * PI * cutoff / sampleRate);
    m_a0 = 1.0 - x;
    m_b1 = -x;
}

double OnePoleFilter::process(double input) {
    m_z1 = input * m_a0 - m_z1 * m_b1;
    if (m_isHighpass) {
        return input - m_z1; // Highpass = Input - Lowpass
    }
    return m_z1;
}

// --- Helper for Noise ---
static float generateNoise() {
    // Simple PRNG between -1 and 1
    return ((float)std::rand() / (float)RAND_MAX) * 2.0f - 1.0f;
}

// --- SnareDSP ---

SnareDSP::SnareDSP() : m_triggerTime(0), m_gain(0), m_pan(0), m_pitchOffset(0), m_active(false), m_phaseTone(0) {}

void SnareDSP::trigger(double time, float gain, float pan, float pitchOffset) {
    m_triggerTime = time;
    m_gain = gain;
    m_pan = pan;
    m_pitchOffset = pitchOffset;
    m_active = true;
    m_phaseTone = 0.0;
}

bool SnareDSP::render(float* buffer, int numFrames, double sampleRate, double currentTime) {
    if (!m_active) return false;
    double duration = 0.18;
    if (currentTime > m_triggerTime + duration) {
        m_active = false;
        return false;
    }

    // Snare filter (Highpass at 1000Hz)
    m_filter.setHighpass(1000.0, sampleRate);

    for (int i = 0; i < numFrames; ++i) {
        double t = currentTime + (i / sampleRate);
        if (t < m_triggerTime) continue;

        double timeSinceTrigger = t - m_triggerTime;

        // 1. Body Tone (Triangle wave)
        double toneFreq = 180.0 * std::pow(2.0, m_pitchOffset / 12.0);
        m_phaseTone += (2.0 * PI * toneFreq) / sampleRate;
        if (m_phaseTone >= 2.0 * PI) m_phaseTone -= 2.0 * PI;

        // Triangle approx
        float toneSample = 2.0f * std::abs(2.0f * ((float)(m_phaseTone / (2.0 * PI)) - std::floor((float)(m_phaseTone / (2.0 * PI)) + 0.5f))) - 1.0f;

        double toneAmp = (m_gain * 0.7) * std::exp(-timeSinceTrigger * 20.0);

        // 2. Noise Snap
        float noiseSample = generateNoise();
        noiseSample = m_filter.process(noiseSample);
        double noiseAmp = (m_gain * 0.8) * std::exp(-timeSinceTrigger * 15.0);

        float sample = (toneSample * toneAmp) + (noiseSample * noiseAmp);

        float panVal = (m_pan + 1.0f) * 0.5f;
        float gainL = std::cos(panVal * PI * 0.5f);
        float gainR = std::sin(panVal * PI * 0.5f);

        buffer[i * 2]     += sample * gainL; // L
        buffer[i * 2 + 1] += sample * gainR; // R
    }

    return true;
}

// --- HiHatDSP ---

HiHatDSP::HiHatDSP() : m_triggerTime(0), m_gain(0), m_pan(0), m_pitchOffset(0), m_active(false) {}

void HiHatDSP::trigger(double time, float gain, float pan, float pitchOffset) {
    m_triggerTime = time;
    m_gain = gain;
    m_pan = pan;
    m_pitchOffset = pitchOffset;
    m_active = true;
}

bool HiHatDSP::render(float* buffer, int numFrames, double sampleRate, double currentTime) {
    if (!m_active) return false;
    double duration = 0.05;
    if (currentTime > m_triggerTime + duration) {
        m_active = false;
        return false;
    }

    double cutoff = 8000.0 * std::pow(2.0, m_pitchOffset / 12.0);
    m_filter.setHighpass(cutoff, sampleRate); // Using highpass for hihat sizzle

    for (int i = 0; i < numFrames; ++i) {
        double t = currentTime + (i / sampleRate);
        if (t < m_triggerTime) continue;

        double timeSinceTrigger = t - m_triggerTime;

        float noiseSample = generateNoise();
        noiseSample = m_filter.process(noiseSample);

        double amp = (m_gain * 0.6) * std::exp(-timeSinceTrigger * 40.0); // Very fast decay
        float sample = noiseSample * amp;

        float panVal = (m_pan + 1.0f) * 0.5f;
        float gainL = std::cos(panVal * PI * 0.5f);
        float gainR = std::sin(panVal * PI * 0.5f);

        buffer[i * 2]     += sample * gainL;
        buffer[i * 2 + 1] += sample * gainR;
    }

    return true;
}

// --- PercDSP ---

PercDSP::PercDSP(float baseFreq) : m_baseFreq(baseFreq), m_triggerTime(0), m_gain(0), m_pan(0), m_pitchOffset(0), m_active(false), m_phase(0) {}

void PercDSP::trigger(double time, float gain, float pan, float pitchOffset) {
    m_triggerTime = time;
    m_gain = gain;
    m_pan = pan;
    m_pitchOffset = pitchOffset;
    m_active = true;
    m_phase = 0.0;
}

bool PercDSP::render(float* buffer, int numFrames, double sampleRate, double currentTime) {
    if (!m_active) return false;
    double duration = 0.15;
    if (currentTime > m_triggerTime + duration) {
        m_active = false;
        return false;
    }

    for (int i = 0; i < numFrames; ++i) {
        double t = currentTime + (i / sampleRate);
        if (t < m_triggerTime) continue;

        double timeSinceTrigger = t - m_triggerTime;

        // Pitch envelope: starts 1.5x higher, drops to baseFreq
        double baseTarget = m_baseFreq * std::pow(2.0, m_pitchOffset / 12.0);
        double freq = baseTarget + (baseTarget * 0.5 * std::exp(-timeSinceTrigger * 30.0));

        m_phase += (2.0 * PI * freq) / sampleRate;
        if (m_phase >= 2.0 * PI) m_phase -= 2.0 * PI;

        double amp = (m_gain * 0.75) * std::exp(-timeSinceTrigger * 15.0);
        float sample = (float)(std::sin(m_phase) * amp);

        float panVal = (m_pan + 1.0f) * 0.5f;
        float gainL = std::cos(panVal * PI * 0.5f);
        float gainR = std::sin(panVal * PI * 0.5f);

        buffer[i * 2]     += sample * gainL;
        buffer[i * 2 + 1] += sample * gainR;
    }

    return true;
}

} // namespace HexAudio
