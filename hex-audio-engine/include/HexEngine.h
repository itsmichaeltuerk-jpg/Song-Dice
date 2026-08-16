#pragma once

#include "DataTypes.h"
#include "DSP.h"
#include <unordered_map>
#include <string>
#include <functional>
#include <memory>
#include <vector>

namespace HexAudio {

class HexEngine {
public:
    using PlayheadCallback = std::function<void(int currentStep, const std::string& trackId, double time)>;

    HexEngine(double initialBpm = 120.0, int totalSteps = 16);
    ~HexEngine();

    // Context & State
    void init(double sampleRate);
    void start();
    void stop();

    // Process audio buffer
    // Interleaved output: [L, R, L, R, ...]
    void processAudio(float* outputBuffer, int numFrames);

    // Track & Pattern Management
    void registerTrack(const TrackConfig& track);
    void setStepState(const std::string& trackId, int stepIndex, bool active, float velocity = 1.0f);
    void setBpm(double newBpm);
    void setMasterVolume(float value);
    void setOnStepTrigger(PlayheadCallback callback);

private:
    void scheduler();
    void advanceStep();
    void scheduleStep(int stepIndex, double time);
    void triggerVoice(VoiceType type, double time, float gainValue, float pan, float pitchOffset);

    // Audio State
    double m_sampleRate;
    bool m_isRunning;
    float m_masterVolume;

    // Scheduler state
    double m_bpm;
    double m_lookaheadMs;
    double m_scheduleAheadTime;
    int m_current16thStep;
    double m_nextNoteTime;
    double m_currentTime; // Maintained based on samples processed
    int m_totalSteps;

    std::unordered_map<std::string, TrackConfig> m_tracks;
    PlayheadCallback m_onStepTrigger;

    // Zero-allocation object pools (fixed size)
    static constexpr int POOL_SIZE_KICK = 4;
    static constexpr int POOL_SIZE_SNARE = 4;
    static constexpr int POOL_SIZE_HIHAT = 8;
    static constexpr int POOL_SIZE_PERC = 8;

    std::vector<KickDSP> m_poolKick;
    std::vector<SnareDSP> m_poolSnare;
    std::vector<HiHatDSP> m_poolHiHat;
    std::vector<PercDSP> m_poolPerc;
};

} // namespace HexAudio
