package com.cookandroid.justspeakapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.adapter.ConversationAdapter;
import com.cookandroid.justspeakapp.model.ConversationMessage;
import com.cookandroid.justspeakapp.model.PronunciationFeedback;
import com.cookandroid.justspeakapp.model.Scenario;
import com.cookandroid.justspeakapp.service.GeminiService;
import com.cookandroid.justspeakapp.service.SpeechRecognitionService;
import com.cookandroid.justspeakapp.service.TextToSpeechService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.cookandroid.justspeakapp.data.UserDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;

/**
 * AI 연동 버전의 ConversationActivity
 *
 * 사용 방법:
 * 1. API_SETUP_GUIDE.md 파일을 참고하여 API 키를 발급받으세요
 * 2. 아래 상수에 발급받은 키를 입력하세요
 */
public class ConversationActivityWithAI extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvChatMessages;
    private FloatingActionButton fabMic;
    private TextView tvListeningStatus;
    private MaterialCardView cardFeedback;
    private TextView tvFeedback;
    private ImageButton btnBack, btnStop, btnCloseFeedback, btnSend;
    private LinearLayout textInputContainer;
    private RelativeLayout voiceInputContainer;
    private EditText etMessageInput;
    private boolean isSpeechRecognitionAvailable = false;

    // 레슨 완료 다이얼로그
    private FrameLayout lessonCompleteOverlay;
    private Button btnStopLesson, btnContinueLesson;
    private TextView tvLessonCompleteMessage;

    private GeminiService geminiService;
    private SpeechRecognitionService basicSpeechService;
    private TextToSpeechService ttsService;
    private ConversationAdapter adapter;
    private List<ConversationMessage> messages;

    private Scenario currentScenario;
    private boolean isListening = false;
    private boolean isWaitingForAIResponse = false; // AI 응답 대기 중 플래그
    private SharedPreferences prefs;
    private String userLevel;

    // 피드백 설정: 0=즉시, 1=대화 종료 후, 2=끄기
    private int feedbackTiming;
    private List<String> userMessagesForFeedback; // 대화 종료 후 피드백용

    // 학습 진도 관련
    private static final long LESSON_DURATION_MS = 3 * 60 * 1000; // 3분 = 1 레슨
    private UserDataManager userDataManager;
    private Timer lessonTimer;
    private long conversationStartTime;
    private boolean lessonCompleted = false;
    private String currentScenarioId;
    private int lastCompletedLessons = 0;
    private int lastTotalLessons = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userLevel = prefs.getString("user_level", "Beginner");
        feedbackTiming = prefs.getInt("feedback_timing", 0); // 0=즉시, 1=종료 후, 2=끄기
        userMessagesForFeedback = new ArrayList<>();
        userDataManager = new UserDataManager(this);

        initViews();
        checkPermissions();
        initRecyclerView();
        setupListeners();
        initServices(); // TTS 초기화 후 자동으로 startConversation 호출됨
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        fabMic = findViewById(R.id.fab_mic);
        tvListeningStatus = findViewById(R.id.tv_listening_status);
        cardFeedback = findViewById(R.id.card_feedback);
        tvFeedback = findViewById(R.id.tv_feedback);
        btnBack = findViewById(R.id.btn_back);
        btnStop = findViewById(R.id.btn_stop);
        btnCloseFeedback = findViewById(R.id.btn_close_feedback);

        // 텍스트 입력 관련 뷰
        textInputContainer = findViewById(R.id.text_input_container);
        voiceInputContainer = findViewById(R.id.voice_input_container);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);

        // 음성 인식 가능 여부 확인
        isSpeechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        android.util.Log.d("Speech", "Speech recognition available: " + isSpeechRecognitionAvailable);

        // 음성 인식 불가 시 텍스트 입력 모드로 전환
        if (!isSpeechRecognitionAvailable) {
            switchToTextInputMode();
        }

        // 레슨 완료 다이얼로그 뷰 초기화
        lessonCompleteOverlay = findViewById(R.id.lesson_complete_overlay);
        btnStopLesson = findViewById(R.id.btn_stop_lesson);
        btnContinueLesson = findViewById(R.id.btn_continue_lesson);
        tvLessonCompleteMessage = findViewById(R.id.tv_lesson_complete_message);
    }

    private void switchToTextInputMode() {
        android.util.Log.d("Speech", "Switching to text input mode");
        if (voiceInputContainer != null) {
            voiceInputContainer.setVisibility(View.GONE);
        }
        if (textInputContainer != null) {
            textInputContainer.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this,
                "음성 인식이 이 기기에서 지원되지 않습니다.\n텍스트로 입력해주세요.",
                Toast.LENGTH_LONG).show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "음성 인식 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "음성 인식 권한이 필요합니다", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initServices() {
        // Gemini AI 서비스 초기화
        try {
            geminiService = new GeminiService(this, BuildConfig.GEMINI_API_KEY);
            if (geminiService.isInitialized()) {
                android.util.Log.d("AI", "Gemini service initialized successfully");
            } else {
                android.util.Log.w("AI", "Gemini service created but not fully initialized");
                Toast.makeText(this, "AI 연결 준비 중... 인터넷 연결을 확인해주세요", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            geminiService = null;
            Toast.makeText(this, "AI 서비스 초기화 실패: 인터넷 연결을 확인해주세요", Toast.LENGTH_LONG).show();
            android.util.Log.e("AI", "Failed to initialize Gemini", e);
        }

        // 기본 음성 인식 초기화
        initBasicSpeech();

        // TTS 서비스 - 초기화 완료 후 대화 시작
        ttsService = new TextToSpeechService(this, success -> {
            // Handler.post()를 사용하여 ttsService 할당이 완료된 후 실행되도록 함
            new android.os.Handler(getMainLooper()).post(() -> {
                if (success) {
                    android.util.Log.d("TTS", "TTS initialized successfully");

                    // 사용자 음성 설정 적용
                    applyVoiceSettings();

                    Toast.makeText(this, "준비 완료!", Toast.LENGTH_SHORT).show();
                    // TTS 준비 완료 후 대화 시작
                    startConversation();
                } else {
                    android.util.Log.e("TTS", "TTS initialization failed");
                    Toast.makeText(this, "TTS 초기화 실패 - 소리가 나오지 않을 수 있습니다", Toast.LENGTH_SHORT).show();
                    // TTS 실패해도 대화는 시작
                    startConversation();
                }
            });
        });
    }

    /**
     * 저장된 음성 설정(성별, 속도)을 TTS에 적용
     */
    private void applyVoiceSettings() {
        if (ttsService == null || userDataManager == null) return;

        try {
            java.util.Map<String, Object> settings = userDataManager.getVoiceSettings();
            String voiceGender = (String) settings.get("voice_gender");
            float voiceSpeed = (float) settings.get("voice_speed");

            ttsService.applySettings(voiceGender, voiceSpeed);
            android.util.Log.d("TTS", "Voice settings applied - Gender: " + voiceGender + ", Speed: " + voiceSpeed);
        } catch (Exception e) {
            android.util.Log.e("TTS", "Failed to apply voice settings", e);
        }
    }

    private void initBasicSpeech() {
        basicSpeechService = new SpeechRecognitionService(this);
        basicSpeechService.setListener(new SpeechRecognitionService.SpeechRecognitionListener() {
            @Override
            public void onSpeechResult(String text, float confidence) {
                handleUserSpeech(text, confidence);
            }

            @Override
            public void onSpeechError(String error) {
                runOnUiThread(() -> {
                    tvListeningStatus.setVisibility(View.GONE);
                    isListening = false;

                    android.util.Log.e("Speech", "Error: " + error);

                    // 사용자 친화적인 에러 메시지
                    String userMessage;
                    if (error.contains("No speech match")) {
                        userMessage = "음성을 인식하지 못했습니다. 다시 시도해주세요.";
                    } else if (error.contains("No speech input")) {
                        userMessage = "음성이 감지되지 않았습니다. 마이크를 확인해주세요.";
                    } else if (error.contains("Network")) {
                        userMessage = "인터넷 연결을 확인해주세요.";
                    } else if (error.contains("Audio recording error")) {
                        userMessage = "마이크 접근 오류. 에뮬레이터 설정을 확인해주세요.";
                    } else {
                        userMessage = "음성 인식 오류: " + error;
                    }

                    Toast.makeText(ConversationActivityWithAI.this, userMessage, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onSpeechStart() {
                runOnUiThread(() -> {
                    tvListeningStatus.setVisibility(View.VISIBLE);
                    tvListeningStatus.setText("듣고 있습니다...");
                });
            }

            @Override
            public void onSpeechEnd() {
                runOnUiThread(() -> {
                    tvListeningStatus.setVisibility(View.GONE);
                    isListening = false;
                });
            }
        });
    }

    private void initRecyclerView() {
        messages = new ArrayList<>();
        adapter = new ConversationAdapter(messages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> handleConversationEnd());

        btnStop.setOnClickListener(v -> {
            stopListening();
            handleConversationEnd();
        });

        fabMic.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });

        btnCloseFeedback.setOnClickListener(v -> cardFeedback.setVisibility(View.GONE));

        // 텍스트 전송 버튼
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendTextMessage());
        }

        // 키보드 엔터 키로 전송
        if (etMessageInput != null) {
            etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    sendTextMessage();
                    return true;
                }
                return false;
            });
        }

        // 레슨 완료 다이얼로그 버튼 리스너
        if (btnStopLesson != null) {
            btnStopLesson.setOnClickListener(v -> {
                // 그만하기: 다이얼로그 닫고 화면 종료
                hideLessonCompleteDialog();
                finish();
            });
        }

        if (btnContinueLesson != null) {
            btnContinueLesson.setOnClickListener(v -> {
                // 계속하기: 다이얼로그 닫고 새 레슨 타이머 시작
                hideLessonCompleteDialog();
                startNewLessonTimer();
                addAIMessage("Great! Let's continue our conversation. What would you like to talk about?");
            });
        }
    }

    private void sendTextMessage() {
        if (etMessageInput == null) return;

        // AI 응답 대기 중이면 전송 차단
        if (isWaitingForAIResponse) {
            Toast.makeText(this, "AI 응답을 기다리는 중입니다...", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etMessageInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "메시지를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 입력 필드 초기화
        etMessageInput.setText("");

        // 텍스트 입력은 confidence를 0.8로 설정 (기본값)
        handleUserSpeech(text, 0.8f);
    }

    private void startConversation() {
        String scenarioId = getIntent().getStringExtra("scenario_id");
        String scenarioTitle = getIntent().getStringExtra("scenario_title");

        currentScenario = new Scenario(
                scenarioId != null ? scenarioId : "daily_conversation",
                scenarioTitle != null ? scenarioTitle : "일상대화",
                "🌻",
                "Basic daily conversation practice",
                "beginner",
                "daily");

        // 시나리오 ID 저장 및 타이머 시작
        currentScenarioId = scenarioId != null ? scenarioId : "scenario_daily";
        startLessonTimer();

        // Gemini AI 대화 시작 - 시나리오 ID 직접 사용
        String scenarioIdForAI = scenarioId != null ? scenarioId : "scenario_daily";
        android.util.Log.d("Conversation", "Starting conversation with scenario ID: " + scenarioIdForAI);

        if (geminiService != null && geminiService.isInitialized()) {
            geminiService.startConversation(scenarioIdForAI, userLevel);

            // 시나리오별 AI 첫 인사
            String greeting = getScenarioGreeting(scenarioIdForAI);
            addAIMessage(greeting);
            speakMessage(greeting);
        } else {
            // AI 서비스 없이 오프라인 모드로 진행
            android.util.Log.w("AI", "Starting in offline mode - AI not available");
            Toast.makeText(this, "AI 연결 실패. 오프라인 모드로 진행합니다.\n인터넷 연결을 확인해주세요.", Toast.LENGTH_LONG).show();

            // 오프라인 안내 메시지
            String offlineGreeting = "Welcome! (Offline Mode)\nAI is not connected. Please check your internet connection and restart the app.";
            addAIMessage(offlineGreeting);
            speakMessage("Welcome! AI is currently not connected.");
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "음성 인식 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }

        // TTS가 말하고 있으면 먼저 중지
        if (ttsService != null && ttsService.isSpeaking()) {
            android.util.Log.d("Speech", "Stopping TTS before listening...");
            ttsService.stop();
        }

        android.util.Log.d("Speech", "Starting to listen...");
        isListening = true;
        tvListeningStatus.setVisibility(View.VISIBLE);
        tvListeningStatus.setText("듣고 있습니다...");

        // 기본 음성 인식 사용
        if (basicSpeechService != null) {
            basicSpeechService.startListening();
            Toast.makeText(this, "말씀하세요...", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopListening() {
        isListening = false;
        tvListeningStatus.setVisibility(View.GONE);

        if (basicSpeechService != null) {
            basicSpeechService.stopListening();
        }
    }

    private void handleUserSpeech(String text, float confidence) {
        android.util.Log.d("Conversation", "=== handleUserSpeech called ===");
        android.util.Log.d("Conversation", "Text: " + text + ", Confidence: " + confidence);
        android.util.Log.d("Conversation", "Feedback timing setting: " + feedbackTiming);

        addUserMessage(text);

        // 피드백 설정에 따라 처리
        if (feedbackTiming == 0) {
            // 즉시 피드백: 문법/어휘 분석 + 레슨 종료 시 요약 피드백
            requestImmediateFeedback(text);
            userMessagesForFeedback.add(text); // 요약 피드백용으로도 저장
        } else if (feedbackTiming == 1) {
            // 대화 종료 후 피드백: 메시지 저장 (레슨 종료 시 요약 피드백)
            userMessagesForFeedback.add(text);
            android.util.Log.d("Conversation", "Message saved for later feedback. Total: " + userMessagesForFeedback.size());
        }
        // feedbackTiming == 2: 피드백 끄기 - 아무것도 안 함

        // AI 서비스 사용 가능 여부 확인
        android.util.Log.d("Conversation", "Checking AI service - geminiService null: " + (geminiService == null));
        if (geminiService != null) {
            android.util.Log.d("Conversation", "geminiService.isInitialized(): " + geminiService.isInitialized());
        }

        if (geminiService == null || !geminiService.isInitialized()) {
            // 오프라인 모드 - 기본 응답 제공
            android.util.Log.w("Conversation", "AI not available - using offline mode");
            String offlineResponse = getOfflineResponse(text);
            addAIMessage(offlineResponse);
            speakMessage(offlineResponse);
            Toast.makeText(this, "오프라인 모드: AI 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("Conversation", "Sending user message to Gemini AI...");

        // AI 응답 대기 시작
        isWaitingForAIResponse = true;
        setInputEnabled(false);

        // Gemini AI로 응답 생성
        geminiService.sendMessage(text, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String aiResponse) {
                android.util.Log.d("Conversation", "AI response SUCCESS: " + aiResponse);
                runOnUiThread(() -> {
                    // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                    if (isFinishing() || isDestroyed()) return;
                    isWaitingForAIResponse = false;
                    setInputEnabled(true);
                    addAIMessage(aiResponse);
                    speakMessage(aiResponse);
                });
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("Conversation", "AI response ERROR: " + error);
                runOnUiThread(() -> {
                    // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                    if (isFinishing() || isDestroyed()) return;
                    isWaitingForAIResponse = false;
                    setInputEnabled(true);
                    Toast.makeText(ConversationActivityWithAI.this,
                            error, Toast.LENGTH_LONG).show();
                    // 폴백 응답
                    String fallbackResponse = "I see. Could you tell me more about that?";
                    addAIMessage(fallbackResponse);
                    speakMessage(fallbackResponse);
                });
            }
        });
    }

    // 입력 활성화/비활성화
    private void setInputEnabled(boolean enabled) {
        if (btnSend != null) {
            btnSend.setEnabled(enabled);
            btnSend.setAlpha(enabled ? 1.0f : 0.5f);
        }
        if (etMessageInput != null) {
            etMessageInput.setEnabled(enabled);
        }
        if (fabMic != null) {
            fabMic.setEnabled(enabled);
            fabMic.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    // ========== 학습 진도 타이머 ==========
    private void startLessonTimer() {
        conversationStartTime = System.currentTimeMillis();
        lessonCompleted = false;

        lessonTimer = new Timer();
        lessonTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!lessonCompleted) {
                    lessonCompleted = true;
                    runOnUiThread(() -> completeLessonAndSaveProgress());
                }
            }
        }, LESSON_DURATION_MS);

        android.util.Log.d("Lesson", "Lesson timer started for scenario: " + currentScenarioId);
    }

    private void stopLessonTimer() {
        if (lessonTimer != null) {
            lessonTimer.cancel();
            lessonTimer = null;
        }
    }

    /**
     * 계속하기 선택 시 새 레슨 타이머 시작
     */
    private void startNewLessonTimer() {
        lessonCompleted = false;
        conversationStartTime = System.currentTimeMillis();

        lessonTimer = new Timer();
        lessonTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!lessonCompleted) {
                    lessonCompleted = true;
                    runOnUiThread(() -> completeLessonAndSaveProgress());
                }
            }
        }, LESSON_DURATION_MS);

        android.util.Log.d("Lesson", "New lesson timer started for continued learning");
    }

    /**
     * 레슨 완료 다이얼로그 표시
     */
    private void showLessonCompleteDialog(int completed, int total) {
        if (lessonCompleteOverlay == null) return;

        // 메시지 업데이트
        if (tvLessonCompleteMessage != null) {
            tvLessonCompleteMessage.setText(
                    "3분 대화를 완료했습니다!\n진도: " + completed + "/" + total + " 레슨\n\n계속 학습하시겠습니까?");
        }

        // 입력 비활성화
        setInputEnabled(false);

        // 다이얼로그 표시
        lessonCompleteOverlay.setVisibility(View.VISIBLE);
    }

    /**
     * 레슨 완료 다이얼로그 숨기기
     */
    private void hideLessonCompleteDialog() {
        if (lessonCompleteOverlay == null) return;

        lessonCompleteOverlay.setVisibility(View.GONE);

        // 입력 다시 활성화
        setInputEnabled(true);
    }

    private void completeLessonAndSaveProgress() {
        android.util.Log.d("Lesson", "Lesson completed for scenario: " + currentScenarioId);

        // 현재 진도 가져오기
        java.util.Map<String, Integer> currentProgress = userDataManager.getScenarioProgress(currentScenarioId);
        int completed = 0;
        int total = getDefaultTotalForScenario(currentScenarioId); // 시나리오별 기본 레슨 수

        if (currentProgress != null) {
            completed = currentProgress.get("completed") != null ? currentProgress.get("completed") : 0;
            // 기존에 저장된 total이 있으면 사용, 없으면 기본값 유지
            Integer savedTotal = currentProgress.get("total");
            if (savedTotal != null && savedTotal > 0) {
                total = savedTotal;
            }
        }

        // 레슨 완료 +1
        final int newCompleted = completed + 1;
        final int finalTotal = total;

        // 다이얼로그에서 사용할 값 저장
        lastCompletedLessons = newCompleted;
        lastTotalLessons = finalTotal;

        // 진도 저장 (로컬 + Firestore)
        userDataManager.updateLearningProgress(currentScenarioId, newCompleted, finalTotal,
                new UserDataManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("Lesson", "Progress saved: " + newCompleted + "/" + finalTotal);
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("Lesson", "Failed to save progress: " + error);
                    }
                });

        // 일일 학습 목표 업데이트
        updateDailyProgress();

        // 레슨 완료 메시지를 채팅방에 추가
        addAIMessage("🎉 축하합니다! 3분 대화를 완료했습니다.\n진도: " + newCompleted + "/" + finalTotal + " 레슨");

        // 피드백 설정이 켜져있고 (즉시 또는 대화 후) 메시지가 있으면 요약 피드백 제공
        if (feedbackTiming != 2 && !userMessagesForFeedback.isEmpty()) {
            generateLessonSummaryFeedback();
        } else {
            // 피드백이 없으면 바로 다이얼로그 표시
            showLessonCompleteDialog(newCompleted, finalTotal);
        }
    }

    /**
     * 레슨 종료 시 요약 피드백 생성 및 채팅방에 표시
     */
    private void generateLessonSummaryFeedback() {
        if (geminiService == null || !geminiService.isInitialized()) {
            android.util.Log.w("Feedback", "Cannot generate summary feedback - AI not available");
            return;
        }

        // 모든 사용자 메시지를 하나로 합침
        StringBuilder allMessages = new StringBuilder();
        for (int i = 0; i < userMessagesForFeedback.size(); i++) {
            allMessages.append((i + 1)).append(". ").append(userMessagesForFeedback.get(i)).append("\n");
        }

        String summaryPrompt = "You are an English tutor. A Korean student just completed a 3-minute English conversation lesson. " +
                "Analyze their messages below and provide detailed feedback IN KOREAN.\n\n" +
                "Student's messages:\n" + allMessages.toString() + "\n\n" +
                "Please provide feedback in this EXACT format:\n\n" +
                "📊 전체 평가\n[한 줄로 전체적인 평가]\n\n" +
                "✅ 장점\n" +
                "• [구체적인 장점 1]\n" +
                "• [구체적인 장점 2]\n" +
                "• [구체적인 장점 3] (있으면)\n\n" +
                "⚠️ 개선이 필요한 점 (약점 목록)\n" +
                "• [문법 오류 1]: \"틀린 문장\" → \"올바른 문장\"\n" +
                "• [문법 오류 2]: \"틀린 문장\" → \"올바른 문장\"\n" +
                "• [어휘 문제]: 설명\n" +
                "(각 약점을 요약하지 말고, 하나씩 구체적으로 나열해주세요)\n\n" +
                "🎯 다음 학습 팁\n[한 줄 조언]\n\n" +
                "IMPORTANT: Do NOT summarize weaknesses. List each weakness individually with specific examples from the student's messages. " +
                "Be encouraging but honest. Use Korean language throughout.";

        geminiService.sendMessage(summaryPrompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    // 요약 피드백을 AI 메시지로 채팅방에 추가
                    addAIMessage("📝 레슨 피드백\n\n" + response);
                    // 자동 스크롤
                    if (rvChatMessages != null && messages != null && !messages.isEmpty()) {
                        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
                    }

                    // 장점/약점 파싱 후 Firestore에 저장
                    parseFeedbackAndSave(response);

                    // 피드백 표시 후 잠시 대기 후 레슨 완료 다이얼로그 표시
                    new android.os.Handler(getMainLooper()).postDelayed(() -> {
                        showLessonCompleteDialog(lastCompletedLessons, lastTotalLessons);
                    }, 1500); // 1.5초 후 다이얼로그 표시
                });
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("Feedback", "Error generating summary feedback: " + error);
                runOnUiThread(() -> {
                    addAIMessage("📝 레슨이 완료되었습니다. 수고하셨어요! 🎉");
                    // 에러 시에도 다이얼로그 표시
                    showLessonCompleteDialog(lastCompletedLessons, lastTotalLessons);
                });
            }
        });

        // 메시지 리스트 초기화
        userMessagesForFeedback.clear();
    }

    /**
     * 피드백 응답에서 장점/약점을 파싱하여 Firestore에 저장
     */
    private void parseFeedbackAndSave(String feedbackResponse) {
        try {
            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();

            String[] lines = feedbackResponse.split("\n");
            boolean inStrengthSection = false;
            boolean inWeaknessSection = false;

            for (String line : lines) {
                line = line.trim();

                // 섹션 감지
                if (line.contains("장점") || line.contains("✅")) {
                    inStrengthSection = true;
                    inWeaknessSection = false;
                    continue;
                } else if (line.contains("개선") || line.contains("약점") || line.contains("⚠️")) {
                    inStrengthSection = false;
                    inWeaknessSection = true;
                    continue;
                } else if (line.contains("학습 팁") || line.contains("🎯") || line.contains("전체 평가") || line.contains("📊")) {
                    inStrengthSection = false;
                    inWeaknessSection = false;
                    continue;
                }

                // 항목 추출 (• 또는 - 로 시작하는 항목)
                if ((line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) && line.length() > 2) {
                    String item = line.substring(1).trim();
                    if (!item.isEmpty()) {
                        if (inStrengthSection) {
                            strengths.add(item);
                        } else if (inWeaknessSection) {
                            weaknesses.add(item);
                        }
                    }
                }
            }

            // Firestore에 저장
            if (!strengths.isEmpty() || !weaknesses.isEmpty()) {
                userDataManager.saveLessonFeedback(currentScenarioId, strengths, weaknesses,
                        new UserDataManager.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                android.util.Log.d("Feedback", "Feedback saved - Strengths: " +
                                        strengths.size() + ", Weaknesses: " + weaknesses.size());
                            }

                            @Override
                            public void onError(String error) {
                                android.util.Log.e("Feedback", "Failed to save feedback: " + error);
                            }
                        });
            }

        } catch (Exception e) {
            android.util.Log.e("Feedback", "Error parsing feedback", e);
        }
    }

    // 일일 학습 목표 업데이트 (사용자별 저장)
    private void updateDailyProgress() {
        // 날짜 변경 체크 및 초기화
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = dateFormat.format(new java.util.Date());

        // UserDataManager를 통해 사용자별 진도 가져오기
        java.util.Map<String, Object> dailyProgress = userDataManager.getDailyProgress();
        int dailyCompleted = (int) dailyProgress.get("daily_completed");
        int dailyGoal = (int) dailyProgress.get("daily_goal");
        String lastLearningDate = (String) dailyProgress.get("last_learning_date");

        // 날짜가 바뀌면 daily_completed 초기화
        if (!today.equals(lastLearningDate)) {
            dailyCompleted = 0;
            android.util.Log.d("Lesson", "New day detected, resetting daily progress for user");
        }

        dailyCompleted++;
        final int finalDailyCompleted = dailyCompleted;
        final int finalDailyGoal = dailyGoal;

        // UserDataManager를 통해 사용자별 진도 저장
        userDataManager.updateDailyProgress(dailyCompleted, today, new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("Lesson", "Daily progress updated for user: " + finalDailyCompleted + "/" + finalDailyGoal);
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("Lesson", "Failed to save daily progress: " + error);
            }
        });

        android.util.Log.d("Lesson", "Daily progress: " + dailyCompleted + "/" + dailyGoal);

        // 일일 목표 달성 시 달력에 오늘 날짜 표시 (사용자별)
        if (dailyCompleted >= dailyGoal) {
            userDataManager.markTodayAsLearned(new UserDataManager.OperationCallback() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("Lesson", "Daily goal achieved! Marked today as learned for user.");
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("Lesson", "Failed to mark today as learned: " + error);
                }
            });
            Toast.makeText(this, "🏆 오늘의 학습 목표 달성!", Toast.LENGTH_LONG).show();
        }
    }
    // ========================================

    // 시나리오별 기본 총 레슨 수 (ProgressActivity, ScenariosActivity와 일치)
    private int getDefaultTotalForScenario(String scenarioId) {
        if (scenarioId == null) return 10;

        switch (scenarioId) {
            case "scenario_daily":
            case "daily_conversation":
                return 10;
            case "scenario_travel":
            case "travel_english":
                return 8;
            case "scenario_shopping":
                return 6;
            case "scenario_restaurant":
                return 7;
            case "scenario_business":
            case "business":
                return 12;
            case "scenario_hotel":
                return 5;
            case "scenario_airport":
                return 6;
            case "scenario_medical":
                return 5;
            case "scenario_phone":
                return 7;
            case "scenario_job_interview":
            case "interview_prep":
                return 9;
            case "scenario_presentation":
                return 10;
            case "scenario_meeting":
                return 8;
            case "scenario_negotiation":
                return 8;
            case "scenario_email":
                return 6;
            case "scenario_debate":
                return 10;
            case "scenario_networking":
                return 7;
            default:
                return 10;
        }
    }

    // 시나리오별 첫 인사 메시지
    private String getScenarioGreeting(String scenarioId) {
        if (scenarioId == null) {
            return "Hi! Let's practice English together. How are you today?";
        }

        switch (scenarioId) {
            case "scenario_daily":
            case "daily_conversation":
            case "daily":
                return "Hi! Let's have a casual chat. How was your day today?";

            case "scenario_travel":
            case "travel_english":
            case "travel":
                return "Welcome! I'm here to help you practice travel English. Are you planning a trip soon?";

            case "scenario_shopping":
                return "Hello! Welcome to our store. Can I help you find something today?";

            case "scenario_restaurant":
                return "Good evening! Welcome to our restaurant. Would you like to see the menu?";

            case "scenario_business":
                return "Good morning. Let's practice some business English. What would you like to discuss today?";

            case "scenario_hotel":
                return "Welcome to our hotel! Do you have a reservation, or would you like to book a room?";

            case "scenario_airport":
                return "Hello! Welcome to the airport. May I see your passport and boarding pass?";

            case "scenario_medical":
                return "Hello, I'm the doctor. What brings you in today? How are you feeling?";

            case "scenario_phone":
                return "Hello, this is the customer service line. How may I help you today?";

            case "scenario_job_interview":
            case "interview_prep":
            case "interview":
                return "Hello, thank you for coming in today. Please have a seat. Can you tell me a little about yourself?";

            case "scenario_presentation":
                return "Let's practice your presentation skills. What topic would you like to present on?";

            case "scenario_meeting":
                return "Good morning everyone. Let's start our meeting. What's on the agenda today?";

            case "scenario_negotiation":
                return "Thank you for meeting with me today. Shall we discuss the terms of our agreement?";

            case "scenario_email":
                return "Let's practice writing professional emails. What kind of email do you need to write?";

            case "scenario_debate":
                return "Welcome to our discussion session. What topic would you like to debate today?";

            case "scenario_networking":
                return "Hi there! Nice to meet you. So, what brings you to this event?";

            default:
                return "Hi! Let's practice English together. What would you like to talk about?";
        }
    }

    // 오프라인 모드용 기본 응답
    private String getOfflineResponse(String userText) {
        String lowerText = userText.toLowerCase();
        if (lowerText.contains("hello") || lowerText.contains("hi")) {
            return "Hello! Nice to meet you. (Offline mode - please check internet connection)";
        } else if (lowerText.contains("how are you")) {
            return "I'm doing well, thank you! (Offline mode)";
        } else if (lowerText.contains("bye") || lowerText.contains("goodbye")) {
            return "Goodbye! Have a great day! (Offline mode)";
        } else {
            return "I understand. Can you tell me more? (Offline mode - AI not connected)";
        }
    }

    // 대화 종료 처리
    private void handleConversationEnd() {
        // 대화 종료 후 피드백 설정인 경우
        if (feedbackTiming == 1 && !userMessagesForFeedback.isEmpty()) {
            showEndOfConversationFeedback();
        } else {
            finish();
        }
    }

    // 대화 종료 후 전체 피드백 표시
    private void showEndOfConversationFeedback() {
        if (geminiService == null || !geminiService.isInitialized()) {
            Toast.makeText(this, "AI 연결 오류로 피드백을 제공할 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 모든 사용자 메시지를 하나로 합침
        StringBuilder allMessages = new StringBuilder();
        for (int i = 0; i < userMessagesForFeedback.size(); i++) {
            allMessages.append((i + 1)).append(". ").append(userMessagesForFeedback.get(i)).append("\n");
        }

        String feedbackPrompt = "You are an English tutor. A Korean student just completed an English conversation. " +
                "Analyze their messages below and provide detailed feedback IN KOREAN.\n\n" +
                "Student's messages:\n" + allMessages.toString() + "\n\n" +
                "Please provide feedback in this EXACT format:\n\n" +
                "📊 전체 평가\n[한 줄로 전체적인 평가]\n\n" +
                "✅ 장점\n" +
                "• [구체적인 장점 1]\n" +
                "• [구체적인 장점 2]\n\n" +
                "⚠️ 개선이 필요한 점 (약점 목록)\n" +
                "• [문법 오류]: \"틀린 문장\" → \"올바른 문장\"\n" +
                "• [어휘 문제]: 설명\n" +
                "(각 약점을 요약하지 말고, 하나씩 구체적으로 나열해주세요)\n\n" +
                "🎯 다음 학습 팁\n[한 줄 조언]\n\n" +
                "IMPORTANT: List each weakness individually with specific examples. Be encouraging but honest. Use Korean.";

        // 로딩 표시
        cardFeedback.setVisibility(View.VISIBLE);
        tvFeedback.setText("📊 대화 분석 중...");

        geminiService.generateText(feedbackPrompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String feedback) {
                runOnUiThread(() -> {
                    // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                    if (isFinishing() || isDestroyed()) return;
                    tvFeedback.setText("📊 대화 종료 피드백\n\n" + feedback);

                    // 확인 버튼으로 변경
                    btnCloseFeedback.setOnClickListener(v -> {
                        cardFeedback.setVisibility(View.GONE);
                        finish();
                    });
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                    if (isFinishing() || isDestroyed()) return;
                    android.util.Log.e("Feedback", "End conversation feedback error: " + error);
                    Toast.makeText(ConversationActivityWithAI.this,
                            "피드백 생성 실패: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    // 즉시 피드백: 현재 메시지에 대한 문법 및 어휘 분석 (매 턴마다 새로 시작)
    private String currentTurnGrammarFeedback = null;
    private String currentTurnVocabFeedback = null;

    private void requestImmediateFeedback(String userText) {
        if (geminiService == null || !geminiService.isInitialized()) {
            android.util.Log.w("Feedback", "Cannot provide grammar feedback - AI not available");
            return;
        }

        // 새 턴 시작: 이전 피드백 초기화
        currentTurnGrammarFeedback = null;
        currentTurnVocabFeedback = null;

        // 피드백 카드 초기화 (이전 턴의 피드백 제거)
        runOnUiThread(() -> {
            tvFeedback.setText("분석 중...");
            cardFeedback.setVisibility(View.VISIBLE);
        });

        // 문법 분석
        geminiService.analyzeGrammar(userText, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String grammarFeedback) {
                currentTurnGrammarFeedback = grammarFeedback;
                updateCurrentTurnFeedback();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("Feedback", "Grammar analysis error: " + error);
                currentTurnGrammarFeedback = ""; // 에러 시 빈 문자열
                updateCurrentTurnFeedback();
            }
        });

        // 어휘 제안
        geminiService.getVocabularySuggestions(userText, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String vocabFeedback) {
                currentTurnVocabFeedback = vocabFeedback;
                updateCurrentTurnFeedback();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("Feedback", "Vocabulary suggestion error: " + error);
                currentTurnVocabFeedback = ""; // 에러 시 빈 문자열
                updateCurrentTurnFeedback();
            }
        });
    }

    // 현재 턴의 피드백 업데이트 (문법/어휘 둘 다 준비되면 표시)
    private void updateCurrentTurnFeedback() {
        runOnUiThread(() -> {
            // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
            if (isFinishing() || isDestroyed()) return;

            // 둘 다 아직 null이면 아직 분석 중
            if (currentTurnGrammarFeedback == null && currentTurnVocabFeedback == null) {
                return;
            }

            StringBuilder feedback = new StringBuilder();

            // 문법 피드백
            if (currentTurnGrammarFeedback != null && !currentTurnGrammarFeedback.isEmpty()) {
                feedback.append("📝 문법: ").append(currentTurnGrammarFeedback);
            }

            // 어휘 피드백
            if (currentTurnVocabFeedback != null && !currentTurnVocabFeedback.isEmpty()) {
                if (feedback.length() > 0) {
                    feedback.append("\n\n");
                }
                feedback.append("📚 어휘: ").append(currentTurnVocabFeedback);
            }

            // 피드백이 있으면 표시
            if (feedback.length() > 0) {
                tvFeedback.setText(feedback.toString());
                cardFeedback.setVisibility(View.VISIBLE);
            } else if (currentTurnGrammarFeedback != null && currentTurnVocabFeedback != null) {
                // 둘 다 완료됐는데 내용이 없으면 기본 메시지
                tvFeedback.setText("✅ 문법과 어휘가 적절합니다!");
                cardFeedback.setVisibility(View.VISIBLE);
            }
        });
    }

    private PronunciationFeedback createBasicFeedback(float confidence) {
        PronunciationFeedback feedback = new PronunciationFeedback();
        float score = confidence * 100;
        feedback.setAccuracyScore(score);
        feedback.setFluencyScore(score * 0.9f);
        feedback.setCompletenessScore(score * 0.95f);

        if (score >= 80) {
            feedback.setSuggestion("훌륭합니다! 👍");
        } else if (score >= 60) {
            feedback.setSuggestion("좋아요! 조금 더 명확하게 발음해보세요.");
        } else {
            feedback.setSuggestion("천천히 또박또박 말해보세요.");
        }

        return feedback;
    }

    private void showFeedback(PronunciationFeedback feedback) {
        cardFeedback.setVisibility(View.VISIBLE);
        String feedbackText = String.format("발음 점수: %.0f/100\n%s",
                feedback.getOverallScore(),
                feedback.getSuggestion());

        if (feedback.getProblematicWords() != null && !feedback.getProblematicWords().isEmpty()) {
            feedbackText += "\n\n연습 필요: " + feedback.getProblematicWords();
        }

        tvFeedback.setText(feedbackText);
    }

    private void showPronunciationFeedback(PronunciationFeedback feedback) {
        cardFeedback.setVisibility(View.VISIBLE);

        StringBuilder feedbackText = new StringBuilder();
        feedbackText.append(String.format("🎯 발음 평가\n\n"));
        feedbackText.append(String.format("정확도: %.0f/100\n", feedback.getAccuracyScore()));
        feedbackText.append(String.format("유창성: %.0f/100\n", feedback.getFluencyScore()));
        feedbackText.append(String.format("완성도: %.0f/100\n", feedback.getCompletenessScore()));

        if (feedback.getProsodyScore() > 0) {
            feedbackText.append(String.format("억양: %.0f/100\n", feedback.getProsodyScore()));
        }

        feedbackText.append(String.format("\n전체 점수: %.0f/100\n\n", feedback.getOverallScore()));
        feedbackText.append(feedback.getSuggestion());

        if (feedback.getWordDetails() != null && !feedback.getWordDetails().isEmpty()) {
            feedbackText.append("\n\n").append(feedback.getWordDetails());
        }

        tvFeedback.setText(feedbackText.toString());
    }

    private void getAIResponse(String userText) {
        if (geminiService != null && geminiService.isInitialized()) {
            geminiService.sendMessage(userText, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String aiResponse) {
                    runOnUiThread(() -> {
                        // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                        if (isFinishing() || isDestroyed()) return;
                        addAIMessage(aiResponse);
                        speakMessage(aiResponse);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // Activity 종료 중이면 UI 업데이트 건너뜀 (crash 방지)
                        if (isFinishing() || isDestroyed()) return;
                        android.util.Log.e("AI", "getAIResponse error: " + error);
                        Toast.makeText(ConversationActivityWithAI.this,
                                error, Toast.LENGTH_LONG).show();
                        String fallbackResponse = "I see. Could you tell me more about that?";
                        addAIMessage(fallbackResponse);
                        speakMessage(fallbackResponse);
                    });
                }
            });
        } else {
            // 오프라인 모드
            String offlineResponse = getOfflineResponse(userText);
            addAIMessage(offlineResponse);
            speakMessage(offlineResponse);
        }
    }

    private void addUserMessage(String text) {
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID().toString(),
                currentScenario.getId(),
                "user",
                text);
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAIMessage(String text) {
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID().toString(),
                currentScenario.getId(),
                "ai",
                text);
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    // 초기화 되지 않은 상태라면, TTS 준비 x
    private void speakMessage(String text) {
        if (ttsService != null && ttsService.isInitialized()) {
            android.util.Log.d("TTS", "Speaking: " + text);
            ttsService.speak(text);
        } else {
            android.util.Log.e("TTS", "Cannot speak - TTS not initialized");
            Toast.makeText(this, "TTS가 아직 준비되지 않았습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 타이머 정리
        stopLessonTimer();

        if (basicSpeechService != null) {
            // Listener 정리하여 Activity 참조 해제 (메모리 누수 방지)
            basicSpeechService.setListener(null);
            basicSpeechService.destroy();
        }
        if (geminiService != null) {
            geminiService.shutdown();
        }
        if (ttsService != null) {
            ttsService.shutdown();
        }
    }
}
