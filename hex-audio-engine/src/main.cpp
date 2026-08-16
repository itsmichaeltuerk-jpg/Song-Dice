#include "HexEngine.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdint>
#include <algorithm>

using namespace HexAudio;

// Simple WAV writer struct
struct WavHeader {
    char riff[4] = {'R', 'I', 'F', 'F'};
    uint32_t chunkSize;
    char wave[4] = {'W', 'A', 'V', 'E'};
    char fmt[4] = {'f', 'm', 't', ' '};
    uint32_t subchunk1Size = 16;
    uint16_t audioFormat = 1; // PCM
    uint16_t numChannels = 2;
    uint32_t sampleRate = 44100;
    uint32_t byteRate;
    uint16_t blockAlign;
    uint16_t bitsPerSample = 16;
    char data[4] = {'d', 'a', 't', 'a'};
    uint32_t subchunk2Size;
};

void writeWav(const std::string& filename, const std::vector<float>& buffer, uint32_t sampleRate) {
    std::ofstream file(filename, std::ios::binary);

    WavHeader header;
    header.sampleRate = sampleRate;
    header.byteRate = sampleRate * header.numChannels * (header.bitsPerSample / 8);
    header.blockAlign = header.numChannels * (header.bitsPerSample / 8);
    header.subchunk2Size = buffer.size() * (header.bitsPerSample / 8);
    header.chunkSize = 36 + header.subchunk2Size;

    file.write(reinterpret_cast<const char*>(&header), sizeof(WavHeader));

    // Convert float [-1.0, 1.0] to 16-bit PCM
    for (float sample : buffer) {
        // Soft clipping / Limiter
        if (sample > 1.0f) sample = 1.0f;
        if (sample < -1.0f) sample = -1.0f;

        int16_t pcm = static_cast<int16_t>(sample * 32767.0f);
        file.write(reinterpret_cast<const char*>(&pcm), sizeof(int16_t));
    }

    file.close();
}

int main() {
    std::cout << "Initializing HexAudioEngine Test Harness..." << std::endl;

    const double sampleRate = 44100.0;
    const double durationSeconds = 4.0;
    const int totalFrames = static_cast<int>(sampleRate * durationSeconds);

    HexEngine engine(120.0, 16);
    engine.init(sampleRate);

    // Setup a 4-to-the-floor beat
    TrackConfig kickTrack{"kick_1", "Kick", VoiceType::Kick, {}, 1.0f, 0.0f, false, false};
    for (int i = 0; i < 16; i += 4) {
        kickTrack.steps.push_back({"k" + std::to_string(i), {0,0,0}, i, true, 1.0f, 0.0f});
    }

    TrackConfig snareTrack{"snare_1", "Snare", VoiceType::Snare, {}, 0.8f, 0.0f, false, false};
    snareTrack.steps.push_back({"s4", {0,0,0}, 4, true, 1.0f, 0.0f});
    snareTrack.steps.push_back({"s12", {0,0,0}, 12, true, 1.0f, 0.0f});

    TrackConfig hihatTrack{"hihat_1", "HiHat", VoiceType::HiHat, {}, 0.6f, 0.0f, false, false};
    for (int i = 0; i < 16; i += 2) {
        hihatTrack.steps.push_back({"h" + std::to_string(i), {0,0,0}, i, true, 1.0f, 0.0f});
    }

    engine.registerTrack(kickTrack);
    engine.registerTrack(snareTrack);
    engine.registerTrack(hihatTrack);

    engine.start();

    std::cout << "Rendering " << durationSeconds << " seconds of audio..." << std::endl;

    // Render loop
    std::vector<float> finalBuffer(totalFrames * 2, 0.0f); // Stereo interleaved

    const int framesPerBlock = 128; // WebAudio/Oboe style block size
    std::vector<float> blockBuffer(framesPerBlock * 2, 0.0f);

    for (int i = 0; i < totalFrames; i += framesPerBlock) {
        int framesToProcess = std::min(framesPerBlock, totalFrames - i);

        engine.processAudio(blockBuffer.data(), framesToProcess);

        // Copy to final buffer
        for (int j = 0; j < framesToProcess * 2; ++j) {
            finalBuffer[i * 2 + j] = blockBuffer[j];
        }
    }

    std::string outputPath = "test_output.wav";
    writeWav(outputPath, finalBuffer, sampleRate);

    std::cout << "Render complete! Saved to " << outputPath << std::endl;

    return 0;
}
