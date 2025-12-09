package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;

public class LevelTestActivity extends AppCompatActivity {
    private static final String TAG = "LevelTestActivity";

    private SharedPreferences prefs;
    private UserDataManager userDataManager;
    private Button btnStartTest, btnSkipTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_test);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnStartTest = findViewById(R.id.btn_start_test);
        // 건너뛰기 버튼은 레이아웃에서 visibility="gone"으로 설정되어 있음
    }

    private void setupListeners() {
        btnStartTest.setOnClickListener(v -> {
            // 레벨 테스트 시작
            startLevelTest();
        });
    }

    private void startLevelTest() {
        Log.d(TAG, "Starting level test");
        // 대화형 레벨 테스트 화면으로 이동
        Intent intent = new Intent(this, LevelTestConversationActivity.class);
        startActivity(intent);
        finish();
    }
}
