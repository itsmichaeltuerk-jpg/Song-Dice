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
    if (!m_isRunning) {
        // Output silence
        for (int i = 0; i < numFrames * 2; ++i) {
            outputBuffer[i] = 0.0f;
        }
        return;
    }

    // A real C++ engine runs the scheduler based on processed samples
    // rather than relying on a Javascript setInterval.
    scheduler();

    // TODO: Process DSP objects and fill outputBuffer...
    // For now, output silence in the stub.
    for (int i = 0; i < numFrames * 2; ++i) {
        outputBuffer[i] = 0.0f;
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
    // TODO: Instantiate/trigger DSP voices here based on type.
    // Example: Kick, Snare, HiHat
    // std::cout << "Triggering voice: " << (int)type << " at time " << time << std::endl;
}

} // namespace HexAudio
