# Just Speak - AI 영어 학습 앱

AI 기반 영어 회화 학습 안드로이드 앱입니다. Google Gemini AI와 대화하며 영어 실력을 향상시킬 수 있습니다.

## 주요 기능

- **AI 영어 회화**: Gemini AI와 실시간 영어 대화
- **음성 인식**: 사용자 음성을 텍스트로 변환
- **TTS (Text-to-Speech)**: AI 응답을 자연스러운 음성으로 출력
- **레벨 테스트**: 사용자의 영어 수준 측정
- **맞춤형 학습**: 관심 분야 및 학습 목표에 따른 커리큘럼
- **학습 진도 추적**: 일별/주별 학습 현황 확인

## 학습 카테고리

- 학술 (Academic)
- 일상 (Daily)
- 여행 (Travel)
- 면접 (Interview)
- 자기계발 (Self-improvement)
- 비즈니스 (Business)

## 학습 목표

- 스피킹
- 발음
- 듣기
- 문법
- 어휘

## 기술 스택

- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **AI**: Google Gemini API
- **TTS**: Google Cloud Text-to-Speech
- **인증**: Firebase Authentication (Google Sign-In)
- **데이터베이스**: Firebase Firestore

## 설정 방법

### 1. 프로젝트 클론

```bash
git clone https://github.com/jaysj0226/Learning-English-AI-App.git
```

### 2. API 키 설정

프로젝트 루트에 `local.properties` 파일을 생성하고 다음 내용을 추가하세요:

```properties
GEMINI_API_KEY=your_gemini_api_key
GOOGLE_CLOUD_TTS_KEY=your_google_cloud_tts_key
```

### 3. Firebase 설정

1. [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
2. Android 앱 추가 (패키지명: `com.cookandroid.justspeakapp`)
3. `google-services.json` 파일 다운로드 후 `app/` 폴더에 배치
4. Authentication에서 Google 로그인 활성화
5. Firestore Database 생성

### 4. 빌드 및 실행

Android Studio에서 프로젝트를 열고 실행하세요.

---

## 📁 프로젝트 구조

```
JustSpeakApp/
├── app/src/main/
│   ├── java/com/cookandroid/justspeakapp/
│   │   ├── 📱 Activity
│   │   │   ├── SplashActivity.java (온보딩)
│   │   │   ├── LoginActivity.java
│   │   │   ├── SignupActivity.java
│   │   │   ├── InterestSelectionActivity.java
│   │   │   ├── LearningGoalActivity.java
│   │   │   ├── MainActivity.java
│   │   │   ├── ConversationActivity.java (기본)
│   │   │   ├── ConversationActivityWithAI.java (AI 버전) ⭐
│   │   │   └── SettingsActivity.java
│   │   ├── 🤖 Service
│   │   │   ├── GeminiService.java (Google Gemini AI)
│   │   │   ├── AzureSpeechService.java (발음 평가)
│   │   │   ├── SpeechRecognitionService.java
│   │   │   └── TextToSpeechService.java
│   │   ├── 📦 Model
│   │   │   ├── ConversationMessage.java
│   │   │   ├── PronunciationFeedback.java
│   │   │   ├── Scenario.java
│   │   │   ├── LearningProgress.java
│   │   │   └── GrammarError.java
│   │   └── 🎨 Adapter
│   │       └── ConversationAdapter.java
│   ├── res/
│   │   ├── layout/ (8개 Activity 레이아웃)
│   │   ├── drawable/ (18개 아이콘)
│   │   ├── values/ (colors, strings)
│   │   └── ...
│   └── AndroidManifest.xml
├── API_SETUP_GUIDE.md 📖
```

---


## 라이선스

This project is for educational purposes.
