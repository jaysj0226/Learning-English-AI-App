package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;
import com.cookandroid.justspeakapp.service.GeminiService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelTestQuizActivity extends AppCompatActivity {
    private static final String TAG = "LevelTestQuizActivity";

    private TextView tvQuestion, tvQuestionNumber;
    private RadioGroup rgAnswers;
    private RadioButton rbOption1, rbOption2, rbOption3, rbOption4;
    private Button btnNext;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private GeminiService geminiService;
    private UserDataManager userDataManager;

    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private boolean questionsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_test_quiz);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);
        questions = new ArrayList<>();

        // GeminiService 초기화 - 실패해도 앱이 크래시되지 않도록 처리
        try {
            geminiService = new GeminiService(this, BuildConfig.GEMINI_API_KEY);
            if (!geminiService.isInitialized()) {
                Log.w(TAG, "Gemini service not fully initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini service", e);
            geminiService = null;
        }

        initViews();
        setupListeners();
        loadUserDataAndGenerateQuestions();
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tv_question);
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        rgAnswers = findViewById(R.id.rg_answers);
        rbOption1 = findViewById(R.id.rb_option1);
        rbOption2 = findViewById(R.id.rb_option2);
        rbOption3 = findViewById(R.id.rb_option3);
        rbOption4 = findViewById(R.id.rb_option4);
        btnNext = findViewById(R.id.btn_next);
        progressBar = findViewById(R.id.progress_bar);

        // 초기 상태: 문제 로딩 중
        showLoading(true);
    }

    private void loadUserDataAndGenerateQuestions() {
        // Firestore에서 사용자 데이터 가져오기
        userDataManager.getUserData(new UserDataManager.DataCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                String interests = (String) data.get("interests");
                String learningGoal = (String) data.get("learning_goal");

                // 기본값 설정
                if (interests == null || interests.isEmpty()) {
                    interests = "general English";
                }
                if (learningGoal == null || learningGoal.isEmpty()) {
                    learningGoal = "speaking";
                }

                loadQuestionsFromGemini(interests, learningGoal);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading user data: " + error);
                // 오류 시 기본값으로 진행
                loadQuestionsFromGemini("general English", "speaking");
            }
        });
    }

    private void loadQuestionsFromGemini(String interests, String learningGoal) {
        // AI 서비스가 없으면 바로 기본 문제 로드
        if (geminiService == null || !geminiService.isInitialized()) {
            Log.w(TAG, "Gemini service not available, loading default questions");
            runOnUiThread(() -> {
                loadDefaultQuestions();
                questionsLoaded = true;
                showLoading(false);
                displayQuestion();
                Toast.makeText(this, "AI 연결 실패. 기본 문제로 진행합니다.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        String prompt = String.format(
                "You are an English level test generator. Generate 10 multiple-choice questions to assess English proficiency from Beginner to Advanced level.\n\n"
                        +
                        "User's interests: %s\n" +
                        "Learning goal: %s\n\n" +
                        "Requirements:\n" +
                        "1. Questions should gradually increase in difficulty (2 Beginner, 3 Pre-Intermediate, 3 Intermediate, 2 Advanced)\n"
                        +
                        "2. Include grammar, vocabulary, and comprehension questions\n" +
                        "3. Each question should have 4 options\n" +
                        "4. Questions should be related to the user's interests when possible\n\n" +
                        "Return ONLY a valid JSON array in this exact format (no markdown, no code blocks):\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"question\": \"question text with ___ for blank\",\n" +
                        "    \"options\": [\"option1\", \"option2\", \"option3\", \"option4\"],\n" +
                        "    \"correctAnswer\": 0\n" +
                        "  }\n" +
                        "]\n\n" +
                        "correctAnswer is the index (0-3) of the correct option.",
                interests, learningGoal);

        geminiService.generateText(prompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        parseQuestionsFromJson(response);
                        questionsLoaded = true;
                        showLoading(false);
                        displayQuestion();
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse questions JSON", e);
                        // Fallback to default questions
                        loadDefaultQuestions();
                        questionsLoaded = true;
                        showLoading(false);
                        displayQuestion();
                        Toast.makeText(LevelTestQuizActivity.this,
                                "AI 문제 생성 실패. 기본 문제로 진행합니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini API error: " + error);
                runOnUiThread(() -> {
                    // Fallback to default questions
                    loadDefaultQuestions();
                    questionsLoaded = true;
                    showLoading(false);
                    displayQuestion();
                    Toast.makeText(LevelTestQuizActivity.this,
                            "AI 연결 실패. 기본 문제로 진행합니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void parseQuestionsFromJson(String jsonResponse) throws JSONException {
        // Remove markdown code blocks if present
        String cleanJson = jsonResponse.trim();
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
        }

        JSONArray jsonArray = new JSONArray(cleanJson);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject questionObj = jsonArray.getJSONObject(i);

            String questionText = questionObj.getString("question");
            JSONArray optionsArray = questionObj.getJSONArray("options");
            int correctAnswer = questionObj.getInt("correctAnswer");

            String[] options = new String[optionsArray.length()];
            for (int j = 0; j < optionsArray.length(); j++) {
                options[j] = optionsArray.getString(j);
            }

            questions.add(new Question(questionText, options, correctAnswer));
        }

        Log.d(TAG, "Successfully loaded " + questions.size() + " questions from Gemini");
    }

    private void loadDefaultQuestions() {
        questions.clear();

        // 기본 레벨 테스트 문제들 (10문제) - 난이도 균형 조정
        // Beginner (2문제) - 기초 문법
        questions.add(new Question(
                "She ___ to the gym three times a week.",
                new String[] { "go", "goes", "going", "gone" },
                1 // goes
        ));

        questions.add(new Question(
                "We ___ dinner when the phone rang.",
                new String[] { "have", "had", "were having", "are having" },
                2 // were having
        ));

        // Pre-Intermediate (3문제) - 중간 난이도
        questions.add(new Question(
                "I've been waiting here ___ two hours.",
                new String[] { "since", "for", "during", "while" },
                1 // for
        ));

        questions.add(new Question(
                "He suggested ___ a break before continuing.",
                new String[] { "to take", "taking", "take", "took" },
                1 // taking
        ));

        questions.add(new Question(
                "The movie was ___ boring that I fell asleep.",
                new String[] { "such", "too", "so", "very" },
                2 // so
        ));

        // Intermediate (3문제) - 중급
        questions.add(new Question(
                "If I had known about the meeting, I ___ earlier.",
                new String[] { "would come", "would have come", "will come", "had come" },
                1 // would have come
        ));

        questions.add(new Question(
                "The project ___ by the time the manager arrives.",
                new String[] { "will complete", "will be completed", "will have been completed", "is completed" },
                2 // will have been completed
        ));

        questions.add(new Question(
                "She denied ___ anything about the missing documents.",
                new String[] { "to know", "knowing", "know", "knew" },
                1 // knowing
        ));

        // Advanced (2문제) - 고급
        questions.add(new Question(
                "Not until I arrived home ___ I had left my wallet at work.",
                new String[] { "I realized", "did I realize", "I did realize", "realized I" },
                1 // did I realize
        ));

        questions.add(new Question(
                "Had the company ___ the new regulations, they wouldn't have faced legal issues.",
                new String[] { "implemented", "been implementing", "implement", "to implement" },
                0 // implemented
        ));

        Log.d(TAG, "Loaded " + questions.size() + " default questions with balanced difficulty");
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rgAnswers.setVisibility(View.GONE);
            btnNext.setEnabled(false);
            tvQuestion.setText("AI가 맞춤형 레벨 테스트를 생성하고 있습니다...");
            tvQuestionNumber.setText("잠시만 기다려주세요");
        } else {
            progressBar.setVisibility(View.GONE);
            rgAnswers.setVisibility(View.VISIBLE);
            btnNext.setEnabled(true);
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questions.size()) {
            Question question = questions.get(currentQuestionIndex);

            tvQuestionNumber.setText("질문 " + (currentQuestionIndex + 1) + " / " + questions.size());
            tvQuestion.setText(question.getQuestion());

            String[] options = question.getOptions();
            rbOption1.setText(options[0]);
            rbOption2.setText(options[1]);
            rbOption3.setText(options[2]);
            rbOption4.setText(options[3]);

            rgAnswers.clearCheck();

            if (currentQuestionIndex == questions.size() - 1) {
                btnNext.setText("완료");
            }
        }
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> {
            int selectedId = rgAnswers.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "답을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 정답 확인
            int selectedAnswer = -1;
            if (selectedId == R.id.rb_option1)
                selectedAnswer = 0;
            else if (selectedId == R.id.rb_option2)
                selectedAnswer = 1;
            else if (selectedId == R.id.rb_option3)
                selectedAnswer = 2;
            else if (selectedId == R.id.rb_option4)
                selectedAnswer = 3;

            if (selectedAnswer == questions.get(currentQuestionIndex).getCorrectAnswer()) {
                correctAnswers++;
            }

            currentQuestionIndex++;

            if (currentQuestionIndex < questions.size()) {
                displayQuestion();
            } else {
                completeTest();
            }
        });
    }

    private void completeTest() {
        // 점수 계산 (10문제 중 몇 개 맞았는지)
        int totalQuestions = questions.size();
        double score = (correctAnswers * 100.0) / totalQuestions;

        // 레벨 결정 - 더 엄격한 기준 적용
        String level;
        if (score >= 90) {
            level = "Advanced"; // 9-10개 정답
        } else if (score >= 70) {
            level = "Intermediate"; // 7-8개 정답
        } else if (score >= 50) {
            level = "Pre-Intermediate"; // 5-6개 정답
        } else {
            level = "Beginner"; // 0-4개 정답
        }

        // 결과 저장
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_level", level);
        editor.putInt("test_score", (int) score);
        editor.putInt("test_correct_answers", correctAnswers);
        editor.putBoolean("onboarding_completed", true);
        editor.apply();

        // 결과 화면으로 이동
        Intent intent = new Intent(this, LevelTestResultActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("score", (int) score);
        intent.putExtra("correct_answers", correctAnswers);
        intent.putExtra("total_questions", totalQuestions);
        startActivity(intent);
        finish();
    }

    // Question 클래스
    private static class Question {
        private final String question;
        private final String[] options;
        private final int correctAnswer;

        public Question(String question, String[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }

        public String getQuestion() {
            return question;
        }

        public String[] getOptions() {
            return options;
        }

        public int getCorrectAnswer() {
            return correctAnswer;
        }
    }
}
