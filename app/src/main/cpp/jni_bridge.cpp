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
        // Prevent resource leaks by explicitly stopping and closing any existing stream
        stop();

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
            if (mStream->getState() != StreamState::Closed && mStream->getState() != StreamState::Closing) {
                mStream->stop();
                mStream->close();
            }
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

    void clearTracks() {
        if (mEngine) {
            mEngine->clearTracks();
        }
    }

    void addTrack(const std::string& id, const std::string& name, VoiceType type, float gain) {
        if (mEngine) {
            TrackConfig track{id, name, type, {}, gain, 0.0f, false, false};
            mEngine->registerTrack(track);
        }
    }

    void addStep(const std::string& trackId, int stepIndex, float velocity, float pitchOffset) {
        if (mEngine) {
            HexStep step{"s_" + std::to_string(stepIndex), {0,0,0}, stepIndex, true, velocity, pitchOffset};
            mEngine->addStepToTrack(trackId, step);
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

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_clearTracksNative(JNIEnv *env, jobject thiz) {
    if (gPlayer) {
        gPlayer->clearTracks();
    }
}

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_addTrackNative(JNIEnv *env, jobject thiz, jstring jId, jstring jName, jint jType, jfloat gain) {
    if (gPlayer) {
        const char *idChars = env->GetStringUTFChars(jId, 0);
        const char *nameChars = env->GetStringUTFChars(jName, 0);

        gPlayer->addTrack(std::string(idChars), std::string(nameChars), static_cast<VoiceType>(jType), gain);

        env->ReleaseStringUTFChars(jId, idChars);
        env->ReleaseStringUTFChars(jName, nameChars);
    }
}

JNIEXPORT void JNICALL
Java_com_example_songdice_audio_HexAudioPlayer_addStepNative(JNIEnv *env, jobject thiz, jstring jTrackId, jint stepIndex, jfloat velocity, jfloat pitchOffset) {
    if (gPlayer) {
        const char *idChars = env->GetStringUTFChars(jTrackId, 0);

        gPlayer->addStep(std::string(idChars), stepIndex, velocity, pitchOffset);

        env->ReleaseStringUTFChars(jTrackId, idChars);
    }
}

} // extern "C"
