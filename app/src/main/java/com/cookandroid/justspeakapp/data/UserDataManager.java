package com.cookandroid.justspeakapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자별 데이터를 관리하는 클래스
 * - 로컬 캐시: SharedPreferences (빠른 접근)
 * - 클라우드 동기화: Firebase Firestore (백업 & 멀티 디바이스)
 * Firebase UID를 기반으로 데이터를 분리하여 저장
 */
public class UserDataManager {
    private static final String TAG = "UserDataManager";
    private static final String PREFS_NAME = "JustSpeakApp_UserData";

    private final SharedPreferences prefs;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Gson gson;

    public interface DataCallback {
        void onSuccess(Map<String, Object> data);
        void onError(String error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String error);
    }

    public UserDataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.gson = new Gson();
    }

    /**
     * 현재 로그인한 사용자의 UID를 가져옴
     */
    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * UID 기반 키 생성
     */
    private String getUserKey(String key) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return userId + "_" + key;
    }

    /**
     * 대화형 레벨 테스트 결과 저장 (로컬 + Firestore 동기화)
     * 문법, 어휘, 문장복잡도, 의사소통 세부 점수 포함
     */
    public void saveLevelTestResult(String level, int score, int grammarScore, int vocabScore,
                                    int complexityScore, int communicationScore, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("level"), level);
            editor.putInt(getUserKey("test_score"), score);
            editor.putInt(getUserKey("grammar_score"), grammarScore);
            editor.putInt(getUserKey("vocabulary_score"), vocabScore);
            editor.putInt(getUserKey("complexity_score"), complexityScore);
            editor.putInt(getUserKey("communication_score"), communicationScore);
            editor.putBoolean(getUserKey("level_test_completed"), true);
            editor.putBoolean(getUserKey("is_conversation_test"), true);
            editor.putLong(getUserKey("level_test_date"), timestamp);
            editor.apply();

            Log.d(TAG, "Conversation level test result saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> levelData = new HashMap<>();
            levelData.put("level", level);
            levelData.put("test_score", score);
            levelData.put("grammar_score", grammarScore);
            levelData.put("vocabulary_score", vocabScore);
            levelData.put("complexity_score", complexityScore);
            levelData.put("communication_score", communicationScore);
            levelData.put("is_conversation_test", true);
            levelData.put("level_test_completed", true);
            levelData.put("level_test_date", timestamp);
            levelData.put("updated_at", timestamp);

            firestore.collection("users").document(userId)
                    .set(levelData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Conversation level test result synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed, but local save succeeded", e);
                        callback.onSuccess(); // 로컬 저장 성공했으므로 성공 처리
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving conversation level test result", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자의 레벨 테스트 결과 저장 (로컬 + Firestore 동기화)
     */
    public void saveLevelTestResult(String level, int score, int correctAnswers, int totalQuestions,
                                    OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("level"), level);
            editor.putInt(getUserKey("test_score"), score);
            editor.putInt(getUserKey("test_correct_answers"), correctAnswers);
            editor.putInt(getUserKey("test_total_questions"), totalQuestions);
            editor.putBoolean(getUserKey("level_test_completed"), true);
            editor.putLong(getUserKey("level_test_date"), timestamp);
            editor.apply();

            Log.d(TAG, "Level test result saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> levelData = new HashMap<>();
            levelData.put("level", level);
            levelData.put("test_score", score);
            levelData.put("correct_answers", correctAnswers);
            levelData.put("total_questions", totalQuestions);
            levelData.put("level_test_completed", true);
            levelData.put("level_test_date", timestamp);
            levelData.put("updated_at", timestamp);

            firestore.collection("users").document(userId)
                    .set(levelData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Level test result synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed, but local save succeeded", e);
                        callback.onSuccess(); // 로컬 저장 성공했으므로 성공 처리
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving level test result", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자의 관심사 저장 (로컬 + Firestore 동기화)
     */
    public void saveUserInterests(String interests, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("interests"), interests);
            editor.putLong(getUserKey("interests_updated"), timestamp);
            editor.apply();

            Log.d(TAG, "User interests saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> interestsData = new HashMap<>();
            interestsData.put("interests", interests);
            interestsData.put("interests_updated", timestamp);

            firestore.collection("users").document(userId)
                    .set(interestsData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User interests synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for interests, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving user interests", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자의 학습 목표 저장 (로컬 + Firestore 동기화)
     */
    public void saveLearningGoal(String goal, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("learning_goal"), goal);
            editor.putLong(getUserKey("goal_updated"), timestamp);
            editor.apply();

            Log.d(TAG, "Learning goal saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> goalData = new HashMap<>();
            goalData.put("learning_goal", goal);
            goalData.put("goal_updated", timestamp);

            firestore.collection("users").document(userId)
                    .set(goalData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Learning goal synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for learning goal, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving learning goal", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 온보딩 완료 상태 저장 (로컬 + Firestore 동기화)
     */
    public void setOnboardingCompleted(boolean completed, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(getUserKey("onboarding_completed"), completed);
            editor.putLong(getUserKey("onboarding_date"), timestamp);
            editor.apply();

            Log.d(TAG, "Onboarding status saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> onboardingData = new HashMap<>();
            onboardingData.put("onboarding_completed", completed);
            onboardingData.put("onboarding_date", timestamp);

            firestore.collection("users").document(userId)
                    .set(onboardingData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Onboarding status synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for onboarding, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving onboarding status", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자 데이터 가져오기
     */
    public void getUserData(DataCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();

            // 모든 사용자 데이터를 Map에 담아서 반환
            String level = prefs.getString(getUserKey("level"), null);
            if (level != null) data.put("level", level);

            int testScore = prefs.getInt(getUserKey("test_score"), -1);
            if (testScore != -1) data.put("test_score", testScore);

            int correctAnswers = prefs.getInt(getUserKey("test_correct_answers"), -1);
            if (correctAnswers != -1) data.put("test_correct_answers", correctAnswers);

            int totalQuestions = prefs.getInt(getUserKey("test_total_questions"), -1);
            if (totalQuestions != -1) data.put("test_total_questions", totalQuestions);

            boolean levelTestCompleted = prefs.getBoolean(getUserKey("level_test_completed"), false);
            data.put("level_test_completed", levelTestCompleted);

            boolean isConversationTest = prefs.getBoolean(getUserKey("is_conversation_test"), false);
            data.put("is_conversation_test", isConversationTest);

            // 대화형 레벨 테스트 세부 점수
            int grammarScore = prefs.getInt(getUserKey("grammar_score"), -1);
            if (grammarScore != -1) data.put("grammar_score", grammarScore);

            int vocabularyScore = prefs.getInt(getUserKey("vocabulary_score"), -1);
            if (vocabularyScore != -1) data.put("vocabulary_score", vocabularyScore);

            int complexityScore = prefs.getInt(getUserKey("complexity_score"), -1);
            if (complexityScore != -1) data.put("complexity_score", complexityScore);

            int communicationScore = prefs.getInt(getUserKey("communication_score"), -1);
            if (communicationScore != -1) data.put("communication_score", communicationScore);

            long levelTestDate = prefs.getLong(getUserKey("level_test_date"), -1);
            if (levelTestDate != -1) data.put("level_test_date", levelTestDate);

            String interests = prefs.getString(getUserKey("interests"), null);
            if (interests != null) data.put("interests", interests);

            long interestsUpdated = prefs.getLong(getUserKey("interests_updated"), -1);
            if (interestsUpdated != -1) data.put("interests_updated", interestsUpdated);

            String learningGoal = prefs.getString(getUserKey("learning_goal"), null);
            if (learningGoal != null) data.put("learning_goal", learningGoal);

            long goalUpdated = prefs.getLong(getUserKey("goal_updated"), -1);
            if (goalUpdated != -1) data.put("goal_updated", goalUpdated);

            boolean onboardingCompleted = prefs.getBoolean(getUserKey("onboarding_completed"), false);
            data.put("onboarding_completed", onboardingCompleted);

            long onboardingDate = prefs.getLong(getUserKey("onboarding_date"), -1);
            if (onboardingDate != -1) data.put("onboarding_date", onboardingDate);

            Log.d(TAG, "User data retrieved successfully for user: " + userId);
            callback.onSuccess(data);
        } catch (Exception e) {
            Log.e(TAG, "Error getting user data", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자 레벨만 저장 (로컬 + Firestore 동기화)
     */
    public void saveUserLevel(String level, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("level"), level);
            editor.putLong(getUserKey("level_updated"), timestamp);
            editor.apply();

            Log.d(TAG, "User level saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> levelData = new HashMap<>();
            levelData.put("level", level);
            levelData.put("updated_at", timestamp);

            firestore.collection("users").document(userId)
                    .set(levelData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User level synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for level, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving user level", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 사용자의 레벨 가져오기
     */
    public void getUserLevel(DataCallback callback) {
        getUserData(new DataCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                String level = (String) data.get("level");
                Map<String, Object> result = new HashMap<>();
                result.put("level", level != null ? level : "Beginner");
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * 대화 로그 저장 (로컬 + Firestore 동기화)
     */
    public void saveConversationLog(String scenario, String conversationData, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            String conversationLogsJson = prefs.getString(getUserKey("conversation_logs"), "[]");
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> logs = gson.fromJson(conversationLogsJson, listType);

            if (logs == null) {
                logs = new ArrayList<>();
            }

            Map<String, Object> newLog = new HashMap<>();
            newLog.put("scenario", scenario);
            newLog.put("conversation_data", conversationData);
            newLog.put("timestamp", timestamp);
            logs.add(newLog);

            String updatedJson = gson.toJson(logs);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("conversation_logs"), updatedJson);
            editor.apply();

            Log.d(TAG, "Conversation log saved to local for user: " + userId);

            // 2. Firestore에 동기화 (각 대화를 별도 문서로 저장)
            Map<String, Object> firestoreLog = new HashMap<>();
            firestoreLog.put("scenario", scenario);
            firestoreLog.put("conversation_data", conversationData);
            firestoreLog.put("timestamp", timestamp);

            firestore.collection("users").document(userId)
                    .collection("conversation_logs").add(firestoreLog)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "Conversation log synced to Firestore: " + docRef.getId());
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for conversation log, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving conversation log", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 학습 진도 업데이트 (로컬 + Firestore 동기화)
     */
    public void updateLearningProgress(String scenario, int completedLessons, int totalLessons,
                                      OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 저장
            Map<String, Object> progress = new HashMap<>();
            progress.put("completed", completedLessons);
            progress.put("total", totalLessons);
            progress.put("last_updated", timestamp);

            String progressJson = gson.toJson(progress);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("progress_" + scenario), progressJson);
            editor.apply();

            Log.d(TAG, "Learning progress saved to local for user: " + userId);

            // 2. Firestore에 동기화
            Map<String, Object> firestoreProgress = new HashMap<>();
            firestoreProgress.put("completed", completedLessons);
            firestoreProgress.put("total", totalLessons);
            firestoreProgress.put("last_updated", timestamp);

            firestore.collection("users").document(userId)
                    .collection("progress").document(scenario)
                    .set(firestoreProgress, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Learning progress synced to Firestore for scenario: " + scenario);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for progress, but local save succeeded", e);
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error updating learning progress", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 특정 시나리오의 학습 진도 가져오기
     */
    public Map<String, Integer> getScenarioProgress(String scenario) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        try {
            String progressJson = prefs.getString(getUserKey("progress_" + scenario), null);
            if (progressJson == null) {
                return null;
            }

            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> progressData = gson.fromJson(progressJson, mapType);

            Map<String, Integer> result = new HashMap<>();

            // Cast 예외 처리: 데이터 형식이 잘못된 경우 기본값 사용
            try {
                Object completedObj = progressData.get("completed");
                result.put("completed", completedObj != null ? ((Number) completedObj).intValue() : 0);
            } catch (ClassCastException e) {
                Log.w(TAG, "Invalid completed value format, using default 0");
                result.put("completed", 0);
            }

            try {
                Object totalObj = progressData.get("total");
                result.put("total", totalObj != null ? ((Number) totalObj).intValue() : 10);
            } catch (ClassCastException e) {
                Log.w(TAG, "Invalid total value format, using default 10");
                result.put("total", 10);
            }

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting scenario progress", e);
            return null;
        }
    }

    /**
     * 모든 시나리오의 진도 데이터 가져오기
     */
    public Map<String, Map<String, Integer>> getAllScenarioProgress() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new HashMap<>();
        }

        Map<String, Map<String, Integer>> allProgress = new HashMap<>();

        try {
            Map<String, ?> allPrefs = prefs.getAll();
            String progressPrefix = userId + "_progress_";

            for (String key : allPrefs.keySet()) {
                if (key.startsWith(progressPrefix)) {
                    String scenarioId = key.substring(progressPrefix.length());
                    Map<String, Integer> progress = getScenarioProgress(scenarioId);
                    if (progress != null) {
                        allProgress.put(scenarioId, progress);
                    }
                }
            }

            Log.d(TAG, "Retrieved progress for " + allProgress.size() + " scenarios");
            return allProgress;
        } catch (Exception e) {
            Log.e(TAG, "Error getting all scenario progress", e);
            return new HashMap<>();
        }
    }

    /**
     * 음성 설정 저장 (로컬 + 클라우드 동기화)
     */
    public void saveVoiceSettings(String voiceGender, float voiceSpeed, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            long timestamp = System.currentTimeMillis();

            // 1. 로컬 캐시에 즉시 저장 (빠른 응답)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getUserKey("voice_gender"), voiceGender);
            editor.putFloat(getUserKey("voice_speed"), voiceSpeed);
            editor.putLong(getUserKey("voice_settings_updated"), timestamp);
            editor.apply();

            Log.d(TAG, "Voice settings saved to local cache for user: " + userId);

            // 2. Firestore에 클라우드 동기화
            Map<String, Object> settings = new HashMap<>();
            settings.put("voice_gender", voiceGender);
            settings.put("voice_speed", voiceSpeed);
            settings.put("updated_at", timestamp);

            firestore.collection("users").document(userId)
                    .collection("settings").document("voice")
                    .set(settings, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Voice settings synced to cloud");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Cloud sync failed, but local save succeeded", e);
                        // 로컬 저장은 성공했으므로 성공으로 처리
                        callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving voice settings", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 음성 설정 가져오기
     */
    public Map<String, Object> getVoiceSettings() {
        String userId = getCurrentUserId();
        Map<String, Object> settings = new HashMap<>();

        if (userId == null) {
            // 로그인하지 않은 경우 기본값 반환
            settings.put("voice_gender", "female");
            settings.put("voice_speed", 1.0f);
            return settings;
        }

        try {
            String voiceGender = prefs.getString(getUserKey("voice_gender"), "female");
            float voiceSpeed = prefs.getFloat(getUserKey("voice_speed"), 1.0f);

            settings.put("voice_gender", voiceGender);
            settings.put("voice_speed", voiceSpeed);

            Log.d(TAG, "Voice settings retrieved for user: " + userId);
            return settings;
        } catch (Exception e) {
            Log.e(TAG, "Error getting voice settings", e);
            // 에러 발생 시 기본값 반환
            settings.put("voice_gender", "female");
            settings.put("voice_speed", 1.0f);
            return settings;
        }
    }

    /**
     * 클라우드에서 음성 설정 동기화 (로그인 시 호출)
     */
    public void syncVoiceSettingsFromCloud(OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "syncVoiceSettingsFromCloud: User not logged in");
            callback.onError("User not logged in");
            return;
        }

        Log.d(TAG, "Starting voice settings sync from cloud for user: " + userId);

        firestore.collection("users").document(userId)
                .collection("settings").document("voice")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            String voiceGender = documentSnapshot.getString("voice_gender");
                            Double voiceSpeedDouble = documentSnapshot.getDouble("voice_speed");
                            Long cloudTimestamp = documentSnapshot.getLong("updated_at");

                            if (voiceGender != null && voiceSpeedDouble != null) {
                                // 로컬 타임스탬프 확인
                                long localTimestamp = prefs.getLong(getUserKey("voice_settings_updated"), 0);

                                // 클라우드 데이터가 더 최신이거나 로컬 데이터가 없는 경우 업데이트
                                if (cloudTimestamp == null || cloudTimestamp > localTimestamp) {
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString(getUserKey("voice_gender"), voiceGender);
                                    editor.putFloat(getUserKey("voice_speed"), voiceSpeedDouble.floatValue());
                                    editor.putLong(getUserKey("voice_settings_updated"),
                                            cloudTimestamp != null ? cloudTimestamp : System.currentTimeMillis());
                                    editor.apply();

                                    Log.d(TAG, "Voice settings synced from cloud to local");
                                } else {
                                    Log.d(TAG, "Local voice settings are up to date");
                                }
                                callback.onSuccess();
                            } else {
                                Log.d(TAG, "Cloud voice settings incomplete, using local cache");
                                callback.onSuccess();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing cloud settings, using local cache", e);
                            callback.onSuccess();
                        }
                    } else {
                        Log.d(TAG, "No cloud voice settings found, using local cache");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to sync from cloud, using local cache", e);
                    callback.onSuccess();
                });
    }

    /**
     * 클라우드에서 레벨 테스트 결과 동기화 (로그인 시 호출)
     * Firestore → 로컬 SharedPreferences
     */
    public void syncLevelTestFromCloud(OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "syncLevelTestFromCloud: User not logged in");
            callback.onError("User not logged in");
            return;
        }

        Log.d(TAG, "Starting level test sync from cloud for user: " + userId);

        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // 클라우드 타임스탬프 확인
                            Long cloudTimestamp = documentSnapshot.getLong("level_test_date");
                            long localTimestamp = prefs.getLong(getUserKey("level_test_date"), 0);

                            // 클라우드 데이터가 더 최신이거나 로컬 데이터가 없는 경우 업데이트
                            if (cloudTimestamp != null && cloudTimestamp > localTimestamp) {
                                SharedPreferences.Editor editor = prefs.edit();

                                // 기본 레벨 테스트 데이터
                                String level = documentSnapshot.getString("level");
                                if (level != null) {
                                    editor.putString(getUserKey("level"), level);
                                }

                                Long testScore = documentSnapshot.getLong("test_score");
                                if (testScore != null) {
                                    editor.putInt(getUserKey("test_score"), testScore.intValue());
                                }

                                Boolean levelTestCompleted = documentSnapshot.getBoolean("level_test_completed");
                                if (levelTestCompleted != null) {
                                    editor.putBoolean(getUserKey("level_test_completed"), levelTestCompleted);
                                }

                                Boolean isConversationTest = documentSnapshot.getBoolean("is_conversation_test");
                                if (isConversationTest != null) {
                                    editor.putBoolean(getUserKey("is_conversation_test"), isConversationTest);
                                }

                                // 대화형 테스트 세부 점수
                                Long grammarScore = documentSnapshot.getLong("grammar_score");
                                if (grammarScore != null) {
                                    editor.putInt(getUserKey("grammar_score"), grammarScore.intValue());
                                }

                                Long vocabularyScore = documentSnapshot.getLong("vocabulary_score");
                                if (vocabularyScore != null) {
                                    editor.putInt(getUserKey("vocabulary_score"), vocabularyScore.intValue());
                                }

                                Long complexityScore = documentSnapshot.getLong("complexity_score");
                                if (complexityScore != null) {
                                    editor.putInt(getUserKey("complexity_score"), complexityScore.intValue());
                                }

                                Long communicationScore = documentSnapshot.getLong("communication_score");
                                if (communicationScore != null) {
                                    editor.putInt(getUserKey("communication_score"), communicationScore.intValue());
                                }

                                // 퀴즈 테스트 데이터 (퀴즈 방식일 경우)
                                Long correctAnswers = documentSnapshot.getLong("correct_answers");
                                if (correctAnswers != null) {
                                    editor.putInt(getUserKey("test_correct_answers"), correctAnswers.intValue());
                                }

                                Long totalQuestions = documentSnapshot.getLong("total_questions");
                                if (totalQuestions != null) {
                                    editor.putInt(getUserKey("test_total_questions"), totalQuestions.intValue());
                                }

                                editor.putLong(getUserKey("level_test_date"), cloudTimestamp);
                                editor.apply();

                                Log.d(TAG, "Level test data synced from cloud to local. Level: " + level);
                            } else {
                                Log.d(TAG, "Local level test data is up to date");
                            }
                            callback.onSuccess();
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing cloud level test data, using local cache", e);
                            callback.onSuccess();
                        }
                    } else {
                        Log.d(TAG, "No cloud level test data found, using local cache");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to sync level test from cloud, using local cache", e);
                    callback.onSuccess();
                });
    }

    /**
     * 클라우드에서 온보딩 데이터 동기화 (로그인 시 호출)
     * Firestore → 로컬 SharedPreferences
     * 관심사, 학습 목표, 온보딩 완료 상태 동기화
     */
    public void syncOnboardingDataFromCloud(OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "syncOnboardingDataFromCloud: User not logged in");
            callback.onError("User not logged in");
            return;
        }

        Log.d(TAG, "Starting onboarding data sync from cloud for user: " + userId);

        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            SharedPreferences.Editor editor = prefs.edit();

                            // 관심사 동기화
                            String interests = documentSnapshot.getString("interests");
                            if (interests != null && !interests.isEmpty()) {
                                editor.putString(getUserKey("interests"), interests);
                                Log.d(TAG, "Synced interests from cloud: " + interests);
                            }

                            Long interestsUpdated = documentSnapshot.getLong("interests_updated");
                            if (interestsUpdated != null) {
                                editor.putLong(getUserKey("interests_updated"), interestsUpdated);
                            }

                            // 학습 목표 동기화
                            String learningGoal = documentSnapshot.getString("learning_goal");
                            if (learningGoal != null && !learningGoal.isEmpty()) {
                                editor.putString(getUserKey("learning_goal"), learningGoal);
                                Log.d(TAG, "Synced learning goal from cloud: " + learningGoal);
                            }

                            Long goalUpdated = documentSnapshot.getLong("learning_goal_updated");
                            if (goalUpdated != null) {
                                editor.putLong(getUserKey("learning_goal_updated"), goalUpdated);
                            }

                            // 온보딩 완료 상태 동기화
                            Boolean onboardingCompleted = documentSnapshot.getBoolean("onboarding_completed");
                            if (onboardingCompleted != null) {
                                editor.putBoolean("onboarding_completed", onboardingCompleted);
                                Log.d(TAG, "Synced onboarding status from cloud: " + onboardingCompleted);
                            }

                            editor.apply();
                            Log.d(TAG, "Onboarding data synced from cloud to local");
                            callback.onSuccess();
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing cloud onboarding data", e);
                            callback.onSuccess();
                        }
                    } else {
                        Log.d(TAG, "No cloud onboarding data found");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to sync onboarding data from cloud", e);
                    callback.onSuccess();
                });
    }

    /**
     * 사용자 데이터 삭제 (계정 탈퇴 시)
     * Firestore 데이터와 로컬 데이터 모두 삭제
     */
    public void deleteUserData(OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        Log.d(TAG, "Starting complete user data deletion for user: " + userId);

        // Firestore 사용자 문서 참조
        com.google.firebase.firestore.DocumentReference userDocRef = firestore.collection("users").document(userId);

        // 1. 서브컬렉션들 삭제 (conversation_logs, progress, settings)
        deleteSubCollection(userDocRef, "conversation_logs", () -> {
            deleteSubCollection(userDocRef, "progress", () -> {
                deleteSubCollection(userDocRef, "settings", () -> {
                    // 2. 메인 사용자 문서 삭제
                    userDocRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Firestore user document deleted successfully");

                                // 3. 로컬 SharedPreferences 데이터 삭제
                                deleteLocalUserData(userId);

                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete Firestore user document", e);
                                // Firestore 삭제 실패해도 로컬 데이터는 삭제
                                deleteLocalUserData(userId);
                                callback.onSuccess();
                            });
                });
            });
        });
    }

    /**
     * Firestore 서브컬렉션 삭제
     */
    private void deleteSubCollection(com.google.firebase.firestore.DocumentReference parentDoc,
                                     String collectionName, Runnable onComplete) {
        parentDoc.collection(collectionName).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "Sub-collection " + collectionName + " is empty or doesn't exist");
                        onComplete.run();
                        return;
                    }

                    // 배치로 모든 문서 삭제
                    com.google.firebase.firestore.WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Sub-collection " + collectionName + " deleted (" +
                                        querySnapshot.size() + " documents)");
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete sub-collection " + collectionName, e);
                                onComplete.run(); // 실패해도 계속 진행
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get sub-collection " + collectionName, e);
                    onComplete.run(); // 실패해도 계속 진행
                });
    }

    /**
     * 로컬 SharedPreferences 사용자 데이터 삭제
     */
    private void deleteLocalUserData(String userId) {
        try {
            SharedPreferences.Editor editor = prefs.edit();

            // 해당 사용자의 모든 키를 찾아서 삭제
            Map<String, ?> allPrefs = prefs.getAll();
            String userPrefix = userId + "_";

            for (String key : allPrefs.keySet()) {
                if (key.startsWith(userPrefix)) {
                    editor.remove(key);
                }
            }

            editor.apply();
            Log.d(TAG, "Local user data deleted successfully for user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting local user data", e);
        }
    }

    // ========== 일일 학습 진도 (사용자별) ==========

    /**
     * 일일 학습 진도 가져오기 (사용자별)
     */
    public Map<String, Object> getDailyProgress() {
        String userId = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();

        if (userId == null) {
            result.put("daily_completed", 0);
            result.put("daily_goal", 5);
            result.put("last_learning_date", "");
            return result;
        }

        try {
            int dailyCompleted = prefs.getInt(getUserKey("daily_completed"), 0);
            int dailyGoal = prefs.getInt(getUserKey("daily_goal"), 5);
            String lastLearningDate = prefs.getString(getUserKey("last_learning_date"), "");

            result.put("daily_completed", dailyCompleted);
            result.put("daily_goal", dailyGoal);
            result.put("last_learning_date", lastLearningDate);

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting daily progress", e);
            result.put("daily_completed", 0);
            result.put("daily_goal", 5);
            result.put("last_learning_date", "");
            return result;
        }
    }

    /**
     * 일일 학습 진도 업데이트 (사용자별 + Firestore 동기화)
     */
    public void updateDailyProgress(int dailyCompleted, String date, OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        try {
            int dailyGoal = prefs.getInt(getUserKey("daily_goal"), 5);

            // 로컬에 저장 (사용자별 키)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(getUserKey("daily_completed"), dailyCompleted);
            editor.putString(getUserKey("last_learning_date"), date);
            editor.apply();

            Log.d(TAG, "Daily progress saved to local for user: " + userId + " - " + dailyCompleted + "/" + dailyGoal);

            // Firestore에 동기화
            Map<String, Object> dailyData = new HashMap<>();
            dailyData.put("daily_completed", dailyCompleted);
            dailyData.put("daily_goal", dailyGoal);
            dailyData.put("last_learning_date", date);
            dailyData.put("updated_at", System.currentTimeMillis());

            firestore.collection("users").document(userId)
                    .set(dailyData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Daily progress synced to Firestore");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for daily progress", e);
                        callback.onSuccess(); // 로컬 저장 성공
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error updating daily progress", e);
            callback.onError(e.getMessage());
        }
    }

    /**
     * 일일 학습 진도 초기화 (날짜 변경 시)
     */
    public void resetDailyProgress(String newDate) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(getUserKey("daily_completed"), 0);
        editor.putString(getUserKey("last_learning_date"), newDate);
        editor.apply();

        Log.d(TAG, "Daily progress reset for new day: " + newDate);
    }

    // ========== 달력 학습 완료일 (사용자별) ==========

    /**
     * 학습 완료일 목록 가져오기 (사용자별)
     */
    public java.util.Set<String> getLearningDays() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new java.util.HashSet<>();
        }

        java.util.Set<String> learningDays = prefs.getStringSet(getUserKey("learning_days"), new java.util.HashSet<>());
        return new java.util.HashSet<>(learningDays);
    }

    /**
     * 오늘 학습 완료 표시 (사용자별 + Firestore 동기화)
     */
    public void markTodayAsLearned(OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in");
            return;
        }

        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String today = dateFormat.format(new java.util.Date());

            // 기존 학습일 목록 가져오기
            java.util.Set<String> learningDays = prefs.getStringSet(getUserKey("learning_days"), new java.util.HashSet<>());
            learningDays = new java.util.HashSet<>(learningDays);
            learningDays.add(today);

            // 로컬에 저장
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(getUserKey("learning_days"), learningDays);
            editor.apply();

            Log.d(TAG, "Marked today as learned for user: " + userId + " - " + today);

            // Firestore에 동기화
            Map<String, Object> calendarData = new HashMap<>();
            calendarData.put("learning_days", new java.util.ArrayList<>(learningDays));
            calendarData.put("updated_at", System.currentTimeMillis());

            firestore.collection("users").document(userId)
                    .set(calendarData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Learning days synced to Firestore");
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore sync failed for learning days", e);
                        if (callback != null) callback.onSuccess();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error marking today as learned", e);
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    // ========== 피드백 기록 (장점/약점 저장) ==========

    /**
     * 레슨 피드백 저장 (장점, 약점을 Firestore에 저장)
     * @param scenarioId 시나리오 ID
     * @param strengths 장점 목록
     * @param weaknesses 약점 목록
     * @param callback 콜백
     */
    public void saveLessonFeedback(String scenarioId, List<String> strengths, List<String> weaknesses,
                                   OperationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in");
            return;
        }

        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            String timestamp = dateFormat.format(new java.util.Date());

            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("scenario_id", scenarioId);
            feedbackData.put("strengths", strengths);
            feedbackData.put("weaknesses", weaknesses);
            feedbackData.put("timestamp", timestamp);
            feedbackData.put("created_at", System.currentTimeMillis());

            // Firestore에 피드백 기록 저장 (서브컬렉션)
            firestore.collection("users").document(userId)
                    .collection("feedback_history")
                    .add(feedbackData)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "Feedback saved to Firestore: " + docRef.getId());
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save feedback to Firestore", e);
                        if (callback != null) callback.onError(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error saving lesson feedback", e);
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    /**
     * 피드백 기록 콜백 인터페이스
     */
    public interface FeedbackHistoryCallback {
        void onSuccess(List<Map<String, Object>> feedbackList);
        void onError(String error);
    }

    /**
     * 피드백 기록 가져오기 (최근 N개)
     * @param limit 가져올 최대 개수
     * @param callback 콜백
     */
    public void getFeedbackHistory(int limit, FeedbackHistoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        firestore.collection("users").document(userId)
                .collection("feedback_history")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> feedbackList = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            feedbackList.add(data);
                        }
                    }
                    Log.d(TAG, "Retrieved " + feedbackList.size() + " feedback records");
                    callback.onSuccess(feedbackList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get feedback history", e);
                    callback.onError(e.getMessage());
                });
    }

    /**
     * 전체 피드백 요약 데이터 가져오기 (약점/장점 통계)
     * @param callback 콜백
     */
    public void getFeedbackSummary(FeedbackHistoryCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        // 최근 30개의 피드백을 가져와서 통계 분석
        firestore.collection("users").document(userId)
                .collection("feedback_history")
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 장점/약점 빈도 분석을 위한 Map
                    Map<String, Integer> strengthFrequency = new HashMap<>();
                    Map<String, Integer> weaknessFrequency = new HashMap<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            // 장점 집계
                            List<String> strengths = (List<String>) data.get("strengths");
                            if (strengths != null) {
                                for (String strength : strengths) {
                                    strengthFrequency.put(strength,
                                            strengthFrequency.getOrDefault(strength, 0) + 1);
                                }
                            }

                            // 약점 집계
                            List<String> weaknesses = (List<String>) data.get("weaknesses");
                            if (weaknesses != null) {
                                for (String weakness : weaknesses) {
                                    weaknessFrequency.put(weakness,
                                            weaknessFrequency.getOrDefault(weakness, 0) + 1);
                                }
                            }
                        }
                    }

                    // 결과를 Map에 담아서 반환
                    List<Map<String, Object>> result = new java.util.ArrayList<>();
                    Map<String, Object> summaryData = new HashMap<>();
                    summaryData.put("total_feedbacks", querySnapshot.size());
                    summaryData.put("strength_frequency", strengthFrequency);
                    summaryData.put("weakness_frequency", weaknessFrequency);
                    result.add(summaryData);

                    Log.d(TAG, "Feedback summary generated: " + querySnapshot.size() + " records analyzed");
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get feedback summary", e);
                    callback.onError(e.getMessage());
                });
    }
}
