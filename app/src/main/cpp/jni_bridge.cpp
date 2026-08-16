#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include "HexEngine.h"

#define LOG_TAG "HexAudioJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace oboe;
using namespace HexAudio;

class OboeAudioPlayer : public AudioStreamDataCallback {
public:
    OboeAudioPlayer() {
        mEngine = std::make_unique<HexEngine>(120.0, 16);
    }

    ~OboeAudioPlayer() {
        stop();
    }

    bool start() {
        AudioStreamBuilder builder;
        builder.setDirection(Direction::Output)
               ->setPerformanceMode(PerformanceMode::LowLatency)
               ->setSharingMode(SharingMode::Exclusive)
               ->setFormat(AudioFormat::Float)
               ->setChannelCount(ChannelCount::Stereo)
               ->setDataCallback(this);

        Result result = builder.openStream(mStream);
        if (result != Result::OK) {
            LOGE("Failed to open stream: %s", convertToText(result));
            return false;
        }

        mEngine->init(mStream->getSampleRate());

        // --- HARDCODE A TEST BEAT FOR NOW (Will be exposed to Kotlin later) ---
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

        mEngine->registerTrack(kickTrack);
        mEngine->registerTrack(snareTrack);
        mEngine->registerTrack(hihatTrack);
        // --------------------------------------------------------------------

        mEngine->start();

        result = mStream->requestStart();
        if (result != Result::OK) {
            LOGE("Failed to start stream: %s", convertToText(result));
            return false;
        }

        LOGI("Oboe audio stream started successfully.");
        return true;
    }

    void stop() {
        if (mStream) {
            mStream->stop();
            mStream->close();
            mStream.reset();
        }
        if (mEngine) {
            mEngine->stop();
        }
        LOGI("Oboe audio stream stopped.");
    }

    void setBpm(double bpm) {
        if (mEngine) {
            mEngine->setBpm(bpm);
        }
    }

    DataCallbackResult onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        float *floatData = static_cast<float *>(audioData);

        if (mEngine) {
            // Process DSP directly into the Oboe buffer (Zero-allocation!)
            mEngine->processAudio(floatData, numFrames);
        } else {
            // Silence
            for (int i = 0; i < numFrames * 2; ++i) {
                floatData[i] = 0.0f;
            }
        }

        return DataCallbackResult::Continue;
    }

private:
    std::shared_ptr<AudioStream> mStream;
    std::unique_ptr<HexEngine> mEngine;
};

// Global instance for the JNI bridge
static OboeAudioPlayer* gPlayer = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_initEngineNative(JNIEnv *env, jobject thiz) {
    if (gPlayer == nullptr) {
        gPlayer = new OboeAudioPlayer();
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_startNative(JNIEnv *env, jobject thiz) {
    if (gPlayer) {
        return gPlayer->start() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_stopNative(JNIEnv *env, jobject thiz) {
    if (gPlayer) {
        gPlayer->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_setBpmNative(JNIEnv *env, jobject thiz, jdouble bpm) {
    if (gPlayer) {
        gPlayer->setBpm(bpm);
    }
}

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_releaseEngineNative(JNIEnv *env, jobject thiz) {
    if (gPlayer) {
        delete gPlayer;
        gPlayer = nullptr;
    }
}

} // extern "C"
