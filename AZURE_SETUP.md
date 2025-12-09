# Azure Speech Service 설정 가이드

JustSpeakApp에서 Azure Pronunciation Assessment를 사용하려면 아래 단계를 따르세요.

## 🎯 목표
- Azure 무료 계정 생성 (월 5시간 무료)
- Speech Service 리소스 생성
- API 키 및 리전 설정

---

## 📋 1단계: Azure 계정 생성

### 1.1 Azure 무료 체험 신청
1. https://azure.microsoft.com/free/ 접속
2. "무료로 시작" 클릭
3. Microsoft 계정으로 로그인 (없으면 새로 생성)
4. 신용카드 등록 (무료 기간 동안 청구 없음, 확인용)

### 1.2 무료 혜택
- ✅ **월 5시간 무료** Speech 서비스 (Standard tier)
- ✅ 12개월 동안 $200 크레딧
- ✅ 신용카드 청구 없음 (사용량 초과 시 알림)

---

## 🔧 2단계: Speech Service 리소스 생성

### 2.1 Azure Portal 접속
1. https://portal.azure.com 로그인
2. 좌측 메뉴에서 "리소스 만들기" 클릭

### 2.2 Speech Service 검색 및 생성
1. 검색창에 **"Speech"** 입력
2. **"Speech Services"** 선택
3. "만들기" 클릭

### 2.3 리소스 설정
아래 정보를 입력하세요:

| 항목 | 값 |
|------|-----|
| **구독** | Free Trial 또는 본인 구독 |
| **리소스 그룹** | 새로 만들기 → "JustSpeakApp-Resources" |
| **지역** | **Korea Central** (한국 중부) 추천<br>또는 East US, West Europe |
| **이름** | "justspeakapp-speech" (고유한 이름) |
| **가격 책정 계층** | **Free F0** (월 5시간 무료)<br>또는 **Standard S0** (무료 크레딧 사용) |

4. "검토 + 만들기" → "만들기" 클릭
5. 배포 완료까지 1-2분 대기

---

## 🔑 3단계: API 키 및 리전 확인

### 3.1 리소스로 이동
1. 배포 완료 후 "리소스로 이동" 클릭
2. 또는 Azure Portal → "모든 리소스" → "justspeakapp-speech" 선택

### 3.2 키 및 엔드포인트 복사
1. 좌측 메뉴에서 **"키 및 엔드포인트"** 클릭
2. 아래 정보를 복사하세요:

```
KEY 1: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
LOCATION: koreacentral
```

**⚠️ 주의:** KEY 1과 KEY 2 중 하나만 사용하면 됩니다. 키는 절대 공개하지 마세요!

---

## 💻 4단계: 앱에 API 키 설정

### 4.1 AzureConfig.java 파일 열기
경로: `app/src/main/java/com/cookandroid/justspeakapp/config/AzureConfig.java`

### 4.2 API 키 및 리전 입력
```java
public class AzureConfig {
    // Azure Portal에서 복사한 값을 아래에 붙여넣으세요
    public static final String SPEECH_SUBSCRIPTION_KEY = "여기에_KEY_1_붙여넣기";
    public static final String SPEECH_REGION = "koreacentral"; // 또는 본인의 리전

    // ... 나머지 코드는 그대로 유지
}
```

### 4.3 예시
```java
public static final String SPEECH_SUBSCRIPTION_KEY = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
public static final String SPEECH_REGION = "koreacentral";
```

---

## ✅ 5단계: 테스트

### 5.1 앱 빌드 및 실행
```bash
./gradlew build
```

### 5.2 테스트 방법
1. 앱 실행
2. 학습 시나리오 선택 (예: 일상대화)
3. 마이크 버튼 클릭
4. AI가 말한 문장을 따라 말하기
5. 발음 점수 확인

### 5.3 성공 확인
- ✅ "Azure 발음 평가 사용 중" 토스트 메시지 표시
- ✅ 발음 점수 (Accuracy, Fluency, Completeness)
- ✅ 잘못 발음한 단어 피드백

---

## 🐛 문제 해결

### 문제 1: "Azure Speech Service가 설정되지 않았습니다"
**원인:** API 키가 설정되지 않음

**해결:**
1. AzureConfig.java 파일 확인
2. SPEECH_SUBSCRIPTION_KEY가 "YOUR_AZURE_SPEECH_KEY_HERE"인지 확인
3. Azure Portal에서 올바른 키를 복사했는지 확인

---

### 문제 2: "음성 인식이 취소되었습니다"
**원인:** 네트워크 오류 또는 잘못된 리전

**해결:**
1. 인터넷 연결 확인
2. SPEECH_REGION이 Azure Portal의 LOCATION과 일치하는지 확인
   - Azure: "Korea Central" → 코드: "koreacentral"
   - Azure: "East US" → 코드: "eastus"

---

### 문제 3: "기본 음성 인식 사용 중 (Azure 미설정)"
**원인:** Azure 설정이 올바르지 않음

**해결:**
1. AzureConfig.isConfigured() 확인
2. API 키와 리전이 모두 설정되었는지 확인
3. 앱 재빌드 및 재실행

---

## 📊 사용량 모니터링

### Azure Portal에서 확인
1. Azure Portal → "Speech Services 리소스"
2. "메트릭" 탭 클릭
3. 사용 시간 및 요청 수 확인

### 무료 한도
- Free F0: **월 5시간** (최대 18,000초)
- 초과 시 자동으로 요청 차단 (과금 없음)

---

## 💡 추가 정보

### 리전 목록
| Azure 리전 이름 | 코드 |
|----------------|------|
| Korea Central | koreacentral |
| East US | eastus |
| West US | westus |
| West Europe | westeurope |
| Southeast Asia | southeastasia |

### 비용 최적화 팁
1. **개발 중:** Free F0 사용 (월 5시간 무료)
2. **테스트:** Standard S0 + 무료 크레딧 사용
3. **프로덕션:** 사용량에 따라 Standard S0 유료 전환

---

## 🔒 보안 주의사항

### ⚠️ 절대 하지 마세요
- ❌ API 키를 GitHub에 커밋하지 마세요
- ❌ API 키를 공개 장소에 공유하지 마세요
- ❌ 앱 APK에 하드코딩하지 마세요 (디컴파일 위험)

### ✅ 권장 사항
- ✅ `.gitignore`에 AzureConfig.java 추가
- ✅ 환경 변수 또는 secure storage 사용 고려
- ✅ 키가 노출되면 즉시 재생성

---

## 🎉 완료!

이제 Azure Pronunciation Assessment를 사용할 준비가 되었습니다!

문제가 있으면 Azure 문서를 참고하세요:
- https://learn.microsoft.com/azure/ai-services/speech-service/
- https://learn.microsoft.com/azure/ai-services/speech-service/how-to-pronunciation-assessment
