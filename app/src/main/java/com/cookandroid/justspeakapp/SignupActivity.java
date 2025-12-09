package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    private EditText etEmail, etName;
    private TextInputEditText etPassword, etPasswordConfirm;
    private Button btnSignup, btnGoogleSignup;
    private ImageButton btnBack;
    private CheckBox cbTerms;
    private SharedPreferences prefs;

    // Password strength UI
    private LinearLayout passwordStrengthContainer;
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private TextView tvPasswordStrength;

    // Firebase Authentication
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Activity Result Launcher for Google Sign-In
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Password strength levels
    private static final int STRENGTH_WEAK = 1;
    private static final int STRENGTH_FAIR = 2;
    private static final int STRENGTH_GOOD = 3;
    private static final int STRENGTH_STRONG = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);

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
                        Toast.makeText(this, "Google 회원가입이 취소되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        btnSignup = findViewById(R.id.btn_signup);
        btnGoogleSignup = findViewById(R.id.btn_google_signup);
        btnBack = findViewById(R.id.btn_back);
        cbTerms = findViewById(R.id.cb_terms);

        // Password strength UI
        passwordStrengthContainer = findViewById(R.id.password_strength_container);
        strengthBar1 = findViewById(R.id.strength_bar_1);
        strengthBar2 = findViewById(R.id.strength_bar_2);
        strengthBar3 = findViewById(R.id.strength_bar_3);
        strengthBar4 = findViewById(R.id.strength_bar_4);
        tvPasswordStrength = findViewById(R.id.tv_password_strength);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSignup.setOnClickListener(v -> handleSignup());

        btnGoogleSignup.setOnClickListener(v -> signUpWithGoogle());

        // Password strength indicator
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthContainer.setVisibility(View.GONE);
            tvPasswordStrength.setVisibility(View.GONE);
            return;
        }

        passwordStrengthContainer.setVisibility(View.VISIBLE);
        tvPasswordStrength.setVisibility(View.VISIBLE);

        int strength = calculatePasswordStrength(password);
        int colorRes;
        String strengthText;

        switch (strength) {
            case STRENGTH_WEAK:
                colorRes = R.color.error;
                strengthText = "약함 - 더 복잡한 비밀번호를 사용하세요";
                break;
            case STRENGTH_FAIR:
                colorRes = R.color.warning;
                strengthText = "보통 - 특수문자를 추가하세요";
                break;
            case STRENGTH_GOOD:
                colorRes = R.color.success;
                strengthText = "좋음 - 조금 더 길게 만들어보세요";
                break;
            case STRENGTH_STRONG:
                colorRes = R.color.success;
                strengthText = "강함 - 훌륭한 비밀번호입니다!";
                break;
            default:
                colorRes = R.color.divider;
                strengthText = "";
        }

        int activeColor = ContextCompat.getColor(this, colorRes);
        int inactiveColor = ContextCompat.getColor(this, R.color.divider);

        strengthBar1.setBackgroundColor(strength >= 1 ? activeColor : inactiveColor);
        strengthBar2.setBackgroundColor(strength >= 2 ? activeColor : inactiveColor);
        strengthBar3.setBackgroundColor(strength >= 3 ? activeColor : inactiveColor);
        strengthBar4.setBackgroundColor(strength >= 4 ? activeColor : inactiveColor);

        tvPasswordStrength.setText(strengthText);
        tvPasswordStrength.setTextColor(activeColor);
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;

        // Length check
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Contains uppercase
        if (password.matches(".*[A-Z].*")) score++;

        // Contains lowercase
        if (password.matches(".*[a-z].*")) score++;

        // Contains digit
        if (password.matches(".*\\d.*")) score++;

        // Contains special character
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        // Map score to strength level
        if (score <= 2) return STRENGTH_WEAK;
        if (score <= 3) return STRENGTH_FAIR;
        if (score <= 4) return STRENGTH_GOOD;
        return STRENGTH_STRONG;
    }

    private boolean isPasswordValid(String password) {
        // At least 8 characters
        if (password.length() < 8) return false;

        // Contains uppercase
        if (!password.matches(".*[A-Z].*")) return false;

        // Contains lowercase
        if (!password.matches(".*[a-z].*")) return false;

        // Contains digit
        if (!password.matches(".*\\d.*")) return false;

        // Contains special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) return false;

        return true;
    }

    private void handleSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();

        // 이름 유효성 검사
        if (TextUtils.isEmpty(name)) {
            etName.setError("이름을 입력하세요");
            etName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            etName.setError("이름은 2자 이상이어야 합니다");
            etName.requestFocus();
            return;
        }

        // 이메일 유효성 검사
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

        // 비밀번호 유효성 검사
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("비밀번호를 입력하세요");
            etPassword.requestFocus();
            return;
        }

        if (!isPasswordValid(password)) {
            etPassword.setError("비밀번호는 8자 이상, 대/소문자, 숫자, 특수문자를 포함해야 합니다");
            etPassword.requestFocus();
            return;
        }

        // 비밀번호 확인 검사
        if (TextUtils.isEmpty(passwordConfirm)) {
            etPasswordConfirm.setError("비밀번호 확인을 입력하세요");
            etPasswordConfirm.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("비밀번호가 일치하지 않습니다");
            etPasswordConfirm.requestFocus();
            return;
        }

        // 약관 동의 검사
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "이용약관에 동의해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        btnSignup.setEnabled(false);
        btnSignup.setText("가입 중...");

        // Firebase Authentication으로 회원가입
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email signup successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // 사용자 프로필에 이름 설정
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated with name: " + name);
                                        }
                                    });

                            // 이메일 인증 발송
                            sendEmailVerification(user, name);
                        }
                    } else {
                        btnSignup.setEnabled(true);
                        btnSignup.setText("가입하기");

                        Log.w(TAG, "Email signup failed", task.getException());
                        String errorMessage = "회원가입 실패";
                        if (task.getException() != null) {
                            String exceptionMsg = task.getException().getMessage();
                            Log.e(TAG, "Full error: " + exceptionMsg);
                            if (exceptionMsg != null) {
                                if (exceptionMsg.contains("already in use")) {
                                    errorMessage = "이미 사용 중인 이메일입니다";
                                } else if (exceptionMsg.contains("network error")) {
                                    errorMessage = "네트워크 오류가 발생했습니다";
                                } else if (exceptionMsg.contains("operation is not allowed")) {
                                    errorMessage = "Firebase Console에서 이메일/비밀번호 로그인을 활성화해주세요\n\nFirebase Console → Authentication → Sign-in method → Email/Password 활성화";
                                } else if (exceptionMsg.contains("CONFIGURATION_NOT_FOUND")) {
                                    errorMessage = "Firebase 설정이 올바르지 않습니다. google-services.json을 확인해주세요";
                                } else {
                                    errorMessage = "회원가입 실패: " + exceptionMsg;
                                }
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, String name) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent to " + user.getEmail());
                        showEmailVerificationDialog(user.getEmail(), name);
                    } else {
                        Log.e(TAG, "Failed to send verification email", task.getException());
                        // 이메일 인증 발송 실패해도 가입은 진행
                        Toast.makeText(this, "인증 이메일 발송에 실패했습니다. 나중에 다시 시도하세요.",
                                Toast.LENGTH_SHORT).show();
                        signupSuccess(user.getEmail(), name);
                    }
                });
    }

    private void showEmailVerificationDialog(String email, String name) {
        new AlertDialog.Builder(this)
                .setTitle("이메일 인증 필요")
                .setMessage("인증 이메일을 " + email + "로 발송했습니다.\n\n이메일을 확인하고 인증 링크를 클릭해주세요.\n\n인증 완료 후 로그인할 수 있습니다.")
                .setPositiveButton("확인", (dialog, which) -> {
                    // 로그아웃 후 로그인 화면으로 이동
                    mAuth.signOut();
                    Toast.makeText(this, "이메일 인증 후 로그인해주세요", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setNeutralButton("인증 메일 재발송", (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        sendEmailVerification(user, name);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void signUpWithGoogle() {
        // 기존 Google 로그인 세션 정리
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Previous Google session signed out");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign-In successful: " + account.getEmail());
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed", e);
            Toast.makeText(this, "Google 회원가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            signupSuccess(user.getEmail(), user.getDisplayName());
                        }
                    } else {
                        Log.w(TAG, "Firebase Authentication failed", task.getException());
                        Toast.makeText(this, "인증 실패: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signupSuccess(String email, String name) {
        // 회원가입 상태 저장
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.putString("user_name", name != null ? name : "User");
        editor.putBoolean("is_logged_in", true);
        editor.putString("current_user_email", email);
        editor.apply();

        Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show();

        // 관심사 선택 화면으로 이동
        Intent intent = new Intent(this, InterestSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
