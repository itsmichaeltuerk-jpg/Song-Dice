#pragma once

#include <string>
#include <vector>

namespace HexAudio {

struct HexCoordinate {
    int q;
    int r;
    int s; // Invariant: q + r + s = 0

    bool operator==(const HexCoordinate& other) const {
        return q == other.q && r == other.r && s == other.s;
    }
};

struct HexStep {
    std::string id;
    HexCoordinate coord;
    int stepIndex;
    bool active;
    float velocity;
    float pitchOffset; // semitones
};

enum class VoiceType {
    Kick,
    Snare,
    HiHat,
    PercHigh,
    PercLow,
    Bass,
    Chord,
    Melody
};

struct TrackConfig {
    std::string id;
    std::string name;
    VoiceType voiceType;
    std::vector<HexStep> steps;
    float gain;
    float pan;
    bool muted;
    bool soloed;
};

} // namespace HexAudio
