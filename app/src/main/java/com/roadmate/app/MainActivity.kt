package com.roadmate.app

import ai.pleos.playground.vehicle.Vehicle
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.roadmate.app.car.CabinController
import com.roadmate.app.car.RestNavigator
import com.roadmate.app.car.VehicleMonitor
import com.roadmate.app.car.VoiceIO
import com.roadmate.app.core.DrowsinessRiskEngine
import com.roadmate.app.core.RoadMateBrain
import com.roadmate.app.core.RoadMateBrain.Action
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 로드메이트 메인 화면.
 *
 * 차량 신호(VehicleMonitor) → 두뇌(RoadMateBrain)가 1초마다 판단 → 행동(Action) 실행
 *   - Speak: 음성으로 말하고 대답 듣기 (VoiceIO)
 *   - NavigateToRest: 졸음쉼터 길 안내 (RestNavigator) + 각성 모드 (CabinController)
 *   - StartNap / StartWake: 낮잠·기상 모드 (CabinController)
 *
 * 아래쪽 '시연 패널'은 에뮬레이터에서 바로 바꾸기 어려운 상황(장시간 운전, 졸음 경고 등)을
 * 버튼으로 재현하기 위한 것이다. 같은 상황은 Pleos Virtual Console로도 만들 수 있다.
 */
class MainActivity : Activity() {

    private lateinit var vehicle: Vehicle
    private lateinit var monitor: VehicleMonitor
    private lateinit var voice: VoiceIO
    private lateinit var cabin: CabinController
    private lateinit var navigator: RestNavigator

    private val brain = RoadMateBrain(RoadMateBrain.Config.DEMO)
    private val main = Handler(Looper.getMainLooper())

    // 상태 (화면 표시용)
    private var sdkTripMinutes = 0
    private var demoExtraMinutes = 0
    private var cruiseOn = false
    private var gearName = "-"
    private var isPark = true
    private var dawOn = false

    private lateinit var phaseView: TextView
    private lateinit var scoreView: TextView
    private lateinit var detailView: TextView
    private lateinit var logView: TextView
    private lateinit var gearButton: Button
    private lateinit var dawButton: Button

    private val loop = object : Runnable {
        override fun run() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            brain.tick(hour).forEach(::perform)
            render()
            main.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestRuntimePermissions()

        vehicle = Vehicle(applicationContext)
        vehicle.initialize()
        monitor = VehicleMonitor(vehicle, ::log)
        cabin = CabinController(vehicle, ::log)
        navigator = RestNavigator(this, ::log)
        voice = VoiceIO(this, ::log)
        voice.listener = object : VoiceIO.Listener {
            override fun onAnswer(text: String) { brain.onAnswer(text).forEach(::perform) }
            override fun onNoAnswer() { brain.onNoAnswer() }
        }

        // 차량 서비스 연결이 끝날 시간을 조금 준 뒤 구독 시작
        main.postDelayed({
            monitor.start(vehicleCallback)
            cabin.prepare() // 이 차가 지원하는 공조·조명·창문 영역과 범위 확인
            voice.boostVolume() // 시연 모드: 안내 음성이 잘 들리도록 볼륨 올리기
        }, 1_000L)
        main.postDelayed(loop, 1_500L)
        log("로드메이트 시작 — 시연 모드(말 걸기 간격 15초, 낮잠 30초)")
    }

    private val vehicleCallback = object : VehicleMonitor.Callback {
        override fun onTripMinutes(minutes: Int) = runOnUiThread {
            sdkTripMinutes = minutes
            brain.onTripMinutes(sdkTripMinutes + demoExtraMinutes)
        }
        override fun onCruise(active: Boolean) = runOnUiThread {
            if (active != cruiseOn) log(if (active) "크루즈 컨트롤 켜짐" else "크루즈 컨트롤 꺼짐")
            cruiseOn = active
            brain.onCruise(active)
        }
        override fun onGear(isPark: Boolean, gearName: String) = runOnUiThread { applyGear(isPark, gearName) }
        override fun onDriverInput(source: String) = runOnUiThread { brain.onDriverInput() }
        override fun onDawWarning(on: Boolean) = runOnUiThread { applyDaw(on) }
    }

    /** 두뇌가 내린 행동을 실제로 실행 */
    private fun perform(action: Action) {
        when (action) {
            is Action.Speak -> voice.speak(action.text, action.listenAfter)
            Action.NavigateToRest -> {
                cabin.refreshMode()
                navigator.guideToNearest()
            }
            Action.StartNap -> cabin.napMode()
            Action.StartWake -> cabin.wakeMode()
        }
    }

    private fun applyGear(park: Boolean, name: String) {
        if (park != isPark) log(if (park) "기어 P (주차)" else "기어 $name (주행)")
        isPark = park
        gearName = name
        brain.onGear(park)
        if (::gearButton.isInitialized) gearButton.text = if (park) "기어: P → D로 출발" else "기어: D → P로 주차"
    }

    private fun applyDaw(on: Boolean) {
        if (on != dawOn) log(if (on) "⚠️ 순정 졸음 경고 켜짐" else "순정 졸음 경고 꺼짐")
        dawOn = on
        brain.onDawWarning(on)
        if (::dawButton.isInitialized) dawButton.text = if (on) "졸음 경고 끄기" else "졸음 경고 켜기"
    }

    // ---------------- 화면 ----------------

    private fun render() {
        val phaseText = when (brain.phase) {
            RoadMateBrain.Phase.DRIVING -> "🚗 주행 중 — 지켜보는 중"
            RoadMateBrain.Phase.TALKING -> "💬 대화 중 — 먼저 말을 걸었어요"
            RoadMateBrain.Phase.GUIDING_TO_REST -> "🧭 졸음쉼터로 안내 중"
            RoadMateBrain.Phase.NAPPING -> "🌙 파워냅 중"
        }
        phaseView.text = phaseText
        val r = brain.lastResult
        if (r != null) {
            scoreView.text = "졸음 위험 ${r.score}점 · ${levelText(r.level)}"
            scoreView.setTextColor(levelColor(r.level))
        } else {
            scoreView.text = if (isPark) "주차 중" else "졸음 위험 계산 중"
            scoreView.setTextColor(Color.parseColor("#B8C4CC"))
        }
        detailView.text = "연속 운전 ${brain.continuousMinutes}분 · 크루즈 ${if (cruiseOn) "ON" else "OFF"} · 기어 ${if (isPark) "P" else gearName} · 졸음 경고 ${if (dawOn) "ON" else "OFF"}" +
            (r?.reasons?.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "\n근거: ") ?: "")
    }

    private fun levelText(l: DrowsinessRiskEngine.Level) = when (l) {
        DrowsinessRiskEngine.Level.NORMAL -> "평상시"
        DrowsinessRiskEngine.Level.CAUTION -> "주의"
        DrowsinessRiskEngine.Level.DANGER -> "위험"
    }

    private fun levelColor(l: DrowsinessRiskEngine.Level) = Color.parseColor(
        when (l) {
            DrowsinessRiskEngine.Level.NORMAL -> "#4CC38A"
            DrowsinessRiskEngine.Level.CAUTION -> "#F5A623"
            DrowsinessRiskEngine.Level.DANGER -> "#FF5A5F"
        }
    )

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(Color.parseColor("#101418"))
        }
        root.addView(text("로드메이트", 44f, "#FFFFFF", bold = true))
        phaseView = text("", 36f, "#FFFFFF", bold = true).also { root.addView(it) }
        scoreView = text("", 40f, "#B8C4CC", bold = true).also { root.addView(it) }
        detailView = text("", 24f, "#B8C4CC").also { root.addView(it) }

        root.addView(text("시연 패널", 26f, "#7A8A99", bold = true).apply { setPadding(0, 32, 0, 8) })
        root.addView(row(
            button("운전 +30분", "#2E6BE6") {
                demoExtraMinutes += 30
                brain.onTripMinutes(sdkTripMinutes + demoExtraMinutes)
                log("시연: 운전 시간 +30분 (연속 ${brain.continuousMinutes}분)")
            },
            button("졸음 경고 켜기", "#D9822B") { applyDaw(!dawOn) }.also { dawButton = it },
            button("기어: P → D로 출발", "#5B6B7A") { applyGear(!isPark, if (isPark) "GEAR_DRIVE" else "GEAR_PARK") }.also { gearButton = it }
        ))
        root.addView(row(
            button("대답: \"괜찮아\"", "#1E9E6A") { voice.simulateAnswer("응 괜찮아") },
            button("대답: \"쉬고 싶어\"", "#1E9E6A") { voice.simulateAnswer("좀 졸려 쉬고 싶어") },
            button("대답 안 함 (기다리기)", "#3A4552") { log("시연: 대답하지 않고 기다립니다 (8초)") }
        ))

        logView = text("", 20f, "#8FA0AE").apply { setPadding(0, 24, 0, 0) }
        root.addView(logView)
        render()
        return ScrollView(this).apply { addView(root) }
    }

    private fun text(value: String, size: Float, color: String, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 6, 0, 6)
    }

    private fun row(vararg buttons: Button) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        buttons.forEach { addView(it) }
    }

    // 차량 화면은 터치 영역이 커야 한다 (Pleos UI 가이드)
    private fun button(label: String, color: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 24f
        isAllCaps = false
        minHeight = 160
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(8, 16, 8, 0)
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)

    /** SDK 콜백은 다른 스레드에서 올 수 있어 화면 갱신은 UI 스레드에서 한다. 최근 40줄만 유지. */
    private fun log(message: String) {
        runOnUiThread {
            if (!::logView.isInitialized) return@runOnUiThread
            val lines = ("[${timeFormat.format(Date())}] $message\n" + logView.text).lines()
            logView.text = lines.take(40).joinToString("\n")
        }
    }

    private fun requestRuntimePermissions() {
        val needed = listOf("android.permission.RECORD_AUDIO", "pleos.car.permission.FUSED_LOCATION")
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        monitor.stop()
        voice.release()
        navigator.release()
        vehicle.release()
        super.onDestroy()
    }
}
