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

    // Render all active voices and mix them into outputBuffer
    for (auto it = m_activeVoices.begin(); it != m_activeVoices.end(); ) {
        // Create a temporary buffer for this voice to render into
        std::vector<float> voiceBuffer(numFrames * 2, 0.0f);

        bool stillActive = (*it)->render(voiceBuffer.data(), numFrames, m_sampleRate, m_currentTime);

        // Mix into main buffer with master volume
        for (int i = 0; i < numFrames * 2; ++i) {
            outputBuffer[i] += voiceBuffer[i] * m_masterVolume;
        }

        if (!stillActive) {
            // Voice finished processing, remove it
            it = m_activeVoices.erase(it);
        } else {
            ++it;
        }
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
    std::unique_ptr<VoiceDSP> voice;

    switch (type) {
        case VoiceType::Kick:
            voice = std::make_unique<KickDSP>();
            break;
        case VoiceType::Snare:
            voice = std::make_unique<SnareDSP>();
            break;
        case VoiceType::HiHat:
            voice = std::make_unique<HiHatDSP>();
            break;
        case VoiceType::PercHigh:
            voice = std::make_unique<PercDSP>(420.0f);
            break;
        case VoiceType::PercLow:
            voice = std::make_unique<PercDSP>(210.0f);
            break;
        default:
            return;
    }

    if (voice) {
        voice->trigger(time, gainValue, pan, pitchOffset);
        m_activeVoices.push_back(std::move(voice));
    }
}

} // namespace HexAudio
