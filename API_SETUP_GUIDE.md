# 🔑 API 설정 가이드

Just Speak 앱을 사용하기 위해 필요한 API 키를 설정하는 방법입니다.

## 📋 필요한 API

### 1. Google Gemini API (필수)
- **용도**: AI 대화 생성, 문법 분석, 어휘 피드백
- **가격**: 무료 (월 60 requests/min)

### 2. Azure Speech Service (선택)
- **용도**: 발음 평가
- **가격**: 매월 5시간 무료, 이후 $1/시간
- **참고**: 없어도 앱 사용 가능 (기본 음성 인식 사용)

---

## 🚀 1. Google Gemini API 키 발급

### Step 1: Google AI Studio 접속
1. [Google AI Studio](https://makersuite.google.com/app/apikey) 접속
2. Google 계정으로 로그인

### Step 2: API 키 생성
1. 좌측 메뉴에서 **"Get API key"** 클릭
2. **"Create API key"** 버튼 클릭
3. 프로젝트 선택 또는 **"Create API key in new project"** 선택
4. 생성된 API 키 복사 (예: `AIzaSyD...`)

### Step 3: 앱에 설정
```java
// ConversationActivity.java 또는 설정에서
private static final String GEMINI_API_KEY = "여기에_발급받은_키_붙여넣기";
```

---

## 🎤 2. Azure Speech Service 키 발급 (선택사항)

### Step 1: Azure Portal 접속
1. [Azure Portal](https://portal.azure.com/) 접속
2. Microsoft 계정으로 로그인 (없으면 무료 계정 생성)

### Step 2: Speech Service 리소스 생성
1. 검색창에 **"Speech Services"** 입력
2. **"+ 만들기"** 클릭
3. 다음 정보 입력:
   - **구독**: Azure subscription 1 (무료)
   - **리소스 그룹**: 새로 만들기 → `JustSpeakResources`
   - **지역**: `Korea Central` (한국)
   - **이름**: `justspeak-speech`
   - **가격 책정 계층**: `Free F0` (무료)
4. **"검토 + 만들기"** → **"만들기"** 클릭

### Step 3: 키 및 엔드포인트 확인
1. 생성된 리소스로 이동
2. 좌측 메뉴에서 **"키 및 엔드포인트"** 클릭
3. 다음 정보 복사:
   - **KEY 1**: (예: `a1b2c3d4...`)
   - **지역**: (예: `koreacentral`)

### Step 4: 앱에 설정
```java
// ConversationActivity.java 또는 설정에서
private static final String AZURE_SPEECH_KEY = "여기에_KEY_1_붙여넣기";
private static final String AZURE_REGION = "koreacentral";
```

---

## ⚙️ 3. 앱에서 API 키 설정하는 방법

### 방법 1: 코드에 직접 설정 (간단, 권장)

**파일**: `ConversationActivity.java`

```java
public class ConversationActivity extends AppCompatActivity {
    // ========== API 키 설정 ==========
    private static final String GEMINI_API_KEY = "여기에_Gemini_키_입력";
    private static final String AZURE_SPEECH_KEY = "여기에_Azure_키_입력"; // 선택사항
    private static final String AZURE_REGION = "koreacentral"; // Azure 지역
    // =================================

    // ... 나머지 코드
}
```

### 방법 2: 설정 화면에서 입력 (안전)

앱 실행 후:
1. 메인 화면 → 우측 상단 **설정 아이콘** 클릭
2. **"AI 설정"** 선택
3. API 키 입력 (향후 구현 예정)

---

## 🧪 4. 테스트

### Gemini API 테스트
1. 앱 실행
2. 로그인/회원가입
3. 대화 시나리오 선택 (예: 일상대화)
4. 마이크 버튼 눌러 영어로 말하기
5. AI 응답 확인

**예시 대화:**
```
You: Hi, how are you?
AI: I'm doing great, thank you! How about you?
```

### Azure 발음 평가 테스트
1. 대화 중 영어로 말하기
2. 화면 하단에 발음 점수 확인
   - 정확도 (Accuracy)
   - 유창성 (Fluency)
   - 완성도 (Completeness)

---

## ❗ 문제 해결

### "API 키 오류" 발생 시
1. API 키를 정확히 복사했는지 확인
2. 공백이나 줄바꿈이 포함되지 않았는지 확인
3. Gemini API: [https://makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)에서 키 상태 확인

### "발음 평가 실패" 발생 시
1. Azure Speech Service 무료 할당량 확인
2. 인터넷 연결 확인
3. 마이크 권한 허용 확인

### 빌드 오류 발생 시
1. Android Studio에서 **"Build → Clean Project"**
2. **"Build → Rebuild Project"**
3. Gradle Sync 실행

---

## 💰 비용 관리

### Google Gemini (무료)
- 월 60 requests/분까지 무료
- 학생 프로젝트로는 충분

### Azure Speech (무료 티어)
- 월 5시간 무료
- 시간당 평균 30회 대화 가능
- 총 150회 대화/월 무료

**팁**: Azure 없이도 앱 사용 가능! 기본 Android 음성 인식 사용됩니다.

---

## 📧 문의

문제가 해결되지 않으면:
- 이메일: support@justspeak.com
- GitHub Issues: [프로젝트 저장소]

---

**작성자**: 정선재 (202101801)
