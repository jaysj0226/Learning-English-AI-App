package com.cookandroid.justspeakapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;
import com.google.android.material.button.MaterialButton;

import java.util.Map;

public class VoiceSettingsActivity extends AppCompatActivity {
    private static final String TAG = "VoiceSettingsActivity";

    private ImageButton btnBack;
    private RadioGroup rgVoiceGender;
    private RadioButton rbMale, rbFemale;
    private RadioGroup rgVoiceSpeed;
    private RadioButton rbSlow, rbNormal, rbFast;
    private MaterialButton btnSave;

    private UserDataManager userDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_settings);

        userDataManager = new UserDataManager(this);

        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rgVoiceGender = findViewById(R.id.rg_voice_gender);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        rgVoiceSpeed = findViewById(R.id.rg_voice_speed);
        rbSlow = findViewById(R.id.rb_slow);
        rbNormal = findViewById(R.id.rb_normal);
        rbFast = findViewById(R.id.rb_fast);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadCurrentSettings() {
        // UserDataManager에서 음성 설정 로드
        Map<String, Object> settings = userDataManager.getVoiceSettings();

        // 음성 성별 로드 (기본값: 여성)
        String voiceGender = (String) settings.get("voice_gender");
        if ("male".equals(voiceGender)) {
            rbMale.setChecked(true);
        } else {
            rbFemale.setChecked(true);
        }

        // 음성 속도 로드 (기본값: 1.0x)
        float voiceSpeed = (float) settings.get("voice_speed");
        if (voiceSpeed == 0.5f) {
            rbSlow.setChecked(true);
        } else if (voiceSpeed == 1.5f) {
            rbFast.setChecked(true);
        } else {
            rbNormal.setChecked(true);
        }

        Log.d(TAG, "Loaded voice settings - Gender: " + voiceGender + ", Speed: " + voiceSpeed);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        // 음성 성별
        String voiceGender = rbMale.isChecked() ? "male" : "female";

        // 음성 속도
        float voiceSpeed = 1.0f;
        if (rbSlow.isChecked()) {
            voiceSpeed = 0.5f;
        } else if (rbFast.isChecked()) {
            voiceSpeed = 1.5f;
        }

        Log.d(TAG, "=== SAVING VOICE SETTINGS ===");
        Log.d(TAG, "Gender: " + voiceGender + ", Speed: " + voiceSpeed);

        // UserDataManager를 통해 사용자별로 저장
        userDataManager.saveVoiceSettings(voiceGender, voiceSpeed, new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Voice settings saved successfully");
                runOnUiThread(() -> {
                    Toast.makeText(VoiceSettingsActivity.this, "음성 설정이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to save voice settings: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(VoiceSettingsActivity.this, "설정 저장 실패: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
