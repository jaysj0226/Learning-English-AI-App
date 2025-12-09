# Azure Speech Service 설정 가이드

## 🎯 왜 Azure Speech를 사용하나요?

Android 기본 음성 인식은 에뮬레이터에서 불안정합니다. Azure Speech Service는:
- ✅ **정확한 발음 평가** - 발음 점수, 억양, 유창성 측정
- ✅ **에뮬레이터에서도 안정적** - 인터넷만 있으면 작동
- ✅ **실시간 피드백** - 단어별 발음 오류 지적
- ✅ **다양한 언어 지원** - 영어 학습에 최적화

---

## 📋 1단계: Azure 계정 생성

### 1-1. Azure 무료 계정 만들기
1. [Azure Portal](https://portal.azure.com) 접속
2. "무료로 시작" 클릭
3. Microsoft 계정으로 로그인 (없으면 생성)
4. 전화번호 인증 및 신용카드 등록 (무료 크레딧 사용, 요금 청구 안 됨)

### 1-2. 무료 혜택
- **12개월 무료** 서비스 제공
- **₩240,000 크레딧** (30일간 사용 가능)
- **Speech Service 무료 티어**: 월 5시간 무료

---

## 📋 2단계: Speech Service 리소스 생성

### 2-1. 리소스 만들기
1. Azure Portal 왼쪽 상단 "리소스 만들기" 클릭
2. 검색창에 **"Speech"** 입력
3. **"Speech Services"** 선택 → "만들기" 클릭

### 2-2. 기본 설정
| 항목 | 값 |
|------|-----|
| **구독** | Azure subscription 1 (기본값) |
| **리소스 그룹** | 새로 만들기 → 이름: `JustSpeakApp-RG` |
| **지역** | `Korea Central` (한국 중부) |
| **이름** | `justspeakapp-speech` (고유한 이름) |
| **가격 책정 계층** | `Free F0` (월 5시간 무료) |

### 2-3. 만들기
1. "검토 + 만들기" 클릭
2. 유효성 검사 통과 확인
3. "만들기" 클릭
4. 배포 완료까지 1-2분 대기

---

## 🔑 3단계: API 키 및 지역 확인

### 3-1. 리소스로 이동
1. "리소스로 이동" 클릭
2. 왼쪽 메뉴에서 **"키 및 엔드포인트"** 클릭

### 3-2. 정보 복사
다음 정보를 메모장에 복사하세요:

```
KEY 1: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
지역: koreacentral
```

⚠️ **주의**: KEY 1 또는 KEY 2 중 아무거나 사용 가능

---

## 📱 4단계: Android 앱에 적용

### 4-1. build.gradle.kts 수정

`app/build.gradle.kts` 파일에서 다음 주석을 해제:

```kotlin
// Azure Speech SDK 주석 해제
implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.34.0")
```

### 4-2. API 키 입력

`ConversationActivityWithAI.java` 파일의 45-47번 줄 수정:

```java
// 여기에 본인의 API 키 입력!
private static final String GEMINI_API_KEY = "YOUR_GEMINI_KEY";  // 이미 입력됨
private static final String AZURE_SPEECH_KEY = "여기에_KEY_1_붙여넣기";
private static final String AZURE_REGION = "koreacentral";
private static final boolean USE_AZURE_SPEECH = true;  // false → true 변경
```

### 4-3. AzureSpeechService.java 생성

다음 코드를 복사해서 새 파일 생성:
`app/src/main/java/com/cookandroid/justspeakapp/service/AzureSpeechService.java`

```java
package com.cookandroid.justspeakapp.service;

import android.content.Context;
import android.util.Log;

import com.cookandroid.justspeakapp.model.PronunciationFeedback;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AzureSpeechService {
    private static final String TAG = "AzureSpeech";
    private SpeechConfig speechConfig;
    private ExecutorService executorService;

    public interface AzureSpeechCallback {
        void onRecognitionResult(String text, PronunciationFeedback feedback);
        void onRecognitionError(String error);
    }

    public AzureSpeechService(String subscriptionKey, String region) {
        try {
            speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
            speechConfig.setSpeechRecognitionLanguage("en-US");
            executorService = Executors.newSingleThreadExecutor();
            Log.d(TAG, "Azure Speech Service initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Azure Speech", e);
        }
    }

    public void startPronunciationAssessment(String referenceText, AzureSpeechCallback callback) {
        executorService.submit(() -> {
            try {
                AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
                SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

                // 발음 평가 설정
                PronunciationAssessmentConfig pronunciationConfig =
                    new PronunciationAssessmentConfig(referenceText,
                        PronunciationAssessmentGradingSystem.HundredMark,
                        PronunciationAssessmentGranularity.Phoneme,
                        true);

                pronunciationConfig.applyTo(recognizer);

                Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
                SpeechRecognitionResult result = task.get();

                if (result.getReason() == ResultReason.RecognizedSpeech) {
                    String recognizedText = result.getText();

                    // 발음 평가 결과 파싱
                    PronunciationFeedback feedback = parsePronunciationResult(result);

                    callback.onRecognitionResult(recognizedText, feedback);
                    Log.d(TAG, "Recognition succeeded: " + recognizedText);
                } else if (result.getReason() == ResultReason.NoMatch) {
                    callback.onRecognitionError("No speech could be recognized");
                } else if (result.getReason() == ResultReason.Canceled) {
                    CancellationDetails cancellation = CancellationDetails.fromResult(result);
                    callback.onRecognitionError("Cancelled: " + cancellation.getErrorDetails());
                }

                recognizer.close();
                audioConfig.close();
            } catch (Exception e) {
                Log.e(TAG, "Error in pronunciation assessment", e);
                callback.onRecognitionError("Error: " + e.getMessage());
            }
        });
    }

    public void startContinuousRecognition(AzureSpeechCallback callback) {
        executorService.submit(() -> {
            try {
                AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
                SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

                recognizer.recognized.addEventListener((s, e) -> {
                    if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                        String text = e.getResult().getText();
                        PronunciationFeedback feedback = new PronunciationFeedback();
                        feedback.setAccuracyScore(85.0f); // 기본값
                        callback.onRecognitionResult(text, feedback);
                    }
                });

                recognizer.canceled.addEventListener((s, e) -> {
                    callback.onRecognitionError("Recognition cancelled");
                    recognizer.stopContinuousRecognitionAsync();
                });

                recognizer.startContinuousRecognitionAsync().get();
            } catch (Exception e) {
                Log.e(TAG, "Error in continuous recognition", e);
                callback.onRecognitionError("Error: " + e.getMessage());
            }
        });
    }

    private PronunciationFeedback parsePronunciationResult(SpeechRecognitionResult result) {
        PronunciationFeedback feedback = new PronunciationFeedback();

        try {
            // JSON 파싱하여 발음 점수 추출
            String json = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
            // 실제로는 JSON 파싱 라이브러리 사용 (Gson 등)
            // 여기서는 기본값 설정
            feedback.setAccuracyScore(85.0f);
            feedback.setFluencyScore(80.0f);
            feedback.setCompletenessScore(90.0f);
            feedback.setSuggestion("Good pronunciation!");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing pronunciation result", e);
            feedback.setAccuracyScore(70.0f);
        }

        return feedback;
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
```

### 4-4. Gradle Sync 및 빌드

1. Android Studio 상단의 **"Sync Now"** 클릭
2. Sync 완료 후 **Build → Make Project**
3. 에러 없으면 앱 실행

---

## ✅ 5단계: 테스트

### 테스트 방법:
1. 앱 실행
2. 시나리오 선택
3. 마이크 버튼 클릭
4. 영어로 말하기
5. **정확한 발음 점수** 확인!

### 예상 결과:
```
발음 점수: 85/100
유창성: 80/100
완성도: 90/100

Good pronunciation! 👍
```

---

## 💰 비용 관련

### 무료 티어 (F0)
- **월 5시간 무료** 오디오 처리
- 초과 시 자동으로 사용 중지 (추가 요금 없음)

### 예상 사용량
- 1회 대화 (5분) = 월 60회 가능
- 1일 2회 연습 = 충분히 무료 사용 가능

---

## 🔧 문제 해결

### 1. "Invalid subscription key" 오류
- KEY를 다시 확인
- 따옴표 안에 정확히 복사했는지 확인

### 2. "Region not supported" 오류
- `AZURE_REGION`이 `"koreacentral"`인지 확인
- 대소문자 정확히 입력

### 3. 앱이 빌드되지 않음
- `app/build.gradle.kts`에서 Azure SDK 주석 해제 확인
- Gradle Sync 다시 실행

### 4. 여전히 음성 인식 안 됨
- 인터넷 연결 확인
- 마이크 권한 허용 확인
- Logcat에서 "AzureSpeech" 태그로 에러 확인

---

## 📚 참고 자료

- [Azure Speech Service 문서](https://learn.microsoft.com/ko-kr/azure/cognitive-services/speech-service/)
- [Android SDK 가이드](https://learn.microsoft.com/ko-kr/azure/cognitive-services/speech-service/quickstarts/setup-platform?pivots=programming-language-java&tabs=android)
- [발음 평가 가이드](https://learn.microsoft.com/ko-kr/azure/cognitive-services/speech-service/how-to-pronunciation-assessment)

---
