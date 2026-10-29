package com.roadmate.app.car

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.control.MoveControl
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.os.Handler
import android.os.Looper

/**
 * 3·4단계에서 차 실내 환경을 바꾼다 (Vehicle SDK 제어 API).
 *
 * - 각성 모드: 졸음쉼터로 가는 동안 시원한 바람 + 통풍시트로 1차로 잠을 깨운다.
 * - 낮잠 모드: 조명을 어둡게, 잠자기 좋은 온도로, 시트 열선을 약하게, 등받이를 뒤로.
 * - 기상 모드: 시원한 바람, 통풍시트, 조명 밝게, 창문 살짝 열기, 등받이 원위치.
 *
 * 차종마다 제어할 수 있는 영역(운전석, 앞좌석 전체 등)과 값의 범위가 다르기 때문에,
 * prepare()에서 Capability API로 '이 차가 지원하는 영역·범위'를 먼저 확인하고 그 값으로 제어한다.
 */
class CabinController(
    private val vehicle: Vehicle,
    private val log: (String) -> Unit
) {
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val main = Handler(Looper.getMainLooper())

    // Capability 확인 결과 (확인 전에는 운전석 기본값)
    private var powerArea: SeatArea = driverSeat
    private var acArea: SeatArea = driverSeat
    private var fanArea: SeatArea = driverSeat
    private var fanRange = 0..8
    private var tempArea: SeatArea = driverSeat
    private var tempRange = 16f..30f
    private var ambientArea: SeatArea = driverSeat
    private var ambientRange = 0..100
    private var backrestArea: SeatArea = driverSeat
    private var windowArea = WindowArea(WindowValue.ROW_1_LEFT)
    private var windowRange = 0..100

    /** 차량이 지원하는 제어 영역·범위 확인. 차량 서비스 연결 후 한 번 호출한다. */
    fun prepare() {
        val hvac = vehicle.getHVAC()
        call("공조 전원 확인") {
            hvac.checkHvacPowerCapabilities(
                onComplete = { _, areas -> areas.firstOrNull()?.let { powerArea = it } },
                onFailed = fail("공조 전원 확인")
            )
        }
        call("에어컨 확인") {
            hvac.checkHvacAcCapabilities(
                onComplete = { _, areas -> areas.firstOrNull()?.let { acArea = it } },
                onFailed = fail("에어컨 확인")
            )
        }
        call("풍량 확인") {
            hvac.checkHvacFanSpeedCapabilities(
                onComplete = { _, map -> map.entries.firstOrNull()?.let { fanArea = it.key; fanRange = it.value.minValue..it.value.maxValue } },
                onFailed = fail("풍량 확인")
            )
        }
        call("온도 확인") {
            hvac.checkHvacTemperatureCapabilities(
                onComplete = { _, map -> map.entries.firstOrNull()?.let { tempArea = it.key; tempRange = it.value.minValue..it.value.maxValue } },
                onFailed = fail("온도 확인")
            )
        }
        call("앰비언트 조명 확인") {
            vehicle.getLight().checkAmbientLightBrightnessCapabilities(
                onComplete = { _, map -> map.entries.firstOrNull()?.let { ambientArea = it.key; ambientRange = it.value.minValue..it.value.maxValue } },
                onFailed = fail("앰비언트 조명 확인")
            )
        }
        call("등받이 확인") {
            vehicle.getSeat().checkMoveSeatBackrestAngleCapabilities(
                onComplete = { _, areas -> areas.firstOrNull()?.let { backrestArea = it } },
                onFailed = fail("등받이 확인")
            )
        }
        call("창문 확인") {
            vehicle.getWindow().checkWindowPositionCapabilities(
                onComplete = { _, map -> map.entries.firstOrNull()?.let { windowArea = it.key; windowRange = it.value.minValue..it.value.maxValue } },
                onFailed = fail("창문 확인")
            )
        }
    }

    /** 졸음쉼터로 가는 동안: 시원한 바람 + 통풍시트 */
    fun refreshMode() {
        log("❄️ 각성 모드: 시원한 바람, 통풍시트")
        climate(temperature = 20f, fanRatio = 0.6f, ac = true)
        call("통풍시트") { vehicle.getSeat().setHvacSeatVentilation(driverSeat, 2, onFailed = fail("통풍시트")) }
    }

    /** 정차 후 낮잠 모드 */
    fun napMode() {
        log("🌙 낮잠 모드: 조명 어둡게, 24℃, 등받이 뒤로")
        climate(temperature = 24f, fanRatio = 0.25f, ac = false)
        call("시트 열선") { vehicle.getSeat().setHvacSeatTemperature(driverSeat, 1, onFailed = fail("시트 열선")) }
        call("실내등") { vehicle.getLight().setCabinLightSwitch(LightSwitch.OFF, onFailed = fail("실내등")) }
        ambient(ratio = 0.1f)
        moveBackrest(MoveControl.BACKWARD)
    }

    /** 낮잠이 끝나면 기상 모드 */
    fun wakeMode() {
        log("☀️ 기상 모드: 시원한 바람, 조명 밝게, 창문 살짝 열기")
        climate(temperature = 19f, fanRatio = 0.75f, ac = true)
        call("시트 열선 끄기") { vehicle.getSeat().setHvacSeatTemperature(driverSeat, 0, onFailed = fail("시트 열선")) }
        call("통풍시트") { vehicle.getSeat().setHvacSeatVentilation(driverSeat, 3, onFailed = fail("통풍시트")) }
        call("실내등") { vehicle.getLight().setCabinLightSwitch(LightSwitch.ON, onFailed = fail("실내등")) }
        ambient(ratio = 1f)
        val open = windowRange.first + ((windowRange.last - windowRange.first) * 0.2f).toInt()
        call("창문") { vehicle.getWindow().setWindowPosition(windowArea, open, onFailed = fail("창문")) }
        moveBackrest(MoveControl.FORWARD)
    }

    private fun climate(temperature: Float, fanRatio: Float, ac: Boolean) {
        val hvac = vehicle.getHVAC()
        val temp = temperature.coerceIn(tempRange.start, tempRange.endInclusive)
        val fan = fanRange.first + ((fanRange.last - fanRange.first) * fanRatio).toInt()
        call("공조 전원") { hvac.setHvacPower(powerArea, true, onFailed = fail("공조 전원")) }
        call("온도") { hvac.setHvacTemperature(tempArea, temp, TemperatureUnit.CELSIUS, onFailed = fail("온도")) }
        call("풍량") { hvac.setHvacFanSpeed(fanArea, fan, onFailed = fail("풍량")) }
        call("에어컨") { hvac.setHvacAc(acArea, ac, onFailed = fail("에어컨")) }
    }

    private fun ambient(ratio: Float) = call("앰비언트 조명") {
        val value = ambientRange.first + ((ambientRange.last - ambientRange.first) * ratio).toInt()
        vehicle.getLight().setAmbientLightBrightness(
            ambientArea, value,
            onComplete = { _, _ -> },
            onFailed = fail("앰비언트 조명")
        )
    }

    /** 등받이는 '움직이기 시작 → 3초 뒤 멈춤' 방식으로 제어한다. */
    private fun moveBackrest(direction: MoveControl) {
        call("등받이") { vehicle.getSeat().moveSeatBackrestAngle(backrestArea, direction, onFailed = fail("등받이")) }
        main.postDelayed({
            call("등받이 정지") { vehicle.getSeat().moveSeatBackrestAngle(backrestArea, MoveControl.STOP, onFailed = fail("등받이")) }
        }, 3_000L)
    }

    private fun fail(name: String): (Exception) -> Unit = { e -> log("  · $name 실패: ${e.message}") }

    private fun call(name: String, block: () -> Unit) {
        try { block() } catch (e: Exception) { log("  · $name 사용 불가: ${e.message}") }
    }
}
