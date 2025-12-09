package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.adapter.ScenarioProgressAdapter;
import com.cookandroid.justspeakapp.data.UserDataManager;
import com.cookandroid.justspeakapp.model.ScenarioProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScenariosActivity extends AppCompatActivity {
    private static final String TAG = "ScenariosActivity";

    private ImageButton btnBack;
    private RecyclerView rvScenarios;
    private UserDataManager userDataManager;
    private ScenarioProgressAdapter scenarioAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenarios);

        userDataManager = new UserDataManager(this);

        initViews();
        setupListeners();
        loadScenarios();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rvScenarios = findViewById(R.id.rv_scenarios);

        rvScenarios.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadScenarios() {
        userDataManager.getUserData(new UserDataManager.DataCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    displayScenarios(data);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading scenarios: " + error);
                // Show default scenarios even if error
                runOnUiThread(() -> {
                    displayScenarios(null);
                });
            }
        });
    }

    private void displayScenarios(Map<String, Object> data) {
        String level = data != null ? (String) data.get("level") : "Beginner";
        Map<String, Map<String, Integer>> savedProgress = userDataManager.getAllScenarioProgress();

        List<ScenarioProgress> scenarios = new ArrayList<>();

        // 모든 시나리오 추가
        addScenarioWithProgress(scenarios, savedProgress, "scenario_daily", "일상 대화", 10);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_travel", "여행 영어", 8);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_shopping", "쇼핑", 6);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_restaurant", "레스토랑", 7);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_business", "비즈니스 영어", 12);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_hotel", "호텔", 5);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_airport", "공항", 6);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_medical", "병원/약국", 5);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_phone", "전화 영어", 7);
        addScenarioWithProgress(scenarios, savedProgress, "scenario_job_interview", "면접", 9);

        // 중급 이상 시나리오
        if ("Intermediate".equals(level) || "Advanced".equals(level)) {
            addScenarioWithProgress(scenarios, savedProgress, "scenario_presentation", "프레젠테이션", 10);
            addScenarioWithProgress(scenarios, savedProgress, "scenario_meeting", "회의", 8);
            addScenarioWithProgress(scenarios, savedProgress, "scenario_negotiation", "협상", 8);
            addScenarioWithProgress(scenarios, savedProgress, "scenario_email", "이메일 작성", 6);
        }

        // 고급 시나리오
        if ("Advanced".equals(level)) {
            addScenarioWithProgress(scenarios, savedProgress, "scenario_debate", "토론", 10);
            addScenarioWithProgress(scenarios, savedProgress, "scenario_networking", "네트워킹", 7);
        }

        if (scenarioAdapter == null) {
            scenarioAdapter = new ScenarioProgressAdapter(scenarios);
            scenarioAdapter.setOnScenarioClickListener(this::startScenarioConversation);
            rvScenarios.setAdapter(scenarioAdapter);
        } else {
            scenarioAdapter.updateData(scenarios);
        }
    }

    private void startScenarioConversation(com.cookandroid.justspeakapp.model.ScenarioProgress scenario) {
        Intent intent = new Intent(this, ConversationActivityWithAI.class);
        intent.putExtra("scenario_id", scenario.getScenarioId());
        intent.putExtra("scenario_title", scenario.getScenarioName());
        startActivity(intent);
    }

    private void addScenarioWithProgress(List<ScenarioProgress> scenarios,
                                        Map<String, Map<String, Integer>> savedProgress,
                                        String scenarioId, String scenarioName, int defaultTotal) {
        int completed = 0;
        int total = defaultTotal;

        if (savedProgress.containsKey(scenarioId)) {
            Map<String, Integer> progress = savedProgress.get(scenarioId);
            if (progress != null) {
                completed = progress.get("completed") != null ? progress.get("completed") : 0;
                total = progress.get("total") != null ? progress.get("total") : defaultTotal;
            }
        }

        scenarios.add(new ScenarioProgress(scenarioId, scenarioName, completed, total));
    }
}
