# RoadMate 0.2.1 검증 기록

2026-11-15에 0.2.0을 별도 보존한 뒤 공식 Android Automotive UX 정책 연동을 추가했습니다. 플랫폼 제한이나 허용목록을 수정하지 않았습니다.

## 최종 APK와 단위 테스트

- 패키지 `com.roadmate.app`, versionName `0.2.1`, versionCode `3`
- 크기: 27,035,786 bytes
- SHA-256: `ef7a12b6076aa781eb2c1536df1b7c3f142c5f4764706343f13afb1ed49781fb`
- `:app:testDebugUnitTest :app:assembleRelease` 성공, 43개 테스트 / 실패 0 / 오류 0
- apksigner v2 서명 검증 통과. 공모전용 debug 인증서 서명이며 배포용 서명 관리 완료를 뜻하지 않습니다.
- 빌드 출력 `app/build/outputs/apk/release/app-release.apk`, 제출 소스 ZIP의 APK `apk/로드메이트_로드메이트_release.apk`
- JDK 21.0.13, SDK 35, Build Tools 35.0.0, Gradle 8.10.2. 한글 경로 테스트 worker 문제를 피하기 위해 ASCII junction 경로를 사용했습니다.

## 0.2.1 변경 범위

`Car.createCar(..., CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`로 비동기 연결하고 `CarUxRestrictionsManager`의 현재 정책을 읽은 뒤 변경 리스너를 등록합니다. `NO_SETUP` 또는 정책 미확인 상태에서 시연 패널·예시 지도·AI 설정·로그를 숨깁니다. 열린 AI 동의창도 닫고, 설정 허용 상태 및 실제 P가 확인되면 다시 표시합니다. 종료 시 리스너와 연결을 해제합니다.

차량 모듈이 boot classpath로 제공되는 Pleos 환경에 맞게 `android.car` 공유 라이브러리는 optional로 선언했습니다. 최초 required 선언은 설치 단계에서 거절되었으며, 최종 APK는 정상 설치됐습니다.

내비게이션과 TaskView 전환 순서에 따라 확인창 표시가 늦는 경우를 위해 기존 Activity에 전달되는 intent에서도 대기 상태를 확인하고, `휴식 시작 확인` 버튼을 추가했습니다. 이 버튼은 확인창만 열며 실제 P와 확인 대기 상태를 재검사합니다. 사용자의 `휴식 시작` 승인 없이 낮잠 제어를 시작하지 않습니다.

## 에뮬레이터 확인

Pleos Connect v2.1.0 / API 34 / 2560×1440 / 160 dpi, 동일 task-local AVD에서 확인했습니다. 모든 '실제 P/D'는 앱 내부 모의 입력과 구분한 에뮬레이터 Gear SDK 신호입니다.

| 검증 | 결과 |
|---|---|
| P에서 설치·실행 | 최종 APK 설치 성공, 기어 P·설정 사용 가능 표시 |
| P에서 AI 설정 | 외부 전송 설명·동의/기본 안내 선택지 정상 표시 |
| D 전환 시 NO_SETUP | UXR 0x50, 리스너 callback 등록 수 9→10 확인. 동의창 닫힘, 시연·예시 지도·AI 설정·로그 숨김, 잠금 문구 표시 |
| P 복귀 | 설정 잠금 해제 및 대기 중인 AI 동의창 재표시 확인 |
| 최종 휴식 회귀 | 최종 APK에서 모의 '쉬고 싶어' 입력→휴식 확인창 자동 표시→명시 승인→파워냅→중단→주차 화면 복귀 확인 |
| 확인창 예외 대응 | 확인 대기 화면에 명시적인 `휴식 시작 확인` 버튼 추가. 실제 P 조건을 매번 재검사 |

[D 잠금](screenshots/v0.2.1/D-locked.png), [P 복귀 동의창](screenshots/v0.2.1/P-recovered.png), [P 설정 복귀](screenshots/v0.2.1/P-settings.png), [최종 휴식 확인창](screenshots/v0.2.1/confirm.png), [휴식 중](screenshots/v0.2.1/nap.png), [중단 후](screenshots/v0.2.1/cancel.png).

D/P 정책 잠금 검증은 NO_SETUP 연동 빌드에서 확인했고, 마지막 확인창 복귀·버튼 보강 후에는 최종 APK의 P 휴식 흐름을 재검증했습니다. 단위 테스트는 마지막 빌드에서도 43개 통과했습니다.

## 검증 범위의 한계

- 이번 0.2.1 정책 전환 화면 검증은 D·0 km/h에서 수행했습니다. 0.2.0의 D·5 km/h(MOVING) 및 실제 D+모의 P 차단 관찰, 공조·조명·창문·등받이 Console 값 변화는 [이전 기록](VALIDATION-0.2.0.md)에 따로 보존합니다. 해당 제어 로직은 0.2.1에서 변경하지 않았습니다.
- NO_SETUP 설정 UI 제한을 구현한 것이며 모든 지역·차종의 UX 플래그나 전체 마켓 심사 준수 검증 완료를 뜻하지 않습니다. 주행 중 음성 연속 동작은 추가 검증이 필요합니다.
- 현재 화면 녹화와 캡처는 호스트 오디오를 끈 상태의 모의 대답 기반 자료입니다. 실제 STT 인식 정확도·TTS 재생음을 검증하지 않았습니다.
- 네트워크 지연·서버 실패 시 AI fallback 통합 시험은 미수행입니다. 응답 취소 gate는 단위 테스트, AI 거부 후 휴식 흐름은 화면으로 확인했습니다.
- Odometer 트립 시간은 이 AVD에서 지원되지 않았습니다. 시연 시간 가산·졸음 지도는 모의 데이터입니다.
- 등받이 원래 각도 복원, 실제 차량 물리 동작, 졸음 예측 정확도, 사고 감소 효과, 백그라운드 서비스·시동 자동 실행은 구현·검증 완료 항목이 아닙니다.

공식 참고: [Pleos 주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [Android Car API](https://developer.android.com/reference/android/car/Car), [CarUxRestrictionsManager](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager).
