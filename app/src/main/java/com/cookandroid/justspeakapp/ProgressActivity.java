package com.cookandroid.justspeakapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.adapter.ScenarioProgressAdapter;
import com.cookandroid.justspeakapp.data.UserDataManager;
import com.cookandroid.justspeakapp.model.ScenarioProgress;
import com.cookandroid.justspeakapp.service.GeminiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {
    private static final String TAG = "ProgressActivity";
    private static final String PREFS_NAME = "daily_quote_prefs";
    private static final String KEY_QUOTE_DATE = "quote_date";
    private static final String KEY_QUOTE_ENGLISH = "quote_english";
    private static final String KEY_QUOTE_KOREAN = "quote_korean";

    private ImageButton btnBack;
    private TextView tvTotalProgress, tvQuoteEnglish, tvQuoteKorean;
    private ProgressBar progressTotal;
    private RecyclerView rvScenarioProgress;
    private UserDataManager userDataManager;
    private ScenarioProgressAdapter scenarioAdapter;
    private GeminiService geminiService;
    private SharedPreferences quotePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        userDataManager = new UserDataManager(this);
        quotePrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Gemini 서비스 초기화
        try {
            String apiKey = BuildConfig.GEMINI_API_KEY;
            if (apiKey != null && !apiKey.isEmpty()) {
                geminiService = new GeminiService(this, apiKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini", e);
        }

        initViews();
        setupListeners();
        loadProgressData();
        loadDailyQuote();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTotalProgress = findViewById(R.id.tv_total_progress);
        tvQuoteEnglish = findViewById(R.id.tv_quote_english);
        tvQuoteKorean = findViewById(R.id.tv_quote_korean);
        progressTotal = findViewById(R.id.progress_total);
        rvScenarioProgress = findViewById(R.id.rv_scenario_progress);

        rvScenarioProgress.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadProgressData() {
        userDataManager.getUserData(new UserDataManager.DataCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                runOnUiThread(() -> {
                    displayProgressData(data);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading progress data: " + error);
            }
        });
    }

    private void displayProgressData(Map<String, Object> data) {
        // 전체 진도 계산 - 모든 시나리오의 진도 합산
        Map<String, Map<String, Integer>> savedProgress = userDataManager.getAllScenarioProgress();

        int totalCompletedLessons = 0;
        int totalLessons = 0;

        for (Map.Entry<String, Map<String, Integer>> entry : savedProgress.entrySet()) {
            Map<String, Integer> progress = entry.getValue();
            if (progress != null) {
                totalCompletedLessons += progress.get("completed") != null ? progress.get("completed") : 0;
                totalLessons += progress.get("total") != null ? progress.get("total") : 0;
            }
        }

        // 저장된 진도가 없으면 기본값 사용
        if (totalLessons == 0) {
            totalLessons = 100; // 기본 총 레슨 수
        }

        tvTotalProgress.setText(totalCompletedLessons + " / " + totalLessons);
        int progress = (int) ((totalCompletedLessons / (float) totalLessons) * 100);
        progressTotal.setProgress(progress);

        // 시나리오별 진도
        List<ScenarioProgress> scenarios = createScenarioList(data);

        if (scenarioAdapter == null) {
            scenarioAdapter = new ScenarioProgressAdapter(scenarios);
            rvScenarioProgress.setAdapter(scenarioAdapter);
        } else {
            scenarioAdapter.updateData(scenarios);
        }
    }

    /**
     * 오늘의 동기부여 문구 로드
     * 하루마다 새로운 문구 생성, 모든 유저에게 동일하게 적용
     */
    private void loadDailyQuote() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = quotePrefs.getString(KEY_QUOTE_DATE, "");

        if (today.equals(savedDate)) {
            // 오늘 이미 생성된 문구가 있으면 사용
            String englishQuote = quotePrefs.getString(KEY_QUOTE_ENGLISH, "");
            String koreanQuote = quotePrefs.getString(KEY_QUOTE_KOREAN, "");

            if (!englishQuote.isEmpty() && !koreanQuote.isEmpty()) {
                tvQuoteEnglish.setText("\"" + englishQuote + "\"");
                tvQuoteKorean.setText(koreanQuote);
                return;
            }
        }

        // 새로운 문구 생성
        generateDailyQuote(today);
    }

    /**
     * Gemini를 사용하여 새로운 동기부여 문구 생성
     */
    private void generateDailyQuote(String today) {
        if (geminiService == null || !geminiService.isInitialized()) {
            // Gemini 사용 불가 시 기본 문구 표시
            showDefaultQuote();
            return;
        }

        String prompt = "Generate a short, inspiring motivational quote about learning English or language learning. " +
                "The quote should be encouraging and suitable for someone studying English as a second language. " +
                "Format your response EXACTLY like this:\n" +
                "ENGLISH: [the English quote here]\n" +
                "KOREAN: [the Korean translation here]\n" +
                "Keep the quote under 30 words. Do not include author names or attribution.";

        geminiService.sendMessage(prompt, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    parseAndDisplayQuote(response, today);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error generating quote: " + error);
                runOnUiThread(() -> showDefaultQuote());
            }
        });
    }

    /**
     * Gemini 응답 파싱 및 표시
     */
    private void parseAndDisplayQuote(String response, String today) {
        try {
            String englishQuote = "";
            String koreanQuote = "";

            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.toUpperCase().startsWith("ENGLISH:")) {
                    englishQuote = line.substring(8).trim();
                } else if (line.toUpperCase().startsWith("KOREAN:")) {
                    koreanQuote = line.substring(7).trim();
                }
            }

            if (!englishQuote.isEmpty() && !koreanQuote.isEmpty()) {
                // 저장
                quotePrefs.edit()
                        .putString(KEY_QUOTE_DATE, today)
                        .putString(KEY_QUOTE_ENGLISH, englishQuote)
                        .putString(KEY_QUOTE_KOREAN, koreanQuote)
                        .apply();

                tvQuoteEnglish.setText("\"" + englishQuote + "\"");
                tvQuoteKorean.setText(koreanQuote);
            } else {
                showDefaultQuote();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing quote", e);
            showDefaultQuote();
        }
    }

    /**
     * 기본 동기부여 문구 (Gemini 사용 불가 시)
     */
    private void showDefaultQuote() {
        // 날짜 기반으로 기본 문구 선택 (매일 다른 문구)
        String[][] defaultQuotes = {
                {"Every word you learn is a step closer to fluency.", "배우는 모든 단어가 유창함에 한 걸음 더 가까워지는 것입니다."},
                {"The best time to start learning was yesterday. The second best time is now.", "배움을 시작하기 가장 좋은 때는 어제였습니다. 두 번째로 좋은 때는 바로 지금입니다."},
                {"Small progress is still progress. Keep going!", "작은 발전도 발전입니다. 계속 나아가세요!"},
                {"Your accent is beautiful - it shows you speak more than one language.", "당신의 억양은 아름답습니다 - 그것은 당신이 하나 이상의 언어를 구사한다는 것을 보여줍니다."},
                {"Mistakes are proof that you are trying.", "실수는 당신이 노력하고 있다는 증거입니다."},
                {"Learning a language opens doors to new worlds.", "언어를 배우는 것은 새로운 세계로 가는 문을 엽니다."},
                {"Consistency beats intensity. A little every day goes a long way.", "꾸준함이 강도를 이깁니다. 매일 조금씩이 큰 변화를 만듭니다."}
        };

        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        int index = dayOfYear % defaultQuotes.length;

        tvQuoteEnglish.setText("\"" + defaultQuotes[index][0] + "\"");
        tvQuoteKorean.setText(defaultQuotes[index][1]);
    }

    private List<ScenarioProgress> createScenarioList(Map<String, Object> data) {
        List<ScenarioProgress> scenarios = new ArrayList<>();

        // 레벨에 따라 추천 시나리오 생성
        String level = (String) data.get("level");
        if (level == null) level = "Beginner";

        // 저장된 진도 데이터 가져오기
        Map<String, Map<String, Integer>> savedProgress = userDataManager.getAllScenarioProgress();

        // 모든 기본 시나리오들 (ScenariosActivity와 동일)
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

        return scenarios;
    }

    private void addScenarioWithProgress(List<ScenarioProgress> scenarios,
                                        Map<String, Map<String, Integer>> savedProgress,
                                        String scenarioId, String scenarioName, int defaultTotal) {
        int completed = 0;
        int total = defaultTotal;

        // 저장된 진도가 있으면 사용
        if (savedProgress.containsKey(scenarioId)) {
            Map<String, Integer> progress = savedProgress.get(scenarioId);
            if (progress != null) {
                completed = progress.get("completed") != null ? progress.get("completed") : 0;
                total = progress.get("total") != null ? progress.get("total") : defaultTotal;
            }
        }

        scenarios.add(new ScenarioProgress(scenarioId, scenarioName, completed, total));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geminiService != null) {
            geminiService.shutdown();
        }
    }
}
