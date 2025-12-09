package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;
import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.Set;

public class LearningGoalActivity extends AppCompatActivity {
    private static final String TAG = "LearningGoal";

    private Button btnContinue;
    private SharedPreferences prefs;
    private UserDataManager userDataManager;
    private MaterialCardView cardSpeaking, cardPronunciation, cardListening, cardGrammar, cardVocabulary;

    // 복수 선택을 위한 Set
    private Set<String> selectedGoals = new HashSet<>();
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_goal);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);

        // 편집 모드 확인
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);

        initViews();
        setupListeners();

        // 편집 모드일 경우 기존 선택 항목 로드
        if (isEditMode) {
            loadExistingGoals();
            btnContinue.setText("저장");
        }
    }

    private void loadExistingGoals() {
        String existingGoals = prefs.getString("learning_goal", null);
        if (existingGoals != null && !existingGoals.isEmpty()) {
            String[] goals = existingGoals.split(",");
            for (String goal : goals) {
                String trimmed = goal.trim();
                selectedGoals.add(trimmed);
                highlightCard(trimmed);
            }
        }
    }

    private void highlightCard(String goal) {
        MaterialCardView card = null;
        switch (goal.toLowerCase()) {
            case "speaking": card = cardSpeaking; break;
            case "pronunciation": card = cardPronunciation; break;
            case "listening": card = cardListening; break;
            case "grammar": card = cardGrammar; break;
            case "vocabulary": card = cardVocabulary; break;
        }
        if (card != null) {
            card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
            card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            card.setStrokeWidth(4);
        }
    }

    private void initViews() {
        btnContinue = findViewById(R.id.btn_continue);
        cardSpeaking = findViewById(R.id.card_speaking);
        cardPronunciation = findViewById(R.id.card_pronunciation);
        cardListening = findViewById(R.id.card_listening);
        cardGrammar = findViewById(R.id.card_grammar);
        cardVocabulary = findViewById(R.id.card_vocabulary);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        setupGoalCard(cardSpeaking, "speaking");
        setupGoalCard(cardPronunciation, "pronunciation");
        setupGoalCard(cardListening, "listening");
        setupGoalCard(cardGrammar, "grammar");
        setupGoalCard(cardVocabulary, "vocabulary");

        btnContinue.setOnClickListener(v -> {
            if (selectedGoals.isEmpty()) {
                Toast.makeText(this, "학습 목표를 하나 이상 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 선택된 목표들을 쉼표로 연결
            String goalsString = String.join(",", selectedGoals);
            Log.d(TAG, "Selected goals: " + goalsString);

            // Firebase Firestore에 저장
            userDataManager.saveLearningGoal(goalsString,
                    new UserDataManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Learning goals saved to Firestore");

                            if (isEditMode) {
                                // 편집 모드: 저장 후 종료
                                Toast.makeText(LearningGoalActivity.this, "학습 목표가 저장되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // 온보딩 모드: 레벨 테스트 화면으로 이동
                                startActivity(new Intent(LearningGoalActivity.this, LevelTestActivity.class));
                                finish();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error saving learning goals: " + error);
                            Toast.makeText(LearningGoalActivity.this,
                                    "학습 목표 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setupGoalCard(MaterialCardView card, String goal) {
        if (card == null) {
            Log.e(TAG, "Card is null for goal: " + goal);
            return;
        }

        card.setOnClickListener(v -> {
            try {
                if (selectedGoals.contains(goal)) {
                    // 이미 선택된 경우 -> 선택 해제
                    selectedGoals.remove(goal);
                    card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    card.setStrokeWidth(0);
                } else {
                    // 선택되지 않은 경우 -> 선택
                    selectedGoals.add(goal);
                    card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
                    card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
                    card.setStrokeWidth(4);
                }

                Log.d(TAG, "Current selections: " + selectedGoals);
            } catch (Exception e) {
                Log.e(TAG, "Error toggling card state", e);
            }
        });
    }
}
