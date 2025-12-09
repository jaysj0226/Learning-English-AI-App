package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 2000; // 2초
    private SharedPreferences prefs;
    private Handler splashHandler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);

        // 앱 시작 시 저장된 테마 설정 적용
        applySavedTheme();

        setContentView(R.layout.activity_splash);

        Button btnGetStarted = findViewById(R.id.btn_get_started);
        Button btnLogin = findViewById(R.id.btn_login);

        // 이미 로그인한 사용자는 바로 메인으로
        if (isUserLoggedIn()) {
            splashHandler = new Handler(Looper.getMainLooper());
            splashRunnable = () -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            };
            splashHandler.postDelayed(splashRunnable, SPLASH_DELAY);
        }

        btnGetStarted.setOnClickListener(v -> {
            // 회원가입 화면으로 이동
            startActivity(new Intent(SplashActivity.this, SignupActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            // 로그인 화면으로 이동
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        });
    }

    private boolean isUserLoggedIn() {
        // Firebase Auth 상태와 로컬 SharedPreferences 모두 확인
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean localLoggedIn = prefs.getBoolean("is_logged_in", false);

        // Firebase 인증이 유효하고 로컬에도 로그인 상태면 true
        if (currentUser != null && localLoggedIn) {
            return true;
        }

        // Firebase와 로컬 상태가 불일치하면 동기화
        if (currentUser == null && localLoggedIn) {
            // Firebase 세션 만료 - 로컬 상태 초기화
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_logged_in", false);
            editor.remove("current_user_email");
            editor.apply();
        }

        return false;
    }

    private void applySavedTheme() {
        int savedTheme = prefs.getInt("theme", 2); // 기본값: 시스템 설정 따르기
        switch (savedTheme) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Handler 메모리 누수 방지: 콜백 제거
        if (splashHandler != null && splashRunnable != null) {
            splashHandler.removeCallbacks(splashRunnable);
        }
    }
}
