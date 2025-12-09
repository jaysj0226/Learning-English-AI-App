package com.cookandroid.justspeakapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cookandroid.justspeakapp.data.UserDataManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.cookandroid.justspeakapp.model.LearningProgress;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView tvProgress;
    private ProgressBar progressBar;
    private Button btnStartLearning;
    private ImageButton btnSettings;
    private CardView cardDailyConversation;
    private CardView cardTravelEnglish;
    private CardView cardInterviewPrep;
    private CardView cardBusiness;
    private Button btnMoreScenarios;
    private BottomNavigationView bottomNavigation;

    private LearningProgress learningProgress;
    private SharedPreferences sharedPreferences;
    private UserDataManager userDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userDataManager = new UserDataManager(this);

        // 레벨 테스트 완료 여부 확인
        checkLevelTestCompletion();
    }

    private void checkLevelTestCompletion() {
        userDataManager.getUserData(new UserDataManager.DataCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                Boolean levelTestCompleted = (Boolean) data.get("level_test_completed");

                if (levelTestCompleted == null || !levelTestCompleted) {
                    // 레벨 테스트를 완료하지 않았으면 LevelTestActivity로 이동
                    Log.d(TAG, "Level test not completed, redirecting to LevelTestActivity");
                    Toast.makeText(MainActivity.this,
                            "학습을 시작하려면 먼저 레벨 테스트를 완료해주세요",
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(MainActivity.this, LevelTestActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // 레벨 테스트를 완료했으면 정상적으로 MainActivity 초기화
                    Log.d(TAG, "Level test completed, initializing MainActivity");
                    initializeMainActivity();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking level test completion: " + error);
                // 에러가 발생해도 일단 MainActivity는 보여줌
                initializeMainActivity();
            }
        });
    }

    private void initializeMainActivity() {
        initViews();
        loadProgress();
        updateProgressUI();
        setupListeners();

        // 클라우드에서 음성 설정 동기화
        syncCloudSettings();
    }

    private void syncCloudSettings() {
        Log.d(TAG, "syncCloudSettings() called");
        userDataManager.syncVoiceSettingsFromCloud(new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Cloud settings synced successfully");
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to sync cloud settings: " + error);
            }
        });
        Log.d(TAG, "syncCloudSettings() finished calling userDataManager");
    }

    private void initViews() {
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        btnStartLearning = findViewById(R.id.btn_start_learning);
        btnSettings = findViewById(R.id.btn_settings);
        cardDailyConversation = findViewById(R.id.card_daily_conversation);
        cardTravelEnglish = findViewById(R.id.card_travel_english);
        cardInterviewPrep = findViewById(R.id.card_interview_prep);
        cardBusiness = findViewById(R.id.card_business);
        btnMoreScenarios = findViewById(R.id.btn_more_scenarios);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void loadProgress() {
        sharedPreferences = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        learningProgress = new LearningProgress("default_user");

        // 날짜 변경 체크 및 초기화
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = dateFormat.format(new java.util.Date());

        // UserDataManager를 통해 사용자별 진도 가져오기
        Map<String, Object> dailyProgress = userDataManager.getDailyProgress();
        int dailyCompleted = (int) dailyProgress.get("daily_completed");
        int dailyGoal = (int) dailyProgress.get("daily_goal");
        String lastLearningDate = (String) dailyProgress.get("last_learning_date");

        // 날짜가 바뀌면 daily_completed 초기화 (단, 이전 학습 기록이 있는 경우에만)
        if (lastLearningDate != null && !lastLearningDate.isEmpty() && !today.equals(lastLearningDate)) {
            dailyCompleted = 0;
            userDataManager.resetDailyProgress(today);
            Log.d(TAG, "New day detected, reset daily progress for user");
        }

        learningProgress.setDailyCompleted(dailyCompleted);
        learningProgress.setDailyGoal(dailyGoal);
    }

    private void updateProgressUI() {
        int completed = learningProgress.getDailyCompleted();
        int goal = learningProgress.getDailyGoal();

        tvProgress.setText(completed + "/" + goal);
        progressBar.setMax(goal);
        progressBar.setProgress(completed);
    }

    private void setupListeners() {
        btnStartLearning.setOnClickListener(v -> {
            startConversation("scenario_daily", "일상 대화");
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        cardDailyConversation.setOnClickListener(v -> {
            startConversation("scenario_daily", "일상 대화");
        });

        cardTravelEnglish.setOnClickListener(v -> {
            startConversation("scenario_travel", "여행 영어");
        });

        cardInterviewPrep.setOnClickListener(v -> {
            startConversation("scenario_job_interview", "면접");
        });

        cardBusiness.setOnClickListener(v -> {
            startConversation("scenario_business", "비즈니스 영어");
        });

        btnMoreScenarios.setOnClickListener(v -> {
            startActivity(new Intent(this, ScenariosActivity.class));
        });

        // 개발자 테스트: 설정 버튼 길게 누르면 네트워크 테스트 실행
        btnSettings.setOnLongClickListener(v -> {
            startActivity(new Intent(this, NetworkTestActivity.class));
            return true;
        });

        // 하단 네비게이션 바 리스너
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 이미 홈 화면이므로 아무 작업 안 함
                return true;
            } else if (itemId == R.id.nav_progress) {
                // 진도 화면으로 이동
                startActivity(new Intent(this, ProgressActivity.class));
                return true;
            } else if (itemId == R.id.nav_schedule) {
                // 일정 화면으로 이동
                startActivity(new Intent(this, ScheduleActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }

            return false;
        });

        // 홈 메뉴를 선택된 상태로 설정
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void startConversation(String scenarioId, String scenarioTitle) {
        Intent intent = new Intent(this, ConversationActivityWithAI.class);
        intent.putExtra("scenario_id", scenarioId);
        intent.putExtra("scenario_title", scenarioTitle);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProgress();
        updateProgressUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 진도는 UserDataManager를 통해 저장되므로 별도 저장 불필요
    }
}