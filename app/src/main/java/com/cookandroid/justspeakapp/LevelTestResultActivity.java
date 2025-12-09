package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;

import java.util.ArrayList;

public class LevelTestResultActivity extends AppCompatActivity {
    private static final String TAG = "LevelTestResult";

    private TextView tvLevel, tvScore, tvMessage;
    private TextView tvGrammarScore, tvVocabScore, tvComplexityScore, tvCommunicationScore;
    private TextView tvStrengths, tvImprovements;
    private LinearLayout detailScoresContainer, strengthsContainer, improvementsContainer;
    private Button btnStartLearning;
    private UserDataManager userDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_test_result);

        userDataManager = new UserDataManager(this);

        initViews();
        displayResult();
        setupListeners();
        saveLevelTestResult();
    }

    private void initViews() {
        tvLevel = findViewById(R.id.tv_level);
        tvScore = findViewById(R.id.tv_score);
        tvMessage = findViewById(R.id.tv_message);
        btnStartLearning = findViewById(R.id.btn_start_learning);

        // Conversation test specific views
        detailScoresContainer = findViewById(R.id.detail_scores_container);
        tvGrammarScore = findViewById(R.id.tv_grammar_score);
        tvVocabScore = findViewById(R.id.tv_vocab_score);
        tvComplexityScore = findViewById(R.id.tv_complexity_score);
        tvCommunicationScore = findViewById(R.id.tv_communication_score);
        strengthsContainer = findViewById(R.id.strengths_container);
        tvStrengths = findViewById(R.id.tv_strengths);
        improvementsContainer = findViewById(R.id.improvements_container);
        tvImprovements = findViewById(R.id.tv_improvements);
    }

    private void displayResult() {
        boolean isConversationTest = getIntent().getBooleanExtra("is_conversation_test", false);
        String level = getIntent().getStringExtra("level");
        int score = getIntent().getIntExtra("score", 0);

        tvLevel.setText(level);

        if (isConversationTest) {
            displayConversationTestResult(level, score);
        } else {
            displayQuizTestResult(level, score);
        }
    }

    private void displayConversationTestResult(String level, int score) {
        // Display overall score
        tvScore.setText("종합 점수: " + score + "점");

        // Get detailed scores
        int grammarScore = getIntent().getIntExtra("grammar_score", 0);
        int vocabScore = getIntent().getIntExtra("vocabulary_score", 0);
        int complexityScore = getIntent().getIntExtra("complexity_score", 0);
        int communicationScore = getIntent().getIntExtra("communication_score", 0);
        String feedback = getIntent().getStringExtra("feedback");
        ArrayList<String> strengths = getIntent().getStringArrayListExtra("strengths");
        ArrayList<String> improvements = getIntent().getStringArrayListExtra("improvements");

        // Show detail scores container
        if (detailScoresContainer != null) {
            detailScoresContainer.setVisibility(View.VISIBLE);
            if (tvGrammarScore != null) tvGrammarScore.setText(String.valueOf(grammarScore));
            if (tvVocabScore != null) tvVocabScore.setText(String.valueOf(vocabScore));
            if (tvComplexityScore != null) tvComplexityScore.setText(String.valueOf(complexityScore));
            if (tvCommunicationScore != null) tvCommunicationScore.setText(String.valueOf(communicationScore));
        }

        // Display AI feedback
        if (feedback != null && !feedback.isEmpty()) {
            tvMessage.setText(feedback);
        } else {
            // Default message based on level
            tvMessage.setText(getDefaultMessage(level));
        }

        // Display strengths
        if (strengths != null && !strengths.isEmpty() && strengthsContainer != null) {
            strengthsContainer.setVisibility(View.VISIBLE);
            StringBuilder strengthsText = new StringBuilder();
            for (String strength : strengths) {
                strengthsText.append("• ").append(strength).append("\n");
            }
            tvStrengths.setText(strengthsText.toString().trim());
        }

        // Display improvements
        if (improvements != null && !improvements.isEmpty() && improvementsContainer != null) {
            improvementsContainer.setVisibility(View.VISIBLE);
            StringBuilder improvementsText = new StringBuilder();
            for (String improvement : improvements) {
                improvementsText.append("• ").append(improvement).append("\n");
            }
            tvImprovements.setText(improvementsText.toString().trim());
        }
    }

    private void displayQuizTestResult(String level, int score) {
        int correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        int totalQuestions = getIntent().getIntExtra("total_questions", 10);

        tvScore.setText(correctAnswers + " / " + totalQuestions + " 정답");
        tvMessage.setText(getDefaultMessage(level));
    }

    private String getDefaultMessage(String level) {
        switch (level) {
            case "Advanced":
                return "훌륭합니다! 고급 수준의 영어 실력을 가지고 계시네요.\n더욱 심화된 표현과 비즈니스 영어를 학습할 수 있습니다.";
            case "Intermediate":
                return "좋아요! 중급 수준의 영어 실력을 가지고 계시네요.\n다양한 상황에서 활용할 수 있는 표현을 배워보세요.";
            case "Pre-Intermediate":
                return "잘하고 계세요! 기초를 잘 다지고 계시네요.\n일상 회화와 기본 문법을 더 연습하면 좋겠어요.";
            default: // Beginner
                return "좋은 시작입니다! 기초부터 차근차근 시작해봐요.\n매일 조금씩 연습하면 실력이 쑥쑥 늘어날 거예요.";
        }
    }

    private void setupListeners() {
        btnStartLearning.setOnClickListener(v -> {
            // 메인 화면으로 이동
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveLevelTestResult() {
        boolean isConversationTest = getIntent().getBooleanExtra("is_conversation_test", false);
        String level = getIntent().getStringExtra("level");
        int score = getIntent().getIntExtra("score", 0);

        if (isConversationTest) {
            // Save conversation test result with detailed scores
            int grammarScore = getIntent().getIntExtra("grammar_score", 0);
            int vocabScore = getIntent().getIntExtra("vocabulary_score", 0);
            int complexityScore = getIntent().getIntExtra("complexity_score", 0);
            int communicationScore = getIntent().getIntExtra("communication_score", 0);

            userDataManager.saveLevelTestResult(level, score, grammarScore, vocabScore, complexityScore, communicationScore,
                    new UserDataManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Conversation test result saved to Firestore successfully");
                            saveOnboardingCompleted();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error saving level test result: " + error);
                            // Fallback: try saving with basic data
                            saveBasicLevelTestResult(level, score);
                        }
                    });
        } else {
            // Save quiz test result
            int correctAnswers = getIntent().getIntExtra("correct_answers", 0);
            int totalQuestions = getIntent().getIntExtra("total_questions", 10);
            saveBasicLevelTestResult(level, score, correctAnswers, totalQuestions);
        }
    }

    private void saveBasicLevelTestResult(String level, int score) {
        saveBasicLevelTestResult(level, score, 0, 0);
    }

    private void saveBasicLevelTestResult(String level, int score, int correctAnswers, int totalQuestions) {
        userDataManager.saveLevelTestResult(level, score, correctAnswers, totalQuestions,
                new UserDataManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Level test result saved to Firestore successfully");
                        saveOnboardingCompleted();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error saving level test result: " + error);
                        Toast.makeText(LevelTestResultActivity.this,
                                "결과 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveOnboardingCompleted() {
        userDataManager.setOnboardingCompleted(true,
                new UserDataManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Onboarding completed status saved");
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error saving onboarding status: " + error);
                    }
                });
    }
}
