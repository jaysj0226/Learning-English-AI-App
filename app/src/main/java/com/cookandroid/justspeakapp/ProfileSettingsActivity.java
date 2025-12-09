package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileSettingsActivity extends AppCompatActivity {
    private static final String TAG = "ProfileSettings";

    private ImageButton btnBack;
    private MaterialCardView cardChangePassword, cardChangeInterests, cardChangeGoals, cardRetakeLevelTest;
    private TextView tvPasswordHint, tvCurrentInterests, tvCurrentGoals, tvCurrentLevel;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
        loadCurrentSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 설정 화면에서 돌아올 때 현재 설정 다시 로드
        loadCurrentSettings();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        cardChangePassword = findViewById(R.id.card_change_password);
        cardChangeInterests = findViewById(R.id.card_change_interests);
        cardChangeGoals = findViewById(R.id.card_change_goals);
        cardRetakeLevelTest = findViewById(R.id.card_retake_level_test);

        tvPasswordHint = findViewById(R.id.tv_password_hint);
        tvCurrentInterests = findViewById(R.id.tv_current_interests);
        tvCurrentGoals = findViewById(R.id.tv_current_goals);
        tvCurrentLevel = findViewById(R.id.tv_current_level);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // 비밀번호 변경
        cardChangePassword.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // 로그인 방식 확인
                boolean isEmailUser = false;
                for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                    if ("password".equals(profile.getProviderId())) {
                        isEmailUser = true;
                        break;
                    }
                }

                if (isEmailUser) {
                    showPasswordResetDialog(user.getEmail());
                } else {
                    Toast.makeText(this, "Google 로그인 사용자는 Google 계정에서 비밀번호를 변경해주세요", Toast.LENGTH_LONG).show();
                }
            }
        });

        // 관심사 변경
        cardChangeInterests.setOnClickListener(v -> {
            Intent intent = new Intent(this, InterestSelectionActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });

        // 학습 목표 변경
        cardChangeGoals.setOnClickListener(v -> {
            Intent intent = new Intent(this, LearningGoalActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });

        // 레벨 테스트 다시 하기
        cardRetakeLevelTest.setOnClickListener(v -> {
            showRetakeLevelTestDialog();
        });
    }

    private void loadCurrentSettings() {
        // 비밀번호 변경 가능 여부 표시
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            boolean isEmailUser = false;
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if ("password".equals(profile.getProviderId())) {
                    isEmailUser = true;
                    break;
                }
            }

            if (isEmailUser) {
                tvPasswordHint.setText("비밀번호 재설정 이메일 발송");
                cardChangePassword.setAlpha(1.0f);
            } else {
                tvPasswordHint.setText("Google 로그인은 지원되지 않음");
                cardChangePassword.setAlpha(0.5f);
            }
        }

        // 현재 관심사 표시
        String interests = prefs.getString("interests", null);
        if (interests != null && !interests.isEmpty()) {
            String displayInterests = formatInterests(interests);
            tvCurrentInterests.setText("현재: " + displayInterests);
        } else {
            tvCurrentInterests.setText("현재: 미설정");
        }

        // 현재 학습 목표 표시
        String goals = prefs.getString("learning_goal", null);
        if (goals != null && !goals.isEmpty()) {
            String displayGoals = formatGoals(goals);
            tvCurrentGoals.setText("현재: " + displayGoals);
        } else {
            tvCurrentGoals.setText("현재: 미설정");
        }

        // 현재 레벨 표시
        String level = prefs.getString("user_level", null);
        if (level != null && !level.isEmpty()) {
            tvCurrentLevel.setText("현재 레벨: " + level);
        } else {
            tvCurrentLevel.setText("현재 레벨: 미측정");
        }
    }

    private String formatInterests(String interests) {
        String[] items = interests.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(translateInterest(items[i].trim()));
            if (i < items.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String translateInterest(String interest) {
        switch (interest.toLowerCase()) {
            case "academic": return "학업";
            case "daily": return "일상";
            case "travel": return "여행";
            case "interview": return "면접";
            case "self_improvement": return "자기계발";
            case "business": return "비즈니스";
            default: return interest;
        }
    }

    private String formatGoals(String goals) {
        String[] items = goals.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(translateGoal(items[i].trim()));
            if (i < items.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String translateGoal(String goal) {
        switch (goal.toLowerCase()) {
            case "speaking": return "말하기";
            case "pronunciation": return "발음";
            case "listening": return "듣기";
            case "grammar": return "문법";
            case "vocabulary": return "어휘";
            default: return goal;
        }
    }

    private void showPasswordResetDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("비밀번호 재설정")
                .setMessage(email + "로 비밀번호 재설정 이메일을 보내시겠습니까?")
                .setPositiveButton("보내기", (dialog, which) -> {
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "비밀번호 재설정 이메일을 발송했습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Password reset email failed", task.getException());
                        Toast.makeText(this, "이메일 발송에 실패했습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRetakeLevelTestDialog() {
        new AlertDialog.Builder(this)
                .setTitle("레벨 테스트 다시 하기")
                .setMessage("레벨 테스트를 다시 진행하시겠습니까?\n\n기존 테스트 결과는 새로운 결과로 대체됩니다.")
                .setPositiveButton("테스트 시작", (dialog, which) -> {
                    Intent intent = new Intent(this, LevelTestActivity.class);
                    intent.putExtra("retake_mode", true);
                    startActivity(intent);
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
