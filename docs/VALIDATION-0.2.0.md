# RoadMate 0.2.0 검증 기록

검증일: 2026-11-15. 원본 커밋 `305a29a2242ffd7e7b2bfeea1bc7ceeb09fc3740`에서 수정한 로컬 제출 검토본입니다. 공개 저장소에 업로드했다는 의미는 아닙니다.

## 빌드 산출물

- 애플리케이션: `com.roadmate.app`
- 버전: `0.2.0` / versionCode `2`
- 빌드 명령: `gradlew.bat :app:testDebugUnitTest :app:assembleRelease`
- 최종 결과: Gradle exit 0, release APK 생성. Android apksigner 서명 검증 통과.
- APK 크기: 27,035,750 bytes
- SHA-256: `dfa4210492c5d9a786ba2a189d3c63dc76b1231bbd65662043120e18fb88ac77`
- 제출 소스 ZIP 경로: `apk/로드메이트_로드메이트_release.apk`
- 빌드 출력 경로: `app/build/outputs/apk/release/app-release.apk`
- release는 공모전용 debug 인증서 서명입니다. 앱 배포용 키 관리가 완료된 상태는 아닙니다.

Windows의 한글 경로에서 첫 테스트 worker 기동이 실패해 ASCII junction 경로로 다시 실행했습니다. 이는 단위 테스트 assertion 실패가 아니었으며, 아래 결과는 재실행이 성공한 결과입니다. JDK 21.0.13, SDK Platform 35, Build Tools 35.0.0, Gradle 8.10.2를 사용했습니다.

## Kotlin 단위 테스트

6개 suite, **43개 통과 / 실패 0 / 오류 0 / 건너뜀 0**.

| Suite | 테스트 수 |
|---|---:|
| DrowsinessRiskEngineTest | 5 |
| DrowsyMapTest | 4 |
| ParkingGateTest | 5 |
| PrivacyFilterTest | 5 |
| ResponseGateTest | 3 |
| RoadMateBrainTest | 21 |

실제/모의 기어 분리, 미확인·D 상태에서 승인 거절, 명시적 승인, 휴식 취소와 시간 보존, 기어 이탈 시 취소, 취소된 AI 응답 무시 등을 순수 Kotlin 수준에서 검증했습니다. SDK 콜백·Android 생명주기·실차 안전성 전체를 검사한 계측 테스트는 아닙니다.

## Pleos 에뮬레이터 확인

공식 Pleos Connect v2.1.0 x86_64 이미지, 신규 task-local AVD, 2560×1440 / 160 dpi / RAM 4 GB에서 위 해시 APK를 설치했습니다. 공식 이미지 SHA-1은 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa`와 일치했습니다. Virtual Console을 연결해 실제 Gear SDK가 P를 받는 상태에서 화면 흐름을 확인했습니다. 여기서 '실제 기어'는 앱의 모의 버튼과 구분한 에뮬레이터 SDK 신호이며, 실제 자동차 검증이라는 의미가 아닙니다.

| 항목 | 관찰 결과 |
|---|---|
| 설치·실행 | version 0.2.0 APK 설치 성공, 기본 화면 정상 표시 |
| Gear SDK P | 상단 `실제 기어: GEAR_PARK`, `화면 입력: 차량 신호` 표시 |
| 실제 D/MOVING 화면 | AVD 재시작 후 Console 실제 D·5 km/h, CarDrivingState=2, UXR=0x50 상태에서 앱 표시 확인 |
| 실제 D+모의 P 차단 | 모의 P로 변경하고 쉬고 싶어 입력 후 안내 상태 유지, 낮잠 확인창 없음, 상단 실내 휴식 제어 차단 표시 |
| D 상태 규칙 계산 | 실제 D로 표시 복귀 후 모의 시간 90분 가산, 점수 15→35 갱신. 주행 시간 실측이 아님 |
| AI 동의 화면 | 전송 대상·텍스트·거부 후 사용 가능 안내와 두 선택지 표시, 글자 잘림 없음 |
| 기본 안내 | AI 동의를 거부한 상태에서 휴식 흐름 동작 |
| 휴게소 요청 | 시연 버튼의 모의 `쉬고 싶어` 입력으로 NaviHelper에 예시 휴게소 안내 요청 |
| 사용자 확인 | 실제 P에서 확인창 표시, 내비게이션 요청 후 기존 Activity의 확인 상태 유지 |
| 휴식 시작 | 확인창 `휴식 시작` 후 파워냅 상태와 30초 안내·중단 버튼 표시 |
| 휴식 중단 | 낮잠 중 `휴식 중단` 후 주차 상태 및 시트·창문 직접 확인 문구로 복귀 |
| 중단 후 | 타이머가 지날 때까지 자동 기상 전환이 발생하지 않음, 공조 24℃ 유지 |
| 공조·조명 | Console에서 낮잠 설정 24℃·팬 2·열선 1·앰비언트 10% 반영 확인 |
| 30초 정상 완료 | 실제 P 유지 후 주차 상태로 복귀, 추가 휴식·주변 확인 안내 표시 |
| 기상 제어 | Console에서 19℃·팬 6·통풍 -3·앰비언트 100%·운전석 창문 20%·등받이 위치 0→100 및 이동 STOP 확인 |

현재 버전 캡처는 [확인창](screenshots/v0.2.0/confirm.png), [휴식 중](screenshots/v0.2.0/nap.png), [중단 후](screenshots/v0.2.0/cancel.png), [AI 동의](screenshots/v0.2.0/consent.png), [기본 화면](screenshots/v0.2.0/home.png), [30초 완료](screenshots/v0.2.0/wake.png)입니다. 별도 20초 화면 녹화는 파워냅 상태에서 휴식 중단·주차 화면 복귀를 기록했으며 무음입니다.

## 확인된 제약과 남은 검증

- 초기 설치 세션에서 실제 D 전환 시 '주행 중에는 사용할 수 없습니다' 화면을 관찰했습니다. 최종 APK 설치와 AVD 재시작 후에는 실제 D·5 km/h(MOVING)에서도 정상 화면을 확인했습니다. 당시 `dumpsys car_service`의 UXR는 0x50(NO_VIDEO·NO_SETUP), DO 요구=true였고 차단 기록은 비어 있었습니다. APK의 Activity DO 메타데이터도 정상 포함되어 있었습니다. 초기 현상의 원인은 확정하지 못했으며, 주행 제한·허용목록 변경이나 우회는 하지 않았습니다.
- 실제 D+모의 P+휴식 요청에서 낮잠 진입 차단을 확인했습니다. 관련 캡처는 [실제 D와 모의 P](screenshots/v0.2.0/realD-mockP-guiding.png), [5 km/h 화면](screenshots/v0.2.0/movingD.png), [모의 시간 가산](screenshots/v0.2.0/D-timeadded.png)입니다. 공도·실차 검증이 아닙니다.
- `CarUxRestrictionsManager` 리스너 및 ActiveRestrictions에 따른 기능별 설정 UI 잠금은 현재 미구현입니다. 화면이 보였다는 사실은 공식 주행 UX 정책 준수 검증 완료를 뜻하지 않습니다. 주행 중 음성 연속 동작도 추가 검증해야 합니다.
- Odometer 트립 시간 조회와 구독은 이 AVD에서 `ERROR_NOT_SUPPORTED_PROPERTY`를 반환했습니다. 장시간 운전은 시연용 모의 가산으로 재현합니다.
- 등받이·창문은 Console의 위치 값 변화를 확인한 것입니다. 실제 자동차의 각도·속도·끼임 안전성을 검증한 것이 아닙니다. 등받이는 앞/뒤 3초 명령이며 이전 각도를 저장·복원하지 않습니다. 초기 위치가 하한 0이면 낮잠의 뒤 방향 명령은 값 변화가 없습니다. 기상 시에는 0→100으로 바뀌었습니다.
- STT/TTS 서비스 준비 상태는 확인했지만, 이번 화면 검증은 모의 대답을 사용했습니다. 호스트 오디오 장치 오류로 재시작 후 `-no-audio` 상태에서 화면을 기록했으므로 음성 인식 정확도나 실제 재생음을 검증한 자료가 아닙니다.
- 외부 Gemini 통신 성공·실패/4초 timeout의 통합 네트워크 실험은 수행하지 않았습니다. 늦은 응답 차단은 순수 Kotlin 테스트, AI 없이 동작하는 휴식 흐름은 화면으로 확인했습니다.
- 휴게소는 예시 3개 좌표/직선거리 기준이며, 진행 방향·도로 진입 가능성·안전한 정차 장소를 보증하지 않습니다. 졸음 지도는 예시 데이터입니다.
- 실차 졸음 예측 성능, 사고 감소 효과, 실제 차량 제어, 시동 자동 실행, 백그라운드 서비스, 프로세스 복원은 검증되지 않았습니다.

공식 참고: [주행 제한 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide), [Virtual Console 가이드](https://document.pleos.ai/en/docs/pleos-only/app-test-guide/virtual-console-guide).
