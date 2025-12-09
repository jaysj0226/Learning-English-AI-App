package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.adapter.ConversationAdapter;
import com.cookandroid.justspeakapp.data.UserDataManager;
import com.cookandroid.justspeakapp.model.ConversationMessage;
import com.cookandroid.justspeakapp.service.GeminiService;
import com.cookandroid.justspeakapp.service.TextToSpeechService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LevelTestConversationActivity extends AppCompatActivity implements TextToSpeechService.OnInitListener {
    private static final String TAG = "LevelTestConversation";
    private static final int MAX_EXCHANGES = 5; // 5번의 대화 교환 후 평가

    private RecyclerView rvChatMessages;
    private EditText etMessageInput;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private TextView tvProgress, tvLoadingMessage;
    private FrameLayout loadingOverlay;

    private GeminiService geminiService;
    private TextToSpeechService ttsService;
    private ConversationAdapter adapter;
    private List<ConversationMessage> messages;
    private SharedPreferences prefs;
    private UserDataManager userDataManager;

    private int exchangeCount = 0;
    private List<String> userResponses = new ArrayList<>();
    private List<String> aiResponses = new ArrayList<>();
    private boolean isWaitingForAI = false;
    private String userInterests = "";
    private String learningGoal = "";
    private static final String SCENARIO_ID = "level_test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_test_conversation);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);

        initViews();
        initServices();
        setupListeners();
        loadUserDataAndStart();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        loadingOverlay = findViewById(R.id.loading_overlay);
        tvLoadingMessage = findViewById(R.id.tv_loading_message);

        messages = new ArrayList<>();
        adapter = new ConversationAdapter(messages);
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvChatMessages.setAdapter(adapter);
    }

    private void initServices() {
        try {
            geminiService = new GeminiService(this, BuildConfig.GEMINI_API_KEY);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini service", e);
            Toast.makeText(this, "AI 서비스 초기화 실패", Toast.LENGTH_SHORT).show();
            finish();
        }

        try {
            ttsService = new TextToSpeechService(this, this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TTS service", e);
        }
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadUserDataAndStart() {
        showLoading("사용자 정보를 불러오는 중...");

        userDataManager.getUserData(new UserDataManager.DataCallback() {
            @Override
            public void onSuccess(java.util.Map<String, Object> data) {
                userInterests = (String) data.get("interests");
                learningGoal = (String) data.get("learning_goal");

                if (userInterests == null) userInterests = "general";
                if (learningGoal == null) learningGoal = "speaking";

                runOnUiThread(() -> {
                    hideLoading();
                    startConversation();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading user data: " + error);
                userInterests = "general";
                learningGoal = "speaking";
                runOnUiThread(() -> {
                    hideLoading();
                    startConversation();
                });
            }
        });
    }

    private void startConversation() {
        // 첫 AI 메시지 생성 (시스템 프롬프트를 포함)
        String firstPrompt = "You are a friendly English level assessment assistant. " +
                "Your task is to evaluate a Korean student's English proficiency through natural conversation. " +
                "User's interests: " + userInterests + ". Learning goal: " + learningGoal + ".\n\n" +
                "Guidelines:\n" +
                "1. Start with a warm greeting and an easy question\n" +
                "2. Gradually increase question difficulty based on responses\n" +
                "3. Ask questions that encourage longer responses\n" +
                "4. Be encouraging but note grammar/vocabulary issues internally\n" +
                "5. Keep responses short and conversational (2-3 sentences max)\n" +
                "6. After 5 exchanges, you'll be asked to provide an assessment\n\n" +
                "Start the level assessment conversation. Greet the user warmly and ask them a simple opening question about themselves or their day. Keep it friendly and casual. Speak only in English.";

        isWaitingForAI = true;
        setInputEnabled(false);

        geminiService.generateText(firstPrompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    addAIMessage(response);
                    aiResponses.add(response);
                    isWaitingForAI = false;
                    setInputEnabled(true);
                    updateProgress();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error starting conversation: " + error);
                runOnUiThread(() -> {
                    String defaultGreeting = "Hello! I'm here to help assess your English level. Let's have a friendly chat! Could you tell me a little about yourself?";
                    addAIMessage(defaultGreeting);
                    aiResponses.add(defaultGreeting);
                    isWaitingForAI = false;
                    setInputEnabled(true);
                    updateProgress();
                });
            }
        });
    }

    private void sendMessage() {
        String userText = etMessageInput.getText().toString().trim();
        if (userText.isEmpty() || isWaitingForAI) return;

        // 사용자 메시지 추가
        addUserMessage(userText);
        userResponses.add(userText);
        etMessageInput.setText("");
        exchangeCount++;

        updateProgress();

        // 대화 교환 완료 체크
        if (exchangeCount >= MAX_EXCHANGES) {
            evaluateLevel();
            return;
        }

        // AI 응답 요청
        isWaitingForAI = true;
        setInputEnabled(false);

        // 대화 히스토리를 포함한 프롬프트 구성
        StringBuilder conversationHistory = new StringBuilder();
        conversationHistory.append("You are a friendly English level assessment assistant evaluating a Korean student. ");
        conversationHistory.append("Keep responses short (2-3 sentences). Speak only in English.\n\n");
        conversationHistory.append("Conversation history:\n");

        for (int i = 0; i < aiResponses.size(); i++) {
            conversationHistory.append("You: ").append(aiResponses.get(i)).append("\n");
            if (i < userResponses.size()) {
                conversationHistory.append("Student: ").append(userResponses.get(i)).append("\n");
            }
        }

        conversationHistory.append("\nThis is exchange ").append(exchangeCount).append(" of ").append(MAX_EXCHANGES).append(". ");

        if (exchangeCount <= 2) {
            conversationHistory.append("Ask a slightly more challenging question based on their interests.");
        } else if (exchangeCount <= 4) {
            conversationHistory.append("Ask a question that requires more complex English (past tense, conditionals, opinions).");
        } else {
            conversationHistory.append("Ask one final question that tests their ability to express complex ideas.");
        }

        conversationHistory.append("\n\nRespond naturally as the assessment assistant:");

        geminiService.generateText(conversationHistory.toString(), new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    addAIMessage(response);
                    aiResponses.add(response);
                    isWaitingForAI = false;
                    setInputEnabled(true);

                    // TTS로 읽기
                    if (ttsService != null) {
                        ttsService.speak(response);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "AI response error: " + error);
                runOnUiThread(() -> {
                    isWaitingForAI = false;
                    setInputEnabled(true);
                    Toast.makeText(LevelTestConversationActivity.this,
                            "응답 오류. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void evaluateLevel() {
        showLoading("AI가 영어 실력을 분석 중입니다...");
        setInputEnabled(false);

        StringBuilder allResponses = new StringBuilder();
        for (int i = 0; i < userResponses.size(); i++) {
            allResponses.append((i + 1)).append(". ").append(userResponses.get(i)).append("\n");
        }

        String evaluationPrompt = "Based on the conversation, evaluate the user's English level.\n\n" +
                "User's responses:\n" + allResponses.toString() + "\n\n" +
                "Analyze these 4 aspects:\n" +
                "1. Grammar accuracy (subject-verb agreement, tenses, articles, prepositions)\n" +
                "2. Vocabulary range (basic, intermediate, advanced words)\n" +
                "3. Sentence complexity (simple vs compound vs complex sentences, variety of structures)\n" +
                "4. Communication effectiveness (clarity, coherence, ability to convey meaning)\n\n" +
                "Return your assessment in this EXACT JSON format (no markdown, no code blocks):\n" +
                "{\n" +
                "  \"level\": \"Beginner\" or \"Pre-Intermediate\" or \"Intermediate\" or \"Advanced\",\n" +
                "  \"score\": 0-100,\n" +
                "  \"grammar_score\": 0-100,\n" +
                "  \"vocabulary_score\": 0-100,\n" +
                "  \"complexity_score\": 0-100,\n" +
                "  \"communication_score\": 0-100,\n" +
                "  \"strengths\": [\"strength1\", \"strength2\"],\n" +
                "  \"improvements\": [\"improvement1\", \"improvement2\"],\n" +
                "  \"feedback\": \"Brief encouraging feedback in Korean (2-3 sentences)\"\n" +
                "}\n\n" +
                "Be encouraging but honest. The score should reflect: Beginner (0-40), Pre-Intermediate (41-60), Intermediate (61-80), Advanced (81-100).";

        geminiService.generateText(evaluationPrompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> parseEvaluationAndNavigate(response));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Evaluation error: " + error);
                runOnUiThread(() -> {
                    hideLoading();
                    // 기본 평가로 진행
                    navigateToResult("Pre-Intermediate", 50,
                            "문법과 어휘를 꾸준히 연습하면 빠르게 성장할 수 있습니다!");
                });
            }
        });
    }

    private void parseEvaluationAndNavigate(String jsonResponse) {
        try {
            // JSON 파싱
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
            }

            org.json.JSONObject json = new org.json.JSONObject(cleanJson);

            String level = json.getString("level");
            int score = json.getInt("score");
            int grammarScore = json.optInt("grammar_score", score);
            int vocabScore = json.optInt("vocabulary_score", score);
            int complexityScore = json.optInt("complexity_score", score);
            int communicationScore = json.optInt("communication_score", score);
            String feedback = json.optString("feedback", "열심히 노력하시는 모습이 보입니다!");

            // 점수 배열
            org.json.JSONArray strengthsArray = json.optJSONArray("strengths");
            org.json.JSONArray improvementsArray = json.optJSONArray("improvements");

            ArrayList<String> strengths = new ArrayList<>();
            ArrayList<String> improvements = new ArrayList<>();

            if (strengthsArray != null) {
                for (int i = 0; i < strengthsArray.length(); i++) {
                    strengths.add(strengthsArray.getString(i));
                }
            }
            if (improvementsArray != null) {
                for (int i = 0; i < improvementsArray.length(); i++) {
                    improvements.add(improvementsArray.getString(i));
                }
            }

            // 결과 저장 및 이동
            saveResultAndNavigate(level, score, grammarScore, vocabScore, complexityScore,
                    communicationScore, feedback, strengths, improvements);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing evaluation JSON", e);
            hideLoading();
            navigateToResult("Pre-Intermediate", 50,
                    "대화를 분석한 결과, 기초적인 영어 실력을 갖추고 계십니다. 꾸준한 연습으로 더 성장할 수 있습니다!");
        }
    }

    private void saveResultAndNavigate(String level, int score, int grammarScore,
                                        int vocabScore, int complexityScore, int communicationScore,
                                        String feedback, ArrayList<String> strengths, ArrayList<String> improvements) {
        // SharedPreferences에 저장
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_level", level);
        editor.putInt("test_score", score);
        editor.putInt("grammar_score", grammarScore);
        editor.putInt("vocabulary_score", vocabScore);
        editor.putInt("complexity_score", complexityScore);
        editor.putInt("communication_score", communicationScore);
        editor.putBoolean("onboarding_completed", true);
        editor.apply();

        // Firestore에도 저장
        userDataManager.saveUserLevel(level, new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Level saved to Firestore: " + level);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to save level to Firestore: " + error);
            }
        });

        hideLoading();

        // 결과 화면으로 이동
        Intent intent = new Intent(this, LevelTestResultActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("score", score);
        intent.putExtra("grammar_score", grammarScore);
        intent.putExtra("vocabulary_score", vocabScore);
        intent.putExtra("complexity_score", complexityScore);
        intent.putExtra("communication_score", communicationScore);
        intent.putExtra("feedback", feedback);
        intent.putStringArrayListExtra("strengths", strengths);
        intent.putStringArrayListExtra("improvements", improvements);
        intent.putExtra("is_conversation_test", true);
        startActivity(intent);
        finish();
    }

    private void navigateToResult(String level, int score, String feedback) {
        ArrayList<String> emptyList = new ArrayList<>();
        saveResultAndNavigate(level, score, score, score, score, score, feedback, emptyList, emptyList);
    }

    private void addUserMessage(String text) {
        String messageId = UUID.randomUUID().toString();
        ConversationMessage message = new ConversationMessage(messageId, SCENARIO_ID, "user", text);
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAIMessage(String text) {
        String messageId = UUID.randomUUID().toString();
        ConversationMessage message = new ConversationMessage(messageId, SCENARIO_ID, "ai", text);
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    // TextToSpeechService.OnInitListener implementation
    @Override
    public void onInit(boolean success) {
        if (!success) {
            Log.w(TAG, "TTS initialization failed");
        }
    }

    private void updateProgress() {
        int progress = (exchangeCount * 100) / MAX_EXCHANGES;
        progressBar.setProgress(progress);
        tvProgress.setText("대화 진행: " + exchangeCount + "/" + MAX_EXCHANGES);
    }

    private void setInputEnabled(boolean enabled) {
        etMessageInput.setEnabled(enabled);
        btnSend.setEnabled(enabled);
        btnSend.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void showLoading(String message) {
        loadingOverlay.setVisibility(View.VISIBLE);
        tvLoadingMessage.setText(message);
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        // 테스트 중 뒤로가기 방지
        Toast.makeText(this, "테스트를 완료해주세요", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geminiService != null) {
            geminiService.shutdown();
        }
        if (ttsService != null) {
            ttsService.shutdown();
        }
    }
}
