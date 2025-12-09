package com.cookandroid.justspeakapp.service;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.cookandroid.justspeakapp.BuildConfig;

import java.util.Locale;

/**
 * TTS 서비스
 * Google Cloud TTS를 우선 사용하고, 실패 시 Android 기본 TTS로 폴백
 */
public class TextToSpeechService {
    private static final String TAG = "TTS";

    private Context context;
    private TextToSpeech androidTts;
    private GoogleCloudTTSService cloudTts;

    private boolean isInitialized = false;
    private boolean useCloudTts = false;

    private float currentSpeed = 1.0f;
    private float currentPitch = 1.0f;
    private String currentGender = "female";

    public interface OnInitListener {
        void onInit(boolean success);
    }

    public TextToSpeechService(Context context, final OnInitListener listener) {
        this.context = context;

        // Google Cloud TTS API 키 확인
        String cloudTtsKey = BuildConfig.GOOGLE_CLOUD_TTS_KEY;

        if (cloudTtsKey != null && !cloudTtsKey.isEmpty()) {
            // Google Cloud TTS 초기화 시도
            Log.d(TAG, "Initializing Google Cloud TTS...");
            cloudTts = new GoogleCloudTTSService(context, cloudTtsKey, success -> {
                if (success) {
                    useCloudTts = true;
                    isInitialized = true;
                    Log.d(TAG, "Google Cloud TTS initialized successfully");

                    if (listener != null) {
                        listener.onInit(true);
                    }
                } else {
                    // Cloud TTS 실패 시 Android TTS로 폴백
                    Log.w(TAG, "Google Cloud TTS failed, falling back to Android TTS");
                    initAndroidTts(listener);
                }
            });
        } else {
            // API 키가 없으면 Android TTS 사용
            Log.d(TAG, "No Cloud TTS key, using Android TTS");
            initAndroidTts(listener);
        }
    }

    /**
     * Android 기본 TTS 초기화
     */
    private void initAndroidTts(OnInitListener listener) {
        androidTts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = androidTts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Android TTS: Language not supported");
                    isInitialized = false;
                } else {
                    isInitialized = true;
                    useCloudTts = false;
                    androidTts.setPitch(1.0f);
                    androidTts.setSpeechRate(1.0f);
                    Log.d(TAG, "Android TTS initialized successfully");
                }

                if (listener != null) {
                    listener.onInit(isInitialized);
                }
            } else {
                Log.e(TAG, "Android TTS initialization failed");
                isInitialized = false;
                if (listener != null) {
                    listener.onInit(false);
                }
            }
        });
    }

    /**
     * 음성 속도 설정
     */
    public void setSpeechRate(float speed) {
        currentSpeed = speed;

        if (useCloudTts && cloudTts != null) {
            cloudTts.setSpeechRate(speed);
        } else if (androidTts != null) {
            androidTts.setSpeechRate(speed);
        }

        Log.d(TAG, "Speech rate set to: " + speed);
    }

    /**
     * 음성 성별 설정
     */
    public void setVoiceGender(String gender) {
        currentGender = gender;

        if (useCloudTts && cloudTts != null) {
            // Google Cloud TTS: 실제 남성/여성 음성 사용
            cloudTts.setVoiceGender(gender);
            Log.d(TAG, "Cloud TTS voice gender set to: " + gender);
        } else if (androidTts != null) {
            // Android TTS: 피치로 구분
            if ("male".equals(gender)) {
                currentPitch = 0.7f;
            } else {
                currentPitch = 1.3f;
            }
            androidTts.setPitch(currentPitch);
            Log.d(TAG, "Android TTS pitch set to: " + currentPitch + " for gender: " + gender);
        }
    }

    /**
     * 설정 일괄 적용
     */
    public void applySettings(String gender, float speed) {
        setSpeechRate(speed);
        setVoiceGender(gender);
        Log.d(TAG, "Applied settings - Gender: " + gender + ", Speed: " + speed + ", UseCloud: " + useCloudTts);
    }

    /**
     * 텍스트 읽기
     */
    public void speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized - cannot speak");
            return;
        }

        if (useCloudTts && cloudTts != null) {
            // Google Cloud TTS 사용
            Log.d(TAG, "Speaking with Google Cloud TTS: " + text);
            cloudTts.speak(text);
        } else if (androidTts != null) {
            // Android TTS 사용
            androidTts.setPitch(currentPitch);
            androidTts.setSpeechRate(currentSpeed);

            Log.d(TAG, "Speaking with Android TTS: " + text + " (pitch: " + currentPitch + ", speed: " + currentSpeed + ")");
            int result = androidTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error in Android TTS speak()");
            }
        }
    }

    public void stop() {
        if (useCloudTts && cloudTts != null) {
            cloudTts.stop();
        }
        if (androidTts != null) {
            androidTts.stop();
        }
    }

    public void shutdown() {
        if (cloudTts != null) {
            cloudTts.shutdown();
        }
        if (androidTts != null) {
            androidTts.stop();
            androidTts.shutdown();
        }
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isSpeaking() {
        if (useCloudTts && cloudTts != null) {
            return cloudTts.isSpeaking();
        }
        return androidTts != null && androidTts.isSpeaking();
    }

    public boolean isUsingCloudTts() {
        return useCloudTts;
    }

    public String getCurrentGender() {
        return currentGender;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }
}
