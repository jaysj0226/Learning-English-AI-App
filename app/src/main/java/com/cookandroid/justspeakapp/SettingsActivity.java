package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {
    private ImageButton btnBack;
    private MaterialCardView cardProfile, cardAiSettings, cardFeedbackSettings;
    private MaterialCardView cardTheme, cardAccount, cardContact, cardLogout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);

        // 저장된 테마 설정 적용
        int savedTheme = prefs.getInt("theme", 2);
        applyTheme(savedTheme);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        cardProfile = findViewById(R.id.card_profile);
        cardAiSettings = findViewById(R.id.card_ai_settings);
        cardFeedbackSettings = findViewById(R.id.card_feedback_settings);
        cardTheme = findViewById(R.id.card_theme);
        cardAccount = findViewById(R.id.card_account);
        cardContact = findViewById(R.id.card_contact);
        cardLogout = findViewById(R.id.card_logout);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileSettingsActivity.class);
            startActivity(intent);
        });

        cardAiSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoiceSettingsActivity.class);
            startActivity(intent);
        });

        cardFeedbackSettings.setOnClickListener(v -> {
            showFeedbackSettingsDialog();
        });

        cardTheme.setOnClickListener(v -> {
            showThemeDialog();
        });

        cardAccount.setOnClickListener(v -> {
            // 계정 설정 화면으로 이동
            Intent intent = new Intent(this, AccountSettingsActivity.class);
            startActivity(intent);
        });

        cardContact.setOnClickListener(v -> {
            Toast.makeText(this, "문의하기: samuel62b3221@gmail.com", Toast.LENGTH_LONG).show();
        });

        cardLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showFeedbackSettingsDialog() {
        String[] options = {"즉시 피드백", "대화 종료 후 피드백", "피드백 끄기"};
        int currentSetting = prefs.getInt("feedback_timing", 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("피드백 설정");
        builder.setSingleChoiceItems(options, currentSetting, (dialog, which) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("feedback_timing", which);
            editor.apply();

            Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void showThemeDialog() {
        String[] themes = {"라이트 모드", "다크 모드", "시스템 설정 따르기"};
        int currentTheme = prefs.getInt("theme", 2); // 기본값: 시스템 설정 따르기

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("테마 변경");
        builder.setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("theme", which);
            editor.apply();

            // 테마 실제 적용
            applyTheme(which);

            Toast.makeText(this, "테마가 변경되었습니다", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void applyTheme(int themeMode) {
        switch (themeMode) {
            case 0: // 라이트 모드
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1: // 다크 모드
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2: // 시스템 설정 따르기
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("로그아웃");
        builder.setMessage("정말 로그아웃 하시겠습니까?");

        builder.setPositiveButton("로그아웃", (dialog, which) -> {
            // Firebase 로그아웃
            FirebaseAuth.getInstance().signOut();

            // 로컬 로그인 정보 삭제
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_logged_in", false);
            editor.remove("current_user_email");
            editor.apply();

            Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

            // 스플래시 화면으로 이동
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }
}
