#include "DSP.h"
#include <cmath>

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

        // Write to stereo interleaved buffer
        buffer[i * 2]     += sample; // L
        buffer[i * 2 + 1] += sample; // R
    }

    return true;
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

    // Stub: Tone + Noise generator to be filled
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

    // Stub: Bandpassed noise
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

    // Stub: Sine wave with quick attack
    return true;
}

} // namespace HexAudio
