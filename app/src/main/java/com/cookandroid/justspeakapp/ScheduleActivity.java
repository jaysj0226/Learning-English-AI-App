package com.cookandroid.justspeakapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cookandroid.justspeakapp.data.UserDataManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ScheduleActivity extends AppCompatActivity {
    private static final String TAG = "ScheduleActivity";

    private ImageButton btnBack, btnPrevMonth, btnNextMonth;
    private TextView tvMonthYear, tvTotalDays, tvStreakDays;
    private GridLayout calendarGrid;
    private SharedPreferences prefs;
    private UserDataManager userDataManager;

    private Calendar currentCalendar;
    private Set<String> learningDays; // "yyyy-MM-dd" 형식

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);
        currentCalendar = Calendar.getInstance();

        loadLearningDays();
        initViews();
        setupListeners();
        updateCalendar();
        updateStats();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        tvMonthYear = findViewById(R.id.tv_month_year);
        tvTotalDays = findViewById(R.id.tv_total_days);
        tvStreakDays = findViewById(R.id.tv_streak_days);
        calendarGrid = findViewById(R.id.calendar_grid);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
    }

    private void loadLearningDays() {
        // UserDataManager를 통해 사용자별 학습일 가져오기
        learningDays = userDataManager.getLearningDays();
    }

    private void updateCalendar() {
        // 월/년 표시
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("yyyy년 M월", Locale.KOREAN);
        tvMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));

        // 캘린더 그리드 초기화
        calendarGrid.removeAllViews();

        // 해당 월의 1일 가져오기
        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1 = 일요일
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 빈 공간 추가 (월의 시작 요일까지)
        for (int i = 1; i < firstDayOfWeek; i++) {
            addEmptyDay();
        }

        // 날짜 추가
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            String dateString = dateFormat.format(calendar.getTime());
            boolean isLearned = learningDays.contains(dateString);
            addDay(day, isLearned);
        }
    }

    private void addEmptyDay() {
        TextView dayView = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        dayView.setLayoutParams(params);
        calendarGrid.addView(dayView);
    }

    private void addDay(int day, boolean isLearned) {
        TextView dayView = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(4, 4, 4, 4);

        dayView.setLayoutParams(params);
        dayView.setText(String.valueOf(day));
        dayView.setTextSize(14);
        dayView.setGravity(Gravity.CENTER);

        if (isLearned) {
            // 학습 완료한 날
            dayView.setBackgroundResource(R.drawable.bg_day_studied);
            dayView.setTextColor(Color.WHITE);
        } else {
            // 학습 안 한 날
            dayView.setBackgroundResource(R.drawable.bg_day_normal);
            dayView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        calendarGrid.addView(dayView);
    }

    private void updateStats() {
        // 총 학습일
        tvTotalDays.setText(String.valueOf(learningDays.size()));

        // 연속 학습일 계산
        int streakDays = calculateStreakDays();
        tvStreakDays.setText(String.valueOf(streakDays));
    }

    private int calculateStreakDays() {
        if (learningDays.isEmpty()) {
            return 0;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        int streak = 0;

        // 오늘부터 역순으로 체크
        while (true) {
            String dateString = dateFormat.format(calendar.getTime());
            if (learningDays.contains(dateString)) {
                streak++;
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return streak;
    }

}
