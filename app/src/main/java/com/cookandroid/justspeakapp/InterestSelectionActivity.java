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

import java.util.ArrayList;
import java.util.List;

public class InterestSelectionActivity extends AppCompatActivity {
    private static final String TAG = "InterestSelection";

    private List<String> selectedInterests;
    private SharedPreferences prefs;
    private UserDataManager userDataManager;
    private Button btnContinue;
    private MaterialCardView cardAcademic, cardDaily, cardTravel, cardInterview, cardSelfImprovement, cardBusiness;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interest_selection);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);
        selectedInterests = new ArrayList<>();

        // 편집 모드 확인
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);

        initViews();
        setupInterestCards();
        setupListeners();

        // 편집 모드일 경우 기존 선택 항목 로드
        if (isEditMode) {
            loadExistingInterests();
            btnContinue.setText("저장");
        }
    }

    private void loadExistingInterests() {
        String existingInterests = prefs.getString("interests", null);
        if (existingInterests != null && !existingInterests.isEmpty()) {
            String[] interests = existingInterests.split(",");
            for (String interest : interests) {
                String trimmed = interest.trim();
                selectedInterests.add(trimmed);
                highlightCard(trimmed);
            }
        }
    }

    private void highlightCard(String interest) {
        MaterialCardView card = null;
        switch (interest.toLowerCase()) {
            case "academic": card = cardAcademic; break;
            case "daily": card = cardDaily; break;
            case "travel": card = cardTravel; break;
            case "interview": card = cardInterview; break;
            case "self_improvement": card = cardSelfImprovement; break;
            case "business": card = cardBusiness; break;
        }
        if (card != null) {
            card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
            card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            card.setStrokeWidth(4);
        }
    }

    private void initViews() {
        btnContinue = findViewById(R.id.btn_continue);
        cardAcademic = findViewById(R.id.card_academic);
        cardDaily = findViewById(R.id.card_daily);
        cardTravel = findViewById(R.id.card_travel);
        cardInterview = findViewById(R.id.card_interview);
        cardSelfImprovement = findViewById(R.id.card_self_improvement);
        cardBusiness = findViewById(R.id.card_business);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> {
            if (selectedInterests.isEmpty()) {
                Toast.makeText(this, "최소 1개 이상의 관심사를 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            String interestsString = String.join(",", selectedInterests);

            // Firebase Firestore에 저장
            userDataManager.saveUserInterests(interestsString,
                    new UserDataManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "User interests saved to Firestore");

                            if (isEditMode) {
                                // 편집 모드: 저장 후 종료
                                Toast.makeText(InterestSelectionActivity.this, "관심사가 저장되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // 온보딩 모드: 학습 목표 선택 화면으로 이동
                                startActivity(new Intent(InterestSelectionActivity.this, LearningGoalActivity.class));
                                finish();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error saving interests: " + error);
                            Toast.makeText(InterestSelectionActivity.this,
                                    "관심사 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void setupInterestCards() {
        setupCard(cardAcademic, "academic");
        setupCard(cardDaily, "daily");
        setupCard(cardTravel, "travel");
        setupCard(cardInterview, "interview");
        setupCard(cardSelfImprovement, "self_improvement");
        setupCard(cardBusiness, "business");
    }

    private void setupCard(MaterialCardView card, String interest) {
        if (card == null) {
            Log.e(TAG, "Card is null for interest: " + interest);
            return;
        }

        card.setOnClickListener(v -> {
            try {
                if (selectedInterests.contains(interest)) {
                    selectedInterests.remove(interest);
                    card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
                    card.setStrokeWidth(0);
                } else {
                    selectedInterests.add(interest);
                    card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_light));
                    card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
                    card.setStrokeWidth(4);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting card state", e);
            }
        });
    }
}
