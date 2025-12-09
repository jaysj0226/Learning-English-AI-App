package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.cookandroid.justspeakapp.data.UserDataManager;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etEmail;
    private TextInputEditText etPassword;
    private Button btnLogin, btnGoogleLogin;
    private ImageButton btnBack;
    private SharedPreferences prefs;
    private UserDataManager userDataManager;

    // Firebase Authentication
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Activity Result Launcher for Google Sign-In
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        userDataManager = new UserDataManager(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Register Google Sign-In Activity Result Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Google Sign-In result code: " + result.getResultCode());
                    Intent data = result.getData();
                    if (data != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Log.w(TAG, "Google Sign-In result data is null");
                        Toast.makeText(this, "Google 로그인이 취소되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> handleLogin());

        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 유효성 검사
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("이메일을 입력하세요");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("올바른 이메일 형식이 아닙니다");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("비밀번호를 입력하세요");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("비밀번호는 6자 이상이어야 합니다");
            etPassword.requestFocus();
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        btnLogin.setEnabled(false);
        btnLogin.setText("로그인 중...");

        // Firebase Authentication으로 로그인
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("로그인");

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 이메일 인증 여부 확인
                            if (user.isEmailVerified()) {
                                loginSuccess(user.getEmail());
                            } else {
                                // 이메일 인증이 안 된 경우
                                showEmailVerificationRequiredDialog(user);
                            }
                        }
                    } else {
                        Log.w(TAG, "Email login failed", task.getException());
                        String errorMessage = "로그인 실패";
                        if (task.getException() != null) {
                            String exceptionMsg = task.getException().getMessage();
                            Log.e(TAG, "Full error: " + exceptionMsg); // 개발자 로그에만 상세 내용 기록
                            if (exceptionMsg != null) {
                                String lowerMsg = exceptionMsg.toLowerCase();
                                if (lowerMsg.contains("no user record") || lowerMsg.contains("user not found") || lowerMsg.contains("user-not-found")) {
                                    errorMessage = "존재하지 않는 계정입니다";
                                } else if (lowerMsg.contains("wrong password") || lowerMsg.contains("invalid credential") || lowerMsg.contains("wrong-password")) {
                                    errorMessage = "이메일 또는 비밀번호가 일치하지 않습니다";
                                } else if (lowerMsg.contains("network")) {
                                    errorMessage = "네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.";
                                } else if (lowerMsg.contains("too-many-requests") || lowerMsg.contains("too many")) {
                                    errorMessage = "너무 많은 로그인 시도가 있었습니다. 잠시 후 다시 시도해주세요.";
                                } else if (lowerMsg.contains("operation is not allowed") || lowerMsg.contains("operation-not-allowed")) {
                                    errorMessage = "이메일 로그인이 비활성화되어 있습니다. 관리자에게 문의하세요.";
                                } else if (lowerMsg.contains("invalid-email")) {
                                    errorMessage = "올바르지 않은 이메일 형식입니다.";
                                } else {
                                    // 기타 에러는 일반적인 메시지로 표시 (내부 메시지 노출 방지)
                                    errorMessage = "로그인에 실패했습니다. 다시 시도해주세요.";
                                }
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showEmailVerificationRequiredDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("이메일 인증 필요")
                .setMessage("이메일 인증이 완료되지 않았습니다.\n\n" + user.getEmail() + "로 발송된 인증 메일을 확인해주세요.")
                .setPositiveButton("확인", (dialog, which) -> {
                    mAuth.signOut();
                })
                .setNeutralButton("인증 메일 재발송", (dialog, which) -> {
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "인증 메일을 재발송했습니다", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "인증 메일 발송에 실패했습니다", Toast.LENGTH_SHORT).show();
                                }
                                mAuth.signOut();
                            });
                })
                .setCancelable(false)
                .show();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed", e);
            Toast.makeText(this, "Google 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loginSuccess(user.getEmail());
                        }
                    } else {
                        Log.w(TAG, "Firebase Authentication failed", task.getException());
                        Toast.makeText(this, "인증 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginSuccess(String email) {
        // 로그인 상태 저장
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("current_user_email", email);
        editor.apply();

        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show();

        // 클라우드에서 데이터 동기화 후 메인 화면으로 이동
        syncDataFromCloudAndNavigate();
    }

    private void syncDataFromCloudAndNavigate() {
        // 먼저 온보딩 데이터 동기화 (관심사, 목표, 온보딩 상태)
        userDataManager.syncOnboardingDataFromCloud(new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Onboarding data synced from cloud");
                syncLevelTestData();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to sync onboarding data: " + error);
                // 실패해도 레벨 테스트 동기화 시도
                syncLevelTestData();
            }
        });
    }

    private void syncLevelTestData() {
        // 레벨 테스트 결과 동기화
        userDataManager.syncLevelTestFromCloud(new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Level test data synced from cloud");
                // 음성 설정 동기화
                syncVoiceSettingsAndNavigate();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to sync level test data: " + error);
                // 실패해도 음성 설정 동기화 시도
                syncVoiceSettingsAndNavigate();
            }
        });
    }

    private void syncVoiceSettingsAndNavigate() {
        userDataManager.syncVoiceSettingsFromCloud(new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Voice settings synced from cloud");
                checkOnboardingAndNavigate();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to sync voice settings: " + error);
                // 실패해도 온보딩 체크 후 이동
                checkOnboardingAndNavigate();
            }
        });
    }

    private void checkOnboardingAndNavigate() {
        // 온보딩 완료 여부 확인
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);
        String userInterests = prefs.getString("interests", null);
        String learningGoal = prefs.getString("learning_goal", null);

        Log.d(TAG, "Checking onboarding status - completed: " + onboardingCompleted +
                ", interests: " + userInterests + ", goal: " + learningGoal);

        if (!onboardingCompleted) {
            // 온보딩이 완료되지 않은 경우
            if (userInterests == null || userInterests.isEmpty()) {
                // 관심사를 선택하지 않은 경우 → 관심사 선택 화면으로
                Log.d(TAG, "Navigating to InterestSelectionActivity");
                Intent intent = new Intent(this, InterestSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (learningGoal == null || learningGoal.isEmpty()) {
                // 학습 목표를 선택하지 않은 경우 → 목표 선택 화면으로
                Log.d(TAG, "Navigating to LearningGoalActivity");
                Intent intent = new Intent(this, LearningGoalActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // 관심사와 목표는 있지만 레벨 테스트가 안 된 경우 → 레벨 테스트 화면으로
                Log.d(TAG, "Navigating to LevelTestActivity");
                Intent intent = new Intent(this, LevelTestActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        } else {
            // 온보딩이 완료된 경우 → 메인 화면으로
            navigateToMain();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
