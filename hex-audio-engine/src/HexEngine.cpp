#include "HexEngine.h"
#include <algorithm>
#include <iostream>

namespace HexAudio {

HexEngine::HexEngine(double initialBpm, int totalSteps)
    : m_sampleRate(44100.0),
      m_isRunning(false),
      m_masterVolume(0.85f),
      m_bpm(initialBpm),
      m_lookaheadMs(25.0),
      m_scheduleAheadTime(0.1),
      m_current16thStep(0),
      m_nextNoteTime(0.0),
      m_currentTime(0.0),
      m_totalSteps(totalSteps),
      m_onStepTrigger(nullptr) {

    // Pre-allocate voice pools to avoid GC/malloc on real-time thread
    m_poolKick.resize(POOL_SIZE_KICK);
    m_poolSnare.resize(POOL_SIZE_SNARE);
    m_poolHiHat.resize(POOL_SIZE_HIHAT);
    m_poolPerc.resize(POOL_SIZE_PERC);
    m_poolBass.resize(POOL_SIZE_BASS);
    m_poolChord.resize(POOL_SIZE_CHORD);
    m_poolMelody.resize(POOL_SIZE_MELODY);
}

HexEngine::~HexEngine() {
    stop();
}

void HexEngine::init(double sampleRate) {
    m_sampleRate = sampleRate;
}

void HexEngine::start() {
    if (m_isRunning) return;

    m_isRunning = true;
    m_current16thStep = 0;
    m_currentTime = 0.0;
    m_nextNoteTime = 0.05; // initial offset
}

void HexEngine::stop() {
    m_isRunning = false;
    m_current16thStep = 0;
}

void HexEngine::registerTrack(const TrackConfig& track) {
    m_tracks[track.id] = track;
}

void HexEngine::clearTracks() {
    m_tracks.clear();
}

void HexEngine::addStepToTrack(const std::string& trackId, const HexStep& step) {
    auto it = m_tracks.find(trackId);
    if (it != m_tracks.end()) {
        it->second.steps.push_back(step);
    }
}

void HexEngine::setStepState(const std::string& trackId, int stepIndex, bool active, float velocity) {
    auto it = m_tracks.find(trackId);
    if (it != m_tracks.end()) {
        for (auto& step : it->second.steps) {
            if (step.stepIndex == stepIndex) {
                step.active = active;
                step.velocity = velocity;
                break;
            }
        }
    }
}

void HexEngine::setBpm(double newBpm) {
    m_bpm = std::max(30.0, std::min(300.0, newBpm));
}

void HexEngine::setMasterVolume(float value) {
    m_masterVolume = std::max(0.0f, std::min(1.0f, value));
}

void HexEngine::setOnStepTrigger(PlayheadCallback callback) {
    m_onStepTrigger = callback;
}

void HexEngine::processAudio(float* outputBuffer, int numFrames) {
    // Clear buffer (silence)
    for (int i = 0; i < numFrames * 2; ++i) {
        outputBuffer[i] = 0.0f;
    }

    if (!m_isRunning) {
        return;
    }

    // A real C++ engine runs the scheduler based on processed samples
    scheduler();

    // The render functions already accumulate into the buffer using +=
    // BUT we need to apply Master Volume at the end, so we process voices natively
    // directly into outputBuffer (zero temp allocation)

    for (auto& voice : m_poolKick) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolSnare) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolHiHat) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolPerc) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolBass) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolChord) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }
    for (auto& voice : m_poolMelody) {
        if (voice.isActive()) voice.render(outputBuffer, numFrames, m_sampleRate, m_currentTime);
    }

    // Apply master volume and simple soft-clipping limiter directly on the buffer
    for (int i = 0; i < numFrames * 2; ++i) {
        outputBuffer[i] *= m_masterVolume;

        // Soft clipping / Limiter (tanh approx) to prevent harsh digital clipping
        // when multiple loud voices stack up
        if (outputBuffer[i] > 1.0f) outputBuffer[i] = 1.0f;
        if (outputBuffer[i] < -1.0f) outputBuffer[i] = -1.0f;
    }

    // Advance current time based on frames processed
    m_currentTime += (double)numFrames / m_sampleRate;
}

void HexEngine::scheduler() {
    // Schedule ahead logic mapped from JS
    while (m_nextNoteTime < m_currentTime + m_scheduleAheadTime) {
        scheduleStep(m_current16thStep, m_nextNoteTime);
        advanceStep();
    }
}

void HexEngine::advanceStep() {
    double secondsPerBeat = 60.0 / m_bpm;
    double secondsPer16th = 0.25 * secondsPerBeat;
    m_nextNoteTime += secondsPer16th;
    m_current16thStep = (m_current16thStep + 1) % m_totalSteps;
}

void HexEngine::scheduleStep(int stepIndex, double time) {
    bool hasSolo = false;
    for (const auto& pair : m_tracks) {
        if (pair.second.soloed) {
            hasSolo = true;
            break;
        }
    }

    for (const auto& pair : m_tracks) {
        const auto& track = pair.second;
        if (track.muted) continue;
        if (hasSolo && !track.soloed) continue;

        for (const auto& step : track.steps) {
            if (step.stepIndex == stepIndex && step.active) {
                triggerVoice(track.voiceType, time, step.velocity * track.gain, track.pan, step.pitchOffset);

                // Real-time engine would handle callback safely
                // typically pushing to a lock-free queue for the UI thread
                if (m_onStepTrigger) {
                    // For now, call immediately (in a real system, defer to UI thread)
                    // m_onStepTrigger(stepIndex, track.id, time);
                }
                break;
            }
        }
    }
}

void HexEngine::triggerVoice(VoiceType type, double time, float gainValue, float pan, float pitchOffset) {
    // Zero-allocation trigger: Find an inactive voice in the pool and trigger it.
    // If pool is exhausted, the oldest voice will naturally be ignored (or we could steal it,
    // but ignoring is safer for simple percussion for now).

    switch (type) {
        case VoiceType::Kick:
            for (auto& voice : m_poolKick) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::Snare:
            for (auto& voice : m_poolSnare) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::HiHat:
            for (auto& voice : m_poolHiHat) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::PercHigh:
        case VoiceType::PercLow:
            for (auto& voice : m_poolPerc) {
                if (!voice.isActive()) {
                    voice.setBaseFreq(type == VoiceType::PercHigh ? 420.0f : 210.0f);
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::Bass:
            for (auto& voice : m_poolBass) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::Chord:
            for (auto& voice : m_poolChord) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
        case VoiceType::Melody:
            for (auto& voice : m_poolMelody) {
                if (!voice.isActive()) {
                    voice.trigger(time, gainValue, pan, pitchOffset);
                    break;
                }
            }
            break;
    }
}

} // namespace HexAudio
