package com.cookandroid.justspeakapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cookandroid.justspeakapp.data.UserDataManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Map;

public class AccountSettingsActivity extends AppCompatActivity {
    private static final String TAG = "AccountSettings";

    private ImageButton btnBack;
    private TextView tvEmail, tvProvider, tvUserLevel;
    private TextView tvTotalScore, tvGrammarScore, tvVocabScore, tvComplexityScore, tvCommunicationScore;
    private TextView tvStrength, tvWeakness, tvRecommendation;
    private LinearLayout detailScoresContainer;
    private Button btnDeleteAccount;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private UserDataManager userDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        prefs = getSharedPreferences("JustSpeakApp", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        userDataManager = new UserDataManager(this);

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupListeners();
        loadAndDisplayAccountInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvEmail = findViewById(R.id.tv_email);
        tvProvider = findViewById(R.id.tv_provider);
        tvUserLevel = findViewById(R.id.tv_user_level);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);

        // Detailed score views
        tvTotalScore = findViewById(R.id.tv_total_score);
        detailScoresContainer = findViewById(R.id.detail_scores_container);
        tvGrammarScore = findViewById(R.id.tv_grammar_score);
        tvVocabScore = findViewById(R.id.tv_vocab_score);
        tvComplexityScore = findViewById(R.id.tv_complexity_score);
        tvCommunicationScore = findViewById(R.id.tv_communication_score);

        // Strength/Weakness analysis views
        tvStrength = findViewById(R.id.tv_strength);
        tvWeakness = findViewById(R.id.tv_weakness);
        tvRecommendation = findViewById(R.id.tv_recommendation);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountDialog();
        });
    }

    private void loadAndDisplayAccountInfo() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // 이메일 표시
            String email = user.getEmail();
            tvEmail.setText(email != null ? email : "이메일 없음");

            // 로그인 제공자 표시
            String providerId = user.getProviderData().get(1).getProviderId();
            if (providerId.equals("google.com")) {
                tvProvider.setText("Google 계정");
            } else if (providerId.equals("password")) {
                tvProvider.setText("이메일 계정");
            } else {
                tvProvider.setText("기타");
            }

            // Firestore에서 사용자 데이터 가져오기
            userDataManager.getUserData(new UserDataManager.DataCallback() {
                @Override
                public void onSuccess(Map<String, Object> data) {
                    runOnUiThread(() -> {
                        String level = (String) data.get("level");
                        if (level != null) {
                            tvUserLevel.setText("레벨: " + level);
                        } else {
                            tvUserLevel.setText("레벨: 미설정");
                        }

                        // 대화형 테스트 결과인 경우 상세 점수 표시
                        Boolean isConversationTest = (Boolean) data.get("is_conversation_test");
                        if (isConversationTest != null && isConversationTest) {
                            // 총점 표시
                            Object scoreObj = data.get("score");
                            if (scoreObj != null && tvTotalScore != null) {
                                int score = scoreObj instanceof Long ? ((Long) scoreObj).intValue() : (Integer) scoreObj;
                                tvTotalScore.setText("종합 점수: " + score + "점");
                                tvTotalScore.setVisibility(View.VISIBLE);
                            }

                            // 상세 점수 컨테이너 표시
                            if (detailScoresContainer != null) {
                                detailScoresContainer.setVisibility(View.VISIBLE);

                                // 문법 점수
                                Object grammarObj = data.get("grammar_score");
                                if (grammarObj != null && tvGrammarScore != null) {
                                    int grammarScore = grammarObj instanceof Long ? ((Long) grammarObj).intValue() : (Integer) grammarObj;
                                    tvGrammarScore.setText(String.valueOf(grammarScore));
                                }

                                // 어휘 점수
                                Object vocabObj = data.get("vocabulary_score");
                                if (vocabObj != null && tvVocabScore != null) {
                                    int vocabScore = vocabObj instanceof Long ? ((Long) vocabObj).intValue() : (Integer) vocabObj;
                                    tvVocabScore.setText(String.valueOf(vocabScore));
                                }

                                // 복잡도 점수
                                Object complexityObj = data.get("complexity_score");
                                if (complexityObj != null && tvComplexityScore != null) {
                                    int complexityScore = complexityObj instanceof Long ? ((Long) complexityObj).intValue() : (Integer) complexityObj;
                                    tvComplexityScore.setText(String.valueOf(complexityScore));
                                }

                                // 의사소통 점수
                                Object communicationObj = data.get("communication_score");
                                if (communicationObj != null && tvCommunicationScore != null) {
                                    int communicationScore = communicationObj instanceof Long ? ((Long) communicationObj).intValue() : (Integer) communicationObj;
                                    tvCommunicationScore.setText(String.valueOf(communicationScore));
                                }

                                // 강점/약점 분석
                                analyzeStrengthsAndWeaknesses(
                                        grammarObj != null ? (grammarObj instanceof Long ? ((Long) grammarObj).intValue() : (Integer) grammarObj) : 0,
                                        vocabObj != null ? (vocabObj instanceof Long ? ((Long) vocabObj).intValue() : (Integer) vocabObj) : 0,
                                        complexityObj != null ? (complexityObj instanceof Long ? ((Long) complexityObj).intValue() : (Integer) complexityObj) : 0,
                                        communicationObj != null ? (communicationObj instanceof Long ? ((Long) communicationObj).intValue() : (Integer) communicationObj) : 0
                                );
                            }
                        } else {
                            // 대화형 테스트가 아닌 경우 상세 점수 숨김
                            if (tvTotalScore != null) tvTotalScore.setVisibility(View.GONE);
                            if (detailScoresContainer != null) detailScoresContainer.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading user data: " + error);
                    runOnUiThread(() -> {
                        tvUserLevel.setText("레벨: 불러오기 실패");
                        if (tvTotalScore != null) tvTotalScore.setVisibility(View.GONE);
                        if (detailScoresContainer != null) detailScoresContainer.setVisibility(View.GONE);
                    });
                }
            });
        } else {
            tvEmail.setText("로그인 정보 없음");
            tvProvider.setText("-");
            tvUserLevel.setText("-");
            if (tvTotalScore != null) tvTotalScore.setVisibility(View.GONE);
            if (detailScoresContainer != null) detailScoresContainer.setVisibility(View.GONE);
        }
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("계정 탈퇴");
        builder.setMessage("정말로 계정을 탈퇴하시겠습니까?\n\n" +
                "⚠️ 탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.\n\n" +
                "• 학습 기록\n" +
                "• 대화 내역\n" +
                "• 설정 정보\n\n" +
                "이 작업은 되돌릴 수 없습니다.");

        builder.setPositiveButton("탈퇴하기", (dialog, which) -> {
            deleteAccount();
        });

        builder.setNegativeButton("취소", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 탈퇴하기 버튼을 빨간색으로 표시
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                getResources().getColor(R.color.error, null)
        );
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "deleteAccount: user is null");
            Toast.makeText(this, "로그인 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting account deletion process for user: " + user.getEmail());

        // 먼저 로컬 데이터 삭제
        userDataManager.deleteUserData(new UserDataManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Local user data deleted successfully");

                // Firebase Authentication에서 계정 삭제
                Log.d(TAG, "Attempting to delete Firebase account...");
                user.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase account deleted successfully");

                                // Google 계정 로그아웃
                                mGoogleSignInClient.signOut();

                                // 로컬 데이터 삭제
                                clearLocalData();

                                Toast.makeText(AccountSettingsActivity.this,
                                        "계정이 탈퇴되었습니다", Toast.LENGTH_SHORT).show();

                                // 스플래시 화면으로 이동
                                Intent intent = new Intent(AccountSettingsActivity.this, SplashActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                // 에러 로깅
                                Exception exception = task.getException();
                                String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
                                String errorClass = exception != null ? exception.getClass().getName() : "Unknown";

                                Log.e(TAG, "Account deletion failed");
                                Log.e(TAG, "Error class: " + errorClass);
                                Log.e(TAG, "Error message: " + errorMessage);
                                if (exception != null) {
                                    exception.printStackTrace();
                                }

                                // 재인증이 필요한 경우
                                if (exception != null &&
                                    (errorMessage.contains("requires recent authentication") ||
                                     errorMessage.contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN") ||
                                     errorClass.contains("FirebaseAuthRecentLoginRequiredException"))) {

                                    Log.d(TAG, "Re-authentication required");
                                    showReauthenticationDialog();
                                } else {
                                    Toast.makeText(AccountSettingsActivity.this,
                                        "계정 탈퇴 실패: " + errorMessage,
                                        Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Account deletion addOnFailureListener triggered", e);
                            Toast.makeText(AccountSettingsActivity.this,
                                    "계정 탈퇴 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting local user data: " + error);
                Toast.makeText(AccountSettingsActivity.this,
                        "데이터 삭제 중 오류가 발생했습니다: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showReauthenticationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("재인증 필요");
        builder.setMessage("계정 탈퇴를 위해 다시 로그인해주세요.\n\n" +
                "보안을 위해 최근에 로그인한 사용자만 계정을 탈퇴할 수 있습니다.");

        builder.setPositiveButton("확인", (dialog, which) -> {
            // 로그아웃 후 로그인 화면으로 이동
            mAuth.signOut();
            mGoogleSignInClient.signOut();

            Toast.makeText(this, "다시 로그인한 후 계정 탈퇴를 시도해주세요", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void clearLocalData() {
        // SharedPreferences 모든 데이터 삭제
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d(TAG, "Local data cleared");
    }

    private void analyzeStrengthsAndWeaknesses(int grammar, int vocab, int complexity, int communication) {
        // 점수와 카테고리 이름을 매핑
        String[] categories = {"문법", "어휘", "문장복잡도", "의사소통"};
        int[] scores = {grammar, vocab, complexity, communication};

        // 최고점과 최저점 찾기
        int maxIndex = 0, minIndex = 0;
        int maxScore = scores[0], minScore = scores[0];

        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
            if (scores[i] < minScore) {
                minScore = scores[i];
                minIndex = i;
            }
        }

        // 강점 메시지 설정
        String strengthMsg = getStrengthMessage(categories[maxIndex], maxScore);
        if (tvStrength != null) {
            tvStrength.setText(strengthMsg);
        }

        // 약점 메시지 설정
        String weaknessMsg = getWeaknessMessage(categories[minIndex], minScore);
        if (tvWeakness != null) {
            tvWeakness.setText(weaknessMsg);
        }

        // 추천 학습 메시지 설정
        String recommendationMsg = getRecommendationMessage(categories[minIndex]);
        if (tvRecommendation != null) {
            tvRecommendation.setText(recommendationMsg);
        }
    }

    private String getStrengthMessage(String category, int score) {
        if (score >= 90) {
            return category + " 능력이 매우 뛰어납니다! (" + score + "점)";
        } else if (score >= 80) {
            return category + " 능력이 우수합니다 (" + score + "점)";
        } else if (score >= 70) {
            return category + " 능력이 양호합니다 (" + score + "점)";
        } else {
            return category + "이(가) 상대적으로 강점입니다 (" + score + "점)";
        }
    }

    private String getWeaknessMessage(String category, int score) {
        if (score < 50) {
            return category + " 학습이 시급합니다 (" + score + "점)";
        } else if (score < 60) {
            return category + " 향상이 필요합니다 (" + score + "점)";
        } else if (score < 70) {
            return category + "을(를) 더 연습해보세요 (" + score + "점)";
        } else {
            return category + "도 조금 더 향상시켜보세요 (" + score + "점)";
        }
    }

    private String getRecommendationMessage(String category) {
        switch (category) {
            case "문법":
                return "기본 문법 규칙을 복습하고, 다양한 문장 구조로 연습해보세요";
            case "어휘":
                return "새로운 단어를 매일 학습하고, 문맥 속에서 활용해보세요";
            case "문장복잡도":
                return "접속사와 관계대명사를 활용해 복잡한 문장을 만들어보세요";
            case "의사소통":
                return "실제 대화 연습을 늘리고, 자연스러운 표현을 익혀보세요";
            default:
                return "꾸준한 연습으로 실력을 향상시켜보세요";
        }
    }
}
