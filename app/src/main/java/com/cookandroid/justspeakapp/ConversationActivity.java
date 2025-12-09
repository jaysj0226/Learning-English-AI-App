package com.cookandroid.justspeakapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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
import com.cookandroid.justspeakapp.service.SpeechRecognitionService;
import com.cookandroid.justspeakapp.service.TextToSpeechService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ConversationActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvChatMessages;
    private FloatingActionButton fabMic;
    private TextView tvListeningStatus;
    private MaterialCardView cardFeedback;
    private TextView tvFeedback;
    private ImageButton btnBack, btnStop, btnCloseFeedback;

    private SpeechRecognitionService speechRecognitionService;
    private TextToSpeechService ttsService;
    private ConversationAdapter adapter;
    private List<ConversationMessage> messages;

    private Scenario currentScenario;
    private boolean isListening = false;
    private int conversationTurn = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        initViews();
        checkPermissions();
        initServices();
        initRecyclerView();
        setupListeners();
        startConversation();
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
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
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
        // 기본 음성 인식 서비스
        speechRecognitionService = new SpeechRecognitionService(this);
        speechRecognitionService.setListener(new SpeechRecognitionService.SpeechRecognitionListener() {
            @Override
            public void onSpeechResult(String text, float confidence) {
                handleUserSpeech(text, confidence);
            }

            @Override
            public void onSpeechError(String error) {
                runOnUiThread(() -> {
                    tvListeningStatus.setVisibility(View.GONE);
                    isListening = false;
                    Toast.makeText(ConversationActivity.this, "오류: " + error, Toast.LENGTH_SHORT).show();
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

        ttsService = new TextToSpeechService(this, new TextToSpeechService.OnInitListener() {
            @Override
            public void onInit(boolean success) {
                if (success) {
                    Toast.makeText(ConversationActivity.this, "TTS 준비 완료", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ConversationActivity.this, "TTS 초기화 실패", Toast.LENGTH_SHORT).show();
                }
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
        btnBack.setOnClickListener(v -> finish());

        btnStop.setOnClickListener(v -> {
            if (speechRecognitionService != null) {
                speechRecognitionService.stopListening();
            }
            finish();
        });

        fabMic.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });

        btnCloseFeedback.setOnClickListener(v -> cardFeedback.setVisibility(View.GONE));
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
            "daily"
        );

        String greeting = getGreetingForScenario(currentScenario.getCategory());
        addAIMessage(greeting);
        speakMessage(greeting);
    }

    private String getGreetingForScenario(String category) {
        switch (category) {
            case "travel":
                return "Hello! Welcome to our hotel. How can I help you today?";
            case "interview":
                return "Good morning! Thank you for coming. Please tell me about yourself.";
            case "business":
                return "Good afternoon! Let's discuss the project details.";
            default:
                return "Hi! How are you doing today?";
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            isListening = true;
            speechRecognitionService.startListening();
        } else {
            Toast.makeText(this, "음성 인식 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            checkPermissions();
        }
    }

    private void stopListening() {
        isListening = false;
        if (speechRecognitionService != null) {
            speechRecognitionService.stopListening();
        }
    }

    private void handleUserSpeech(String text, float confidence) {
        runOnUiThread(() -> {
            addUserMessage(text);

            PronunciationFeedback feedback = analyzePronunciation(text, confidence);
            showFeedback(feedback);

            conversationTurn++;

            new android.os.Handler().postDelayed(() -> {
                String aiResponse = generateAIResponse(text);
                addAIMessage(aiResponse);
                speakMessage(aiResponse);
            }, 1000);
        });
    }

    private PronunciationFeedback analyzePronunciation(String text, float confidence) {
        PronunciationFeedback feedback = new PronunciationFeedback();

        float accuracyScore = confidence * 100;
        feedback.setAccuracyScore(accuracyScore);
        feedback.setFluencyScore(75 + new Random().nextInt(20));
        feedback.setCompletenessScore(80 + new Random().nextInt(15));

        if (accuracyScore < 70) {
            feedback.setSuggestion("발음을 천천히 더 명확하게 해보세요.");
        } else if (accuracyScore < 85) {
            feedback.setSuggestion("좋아요! 조금만 더 연습하면 완벽해요.");
        } else {
            feedback.setSuggestion("훌륭합니다! 계속 이 조자로 연습하세요.");
        }

        return feedback;
    }

    private void showFeedback(PronunciationFeedback feedback) {
        cardFeedback.setVisibility(View.VISIBLE);
        String feedbackText = String.format("발음 점수: %.0f/100 - %s",
            feedback.getOverallScore(),
            feedback.getSuggestion());
        tvFeedback.setText(feedbackText);
    }

    private String generateAIResponse(String userInput) {
        String[] responses = {
            "That's interesting! Tell me more.",
            "I see. What do you think about that?",
            "Great! Can you give me an example?",
            "That makes sense. How did that make you feel?",
            "Wonderful! What happened next?",
            "I understand. Would you like to talk about something else?"
        };

        return responses[conversationTurn % responses.length];
    }

    private void addUserMessage(String text) {
        ConversationMessage message = new ConversationMessage(
            UUID.randomUUID().toString(),
            currentScenario.getId(),
            "user",
            text
        );
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void addAIMessage(String text) {
        ConversationMessage message = new ConversationMessage(
            UUID.randomUUID().toString(),
            currentScenario.getId(),
            "ai",
            text
        );
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChatMessages.smoothScrollToPosition(messages.size() - 1);
    }

    private void speakMessage(String text) {
        if (ttsService != null && ttsService.isInitialized()) {
            ttsService.speak(text);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognitionService != null) {
            speechRecognitionService.destroy();
        }
        if (ttsService != null) {
            ttsService.shutdown();
        }
    }
}
