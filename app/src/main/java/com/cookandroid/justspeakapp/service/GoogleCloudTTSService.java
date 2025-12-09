package com.cookandroid.justspeakapp.service;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Google Cloud Text-to-Speech API를 사용한 TTS 서비스
 * 실제 남성/여성 음성 제공
 */
public class GoogleCloudTTSService {
    private static final String TAG = "GoogleCloudTTS";
    private static final String TTS_API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";

    private final String apiKey;
    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private MediaPlayer mediaPlayer;

    private String currentGender = "female";
    private float currentSpeed = 1.0f;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;

    // 음성 ID (Wavenet - 자연스러운 음성)
    private static final String VOICE_MALE = "en-US-Wavenet-D";      // 남성
    private static final String VOICE_FEMALE = "en-US-Wavenet-F";    // 여성

    public interface OnInitListener {
        void onInit(boolean success);
    }

    public interface SpeakCallback {
        void onStart();
        void onDone();
        void onError(String error);
    }

    public GoogleCloudTTSService(Context context, String apiKey, OnInitListener listener) {
        this.context = context;
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();

        // API 키 유효성 간단 체크
        if (apiKey != null && !apiKey.isEmpty()) {
            isInitialized = true;
            Log.d(TAG, "Google Cloud TTS initialized with API key");
        } else {
            isInitialized = false;
            Log.e(TAG, "Google Cloud TTS: No API key provided");
        }

        if (listener != null) {
            listener.onInit(isInitialized);
        }
    }

    /**
     * 음성 성별 설정
     */
    public void setVoiceGender(String gender) {
        this.currentGender = gender;
        Log.d(TAG, "Voice gender set to: " + gender);
    }

    /**
     * 음성 속도 설정 (0.25 ~ 4.0, 기본값 1.0)
     */
    public void setSpeechRate(float speed) {
        this.currentSpeed = speed;
        Log.d(TAG, "Speech rate set to: " + speed);
    }

    /**
     * 설정 일괄 적용
     */
    public void applySettings(String gender, float speed) {
        setVoiceGender(gender);
        setSpeechRate(speed);
    }

    /**
     * 텍스트를 음성으로 변환하여 재생
     */
    public void speak(String text) {
        speak(text, null);
    }

    public void speak(String text, SpeakCallback callback) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized");
            if (callback != null) callback.onError("TTS not initialized");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text, skipping");
            return;
        }

        // 이전 재생 중지
        stop();

        executor.submit(() -> {
            try {
                if (callback != null) {
                    new android.os.Handler(context.getMainLooper()).post(callback::onStart);
                }

                isSpeaking = true;

                // API 요청 JSON 생성
                String voiceName = "male".equals(currentGender) ? VOICE_MALE : VOICE_FEMALE;
                String requestJson = createRequestJson(text, voiceName, currentSpeed);

                Log.d(TAG, "Requesting TTS for: " + text.substring(0, Math.min(50, text.length())) + "...");
                Log.d(TAG, "Voice: " + voiceName + ", Speed: " + currentSpeed);

                // API 호출
                RequestBody body = RequestBody.create(
                        requestJson,
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(TTS_API_URL + "?key=" + apiKey)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        isSpeaking = false;
                        if (callback != null) {
                            new android.os.Handler(context.getMainLooper()).post(() ->
                                    callback.onError("API error: " + response.code()));
                        }
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String audioContent = jsonResponse.getString("audioContent");

                    // Base64 디코딩
                    byte[] audioData = Base64.decode(audioContent, Base64.DEFAULT);

                    // 임시 파일로 저장
                    File tempFile = File.createTempFile("tts_audio", ".mp3", context.getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(audioData);
                    }

                    // MediaPlayer로 재생
                    playAudioFile(tempFile, callback);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in TTS", e);
                isSpeaking = false;
                if (callback != null) {
                    new android.os.Handler(context.getMainLooper()).post(() ->
                            callback.onError(e.getMessage()));
                }
            }
        });
    }

    private String createRequestJson(String text, String voiceName, float speed) {
        try {
            JSONObject input = new JSONObject();
            input.put("text", text);

            JSONObject voice = new JSONObject();
            voice.put("languageCode", "en-US");
            voice.put("name", voiceName);

            JSONObject audioConfig = new JSONObject();
            audioConfig.put("audioEncoding", "MP3");
            audioConfig.put("speakingRate", speed);
            audioConfig.put("pitch", 0.0);  // 기본 피치

            JSONObject request = new JSONObject();
            request.put("input", input);
            request.put("voice", voice);
            request.put("audioConfig", audioConfig);

            return request.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating request JSON", e);
            return "{}";
        }
    }

    private void playAudioFile(File audioFile, SpeakCallback callback) {
        new android.os.Handler(context.getMainLooper()).post(() -> {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );

                mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d(TAG, "Audio prepared, starting playback");
                    mp.start();
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Audio playback completed");
                    isSpeaking = false;
                    audioFile.delete();  // 임시 파일 삭제
                    if (callback != null) callback.onDone();
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                    isSpeaking = false;
                    if (callback != null) callback.onError("Playback error");
                    return true;
                });

                mediaPlayer.prepareAsync();

            } catch (IOException e) {
                Log.e(TAG, "Error playing audio", e);
                isSpeaking = false;
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaPlayer", e);
            }
        }
        isSpeaking = false;
    }

    public void shutdown() {
        stop();
        executor.shutdown();
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isSpeaking() {
        return isSpeaking || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    public String getCurrentGender() {
        return currentGender;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }
}
