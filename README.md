# 로드메이트 (RoadMate)

졸음의 시작부터 끝까지 책임지는 AI 동승자.
졸리기 전에 먼저 말을 걸고, 쉬어야 할 땐 차를 낮잠 공간으로 바꿔 개운하게 깨워주는 Pleos Connect 차량용 앱.

## 저장소
https://github.com/PJJ0337/RoadMate

## 동작 흐름
1. **예측**: 연속 운전 시간·시간대·크루즈·운전자 조작·순정 졸음 경고·대답 여부로 졸음 위험 점수(0~100) 계산
2. **대화**: 주의 단계(40점~)면 로드메이트가 먼저 말을 걸고, 대답에 맞춰 같은 주제로 2번 더 이어서 대화 (대답이 없으면 위험 점수 상승)
3. **안내**: 위험 단계(70점~)거나 "쉬고 싶어"라고 하면 가까운 휴게소로 길 안내 + 시원한 바람
4. **파워냅**: 주차(P)하면 낮잠 모드 → 시간이 되면 기상 모드로 깨움

## 시연 방법
앱 하단 '시연 패널'에서 `운전 +30분`, `졸음 경고 켜기`, `기어 P → D`, `대답`, `졸음 구간으로 이동` 버튼으로 상황을 재현합니다.
`🗺 졸음 지도` 버튼으로 졸음 다발 구간 지도(예시 데이터)를 볼 수 있습니다.
(시연 모드: 말 걸기 간격 15초, 낮잠 30초)

## 실행 방법
1. Android Studio에서 `RoadMate` 폴더를 엽니다 (File > Open).
2. Gradle 동기화가 끝날 때까지 기다립니다.
3. Pleos Connect Emulator(AVD, v2.1.0 / API 34)를 실행합니다.
4. 상단 ▶ Run 버튼으로 `app`을 실행합니다.

## AI 대화 (선택)
Pleos LLM SDK가 에뮬레이터 v2.1.0(2026-10-19)부터 제공 종료되어, 외부 AI(Google Gemini API)로 자유 대화를 합니다.
- 키가 없으면 앱에 내장된 **기본 대본**으로 대화합니다 (인터넷 없이도 동작).
- 키를 쓰려면 프로젝트 루트의 `local.properties`(저장소에 올라가지 않음)에 아래 한 줄을 추가하고 다시 빌드합니다.
  ```
  GEMINI_API_KEY=발급받은_키
  # GEMINI_MODEL=gemini-flash-lite-latest   (선택, 기본값)
  ```
- AI 대답이 4초 안에 오지 않거나 실패하면 자동으로 기본 대본으로 대답합니다. "쉬고 싶어" 같은 휴식 요청은 AI보다 먼저 처리합니다.

## 테스트 환경
- Pleos Connect Emulator v2.1.0 (Android 14, API 34)
- Android Gradle Plugin 8.8.2 / Gradle 8.10.2 / Kotlin 2.0.0 / JDK 17
- minSdk 28, targetSdk 35
- Pleos Connect Virtual Console: `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/

## 사용 SDK
Vehicle 2.3.2 · TextToSpeech 3.3.1 · SpeechToText 3.3.1 · NaviHelper 3.0.5 · FusedLocation 1.1.3
(+ 선택: Google Gemini API — AI 자유 대화)

## APK 위치
`apk/팀명_로드메이트_release.apk` (제출 전 추가 예정)

## 개발 일지
[docs/개발일지.md](docs/개발일지.md)

## 참고
빌드 결과물은 OneDrive 동기화 충돌을 피하기 위해 `C:\Users\<사용자>\RoadMateBuild\`에 생성됩니다.
