#pragma once

#include <vector>

namespace HexAudio {

// A simple Envelope to handle ADSR (Attack, Decay, Sustain, Release)
// and exponential decay mimicking WebAudio's exponentialRampToValueAtTime
class Envelope {
public:
    Envelope();

    void trigger(double startTime, double duration);
    void process(float* buffer, int numFrames, double sampleRate, double currentTime);

    bool isActive() const { return m_isActive; }

private:
    bool m_isActive;
    double m_startTime;
    double m_duration;
};

// Abstract base class for a synth voice
class VoiceDSP {
public:
    virtual ~VoiceDSP() = default;

    virtual void trigger(double time, float gain, float pan, float pitchOffset) = 0;

    // Renders the voice output into a stereo interleaved buffer
    // Returns true if the voice is still active (making sound), false if finished
    virtual bool render(float* buffer, int numFrames, double sampleRate, double currentTime) = 0;
};

// Kick Drum Procedural Synth
class KickDSP : public VoiceDSP {
public:
    KickDSP();
    void trigger(double time, float gain, float pan, float pitchOffset) override;
    bool render(float* buffer, int numFrames, double sampleRate, double currentTime) override;

private:
    double m_triggerTime;
    float m_gain;
    float m_pan;
    float m_pitchOffset;
    bool m_active;

    // Oscillator state
    double m_phase;
};

// Snare Drum Procedural Synth
class SnareDSP : public VoiceDSP {
public:
    SnareDSP();
    void trigger(double time, float gain, float pan, float pitchOffset) override;
    bool render(float* buffer, int numFrames, double sampleRate, double currentTime) override;

private:
    double m_triggerTime;
    float m_gain;
    float m_pan;
    float m_pitchOffset;
    bool m_active;

    double m_phaseTone;
};

// HiHat Procedural Synth
class HiHatDSP : public VoiceDSP {
public:
    HiHatDSP();
    void trigger(double time, float gain, float pan, float pitchOffset) override;
    bool render(float* buffer, int numFrames, double sampleRate, double currentTime) override;

private:
    double m_triggerTime;
    float m_gain;
    float m_pan;
    float m_pitchOffset;
    bool m_active;
};

// Percussion Synth (High/Low)
class PercDSP : public VoiceDSP {
public:
    PercDSP(float baseFreq);
    void trigger(double time, float gain, float pan, float pitchOffset) override;
    bool render(float* buffer, int numFrames, double sampleRate, double currentTime) override;

private:
    float m_baseFreq;
    double m_triggerTime;
    float m_gain;
    float m_pan;
    float m_pitchOffset;
    bool m_active;

    double m_phase;
};

} // namespace HexAudio
