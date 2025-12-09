package com.cookandroid.justspeakapp.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechRecognitionService {
    private static final String TAG = "SpeechRecognition";
    private SpeechRecognizer speechRecognizer;
    private Context context;
    private SpeechRecognitionListener listener;
    private Handler mainHandler;
    private boolean isInitializing = false;

    public interface SpeechRecognitionListener {
        void onSpeechResult(String text, float confidence);
        void onSpeechError(String error);
        void onSpeechStart();
        void onSpeechEnd();
    }

    public SpeechRecognitionService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        // 메인 스레드에서 초기화 실행
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initializeSpeechRecognizer();
        } else {
            mainHandler.post(this::initializeSpeechRecognizer);
        }
    }

    private void initializeSpeechRecognizer() {
        Log.d(TAG, "Initializing SpeechRecognizer...");
        isInitializing = true;

        boolean isAvailable = SpeechRecognizer.isRecognitionAvailable(context);
        Log.d(TAG, "Speech recognition available: " + isAvailable);

        if (isAvailable) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
                Log.d(TAG, "SpeechRecognizer created: " + (speechRecognizer != null));

                if (speechRecognizer == null) {
                    Log.e(TAG, "Failed to create SpeechRecognizer - returned null");
                    isInitializing = false;
                    return;
                }

                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    if (listener != null) {
                        listener.onSpeechStart();
                    }
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Volume level changed
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    // Audio buffer received
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                    if (listener != null) {
                        listener.onSpeechEnd();
                    }
                }

                @Override
                public void onError(int error) {
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "Error code: " + error + " - " + errorMessage);

                    // ERROR_NO_MATCH나 ERROR_SPEECH_TIMEOUT은 사용자가 말하지 않은 경우 - 재시도 허용
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Log.d(TAG, "No speech detected - user can retry");
                    }

                    // ERROR_RECOGNIZER_BUSY인 경우 재초기화 시도
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        Log.d(TAG, "Recognizer busy - will reinitialize");
                        reinitialize();
                    }

                    if (listener != null) {
                        listener.onSpeechError(errorMessage);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    float[] confidenceScores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);

                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        float confidence = (confidenceScores != null && confidenceScores.length > 0)
                                ? confidenceScores[0] : 0.0f;

                        Log.d(TAG, "Result: " + text + " (confidence: " + confidence + ")");
                        if (listener != null) {
                            listener.onSpeechResult(text, confidence);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    // Partial results available
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    // Reserved for future use
                }
            });

                Log.d(TAG, "SpeechRecognizer initialization complete");
                isInitializing = false;

            } catch (Exception e) {
                Log.e(TAG, "Exception during SpeechRecognizer initialization: " + e.getMessage(), e);
                speechRecognizer = null;
                isInitializing = false;
            }
        } else {
            Log.e(TAG, "Speech recognition not available on this device");
            isInitializing = false;
        }
    }

    public void startListening() {
        Log.d(TAG, "=== startListening() called ===");
        Log.d(TAG, "speechRecognizer is null: " + (speechRecognizer == null));
        Log.d(TAG, "listener is null: " + (listener == null));
        Log.d(TAG, "isInitializing: " + isInitializing);

        // 초기화 중이면 잠시 대기 후 재시도
        if (isInitializing) {
            Log.d(TAG, "Still initializing, waiting 500ms...");
            mainHandler.postDelayed(this::startListening, 500);
            return;
        }

        // SpeechRecognizer가 null이면 재초기화 시도
        if (speechRecognizer == null) {
            Log.w(TAG, "SpeechRecognizer is null, attempting to reinitialize...");
            reinitialize();
            // 재초기화 후 500ms 대기 후 재시도
            mainHandler.postDelayed(() -> {
                if (speechRecognizer != null) {
                    doStartListening();
                } else {
                    Log.e(TAG, "SpeechRecognizer still null after reinitialization");
                    if (listener != null) {
                        listener.onSpeechError("Speech recognizer not available. Please restart the app.");
                    }
                }
            }, 500);
            return;
        }

        // 메인 스레드에서 실행 보장
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.d(TAG, "Not on main thread, posting to main handler...");
            mainHandler.post(this::doStartListening);
        } else {
            doStartListening();
        }
    }

    private void doStartListening() {
        Log.d(TAG, "=== doStartListening() called ===");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);

        if (speechRecognizer != null) {
            Log.d(TAG, "Starting speech recognition...");
            try {
                // Samsung 기기에서 약간의 지연이 필요할 수 있음
                speechRecognizer.startListening(intent);
                Log.d(TAG, "startListening() called successfully");
            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException - permission issue: " + se.getMessage(), se);
                if (listener != null) {
                    listener.onSpeechError("Permission denied for audio recording");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting speech recognition: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onSpeechError("Failed to start: " + e.getMessage());
                }
            }
        } else {
            Log.e(TAG, "SpeechRecognizer is null in doStartListening - cannot start listening");
            if (listener != null) {
                listener.onSpeechError("Speech recognizer not available");
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    public void reinitialize() {
        Log.d(TAG, "Reinitializing SpeechRecognizer...");
        destroy();
        initializeSpeechRecognizer();
    }

    public boolean isAvailable() {
        return speechRecognizer != null;
    }

    public void setListener(SpeechRecognitionListener listener) {
        this.listener = listener;
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }
}
