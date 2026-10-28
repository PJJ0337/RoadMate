package com.roadmate.app

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.roadmate.app.core.DrowsinessRiskEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1단계 개발용 테스트 화면.
 * 목표: (1) 차량 SDK 연결 확인 (2) 음성(TTS) 출력 확인 (3) 졸음 위험 계산 확인
 * 세 버튼이 모두 성공하면 본격적인 기능 개발을 시작한다.
 */
class MainActivity : Activity() {

    private lateinit var vehicle: Vehicle
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private val riskEngine = DrowsinessRiskEngine()
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())

        // 차량 서비스 연결
        vehicle = Vehicle(applicationContext)
        vehicle.initialize()
        log("차량 SDK 초기화 요청")

        // 음성 합성: 앱 등록이 필요 없는 ON_DEVICE 모드로 시작
        tts = TextToSpeech(applicationContext, Mode.ON_DEVICE)
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                ttsReady = true
                log("TTS 준비 완료")
            }

            override fun onFailure(errorType: ErrorType) {
                log("TTS 초기화 실패: $errorType")
            }
        })
    }

    /** 테스트 1: 운전석 문 상태 읽기 (공식 문서의 첫 예제) */
    private fun testVehicle() {
        log("운전석 문 상태 조회 중...")
        vehicle.getDoor().getCurrentDoorPosition(
            area = DoorArea(DoorValue.ROW_1_LEFT),
            onComplete = { pos: Int? -> log("✅ 차량 연결 성공! 운전석 문 위치 값 = $pos") },
            onFailed = { e: Exception -> log("❌ 차량 조회 실패: ${e.message}") }
        )
    }

    /** 테스트 2: 로드메이트가 먼저 말 걸기 */
    private fun testSpeak() {
        if (!ttsReady) {
            log("TTS가 아직 준비되지 않았어요. 잠시 후 다시 눌러주세요.")
            return
        }
        val line = "운전하신 지 두 시간이 넘었어요. 잠깐 저랑 이야기 좀 할까요?"
        tts.speak(line)
        log("🔊 로드메이트: $line")
    }

    /** 테스트 3: 졸음 위험 점수 계산 (새벽 3시, 2시간 10분 연속 운전, 크루즈 30분) */
    private fun testRisk() {
        val result = riskEngine.evaluate(
            DrowsinessRiskEngine.Input(
                continuousDrivingMinutes = 130,
                hourOfDay = 3,
                cruiseActiveMinutes = 30,
                minutesSinceDriverInput = 5,
                dawWarningOn = false,
                missedResponses = 0
            )
        )
        log("📊 위험 점수 ${result.score}점 → ${result.level} (${result.reasons.joinToString(", ")})")
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#101418"))
        }
        root.addView(TextView(this).apply {
            text = "로드메이트 — 개발 테스트"
            textSize = 44f
            setTextColor(Color.WHITE)
        })
        // 차량 화면은 터치 영역이 커야 한다 (Pleos UI 가이드)
        fun bigButton(label: String, color: String, onClick: () -> Unit) = Button(this).apply {
            text = label
            textSize = 36f
            minHeight = 200
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(color))
            setOnClickListener { onClick() }
            // 버튼 사이에 간격을 두어 구분이 잘 되게 한다
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 32, 0, 0) }
        }
        root.addView(bigButton("1. 차량 연결 테스트", "#2E6BE6") { testVehicle() })
        root.addView(bigButton("2. 말 걸기 테스트 (TTS)", "#1E9E6A") { testSpeak() })
        root.addView(bigButton("3. 졸음 위험 계산 테스트", "#D9822B") { testRisk() })

        logView = TextView(this).apply {
            textSize = 28f
            setTextColor(Color.parseColor("#B8C4CC"))
            setPadding(0, 32, 0, 0)
        }
        root.addView(logView)
        return ScrollView(this).apply { addView(root) }
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)

    /** SDK 콜백은 다른 스레드에서 올 수 있어 화면 갱신은 UI 스레드에서 한다. */
    private fun log(message: String) {
        runOnUiThread {
            logView.text = "[${timeFormat.format(Date())}] $message\n" + logView.text
        }
    }

    override fun onDestroy() {
        tts.release()
        vehicle.release()
        super.onDestroy()
    }
}
