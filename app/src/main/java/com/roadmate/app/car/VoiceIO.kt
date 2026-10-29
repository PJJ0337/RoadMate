package com.roadmate.app.car

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit

/**
 * 2단계 "대화"의 입·출력: 로드메이트가 말하고(TTS), 운전자의 대답을 듣는다(STT).
 *
 * 말한 뒤 대답을 기다려야 하면(listenAfter) 말이 끝나는 순간 마이크를 켜고,
 * 정해진 시간 안에 대답이 없으면 onNoAnswer()를 알린다. → 졸음 위험 점수가 올라간다.
 * 네트워크 없이 동작하도록 차량 내장 엔진(ON_DEVICE) 모드를 쓴다.
 */
class VoiceIO(
    context: Context,
    private val log: (String) -> Unit,
    private val answerTimeoutMs: Long = 8_000L
) {
    interface Listener {
        fun onAnswer(text: String)
        fun onNoAnswer()
    }

    var listener: Listener? = null

    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, TtsMode.ON_DEVICE)
    private val stt = SpeechToText(context.applicationContext, SttMode.ON_DEVICE)

    private var ttsReady = false
    private var sttReady = false
    private var listenAfterSpeech = false
    private var listening = false
    private var answered = false

    private val timeout = Runnable {
        if (listening && !answered) {
            safely { stt.stop() }
            finishListening(noAnswer = true)
        }
    }

    init {
        // 공식 문서 순서: 객체 생성 → initialize() → 리스너 등록. 실패해도 앱이 꺼지지 않도록 감싼다.
        safely {
            tts.initialize(object : TtsInit {
                override fun onSuccess() {
                    ttsReady = true
                    safely { tts.setLocale(Locale.KOREA) }
                    log("음성 출력(TTS) 준비 완료")
                }
                override fun onFailure(errorType: TtsError) = log("TTS 초기화 실패: $errorType")
            })
        }
        safely {
            tts.addEventListener(object : EventListener {
                override fun onReady() = Unit
                override fun onStart() = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onStop() = Unit
                override fun onDone() {
                    main.post { if (listenAfterSpeech) startListening() }
                }
                override fun onError(errorType: TtsError) {
                    log("음성 출력 오류: $errorType")
                    main.post { if (listenAfterSpeech) startListening() }
                }
            })
        }
        safely {
            stt.initialize(object : SttInit {
                override fun onSuccess() {
                    sttReady = true
                    safely { stt.setLocale(Locale.KOREA) }
                    log("음성 인식(STT) 준비 완료")
                }
                override fun onFailure(errorType: SttError) = log("STT 초기화 실패: $errorType")
            })
        }
        safely {
            stt.addListener(object : ResultListener {
                override fun onReady() = Unit
                override fun onStartedRecognition() = Unit
                override fun onUpdatedEpdData(on: Long, off: Long) = Unit
                override fun onUpdatedRms(rms: Double) = Unit
                override fun onUpdated(stt: String, completed: Boolean) {
                    val text = stt
                    if (completed && text.isNotBlank()) main.post {
                        if (listening && !answered) {
                            answered = true
                            log("🎙 운전자: $text")
                            finishListening(noAnswer = false)
                            listener?.onAnswer(text)
                        }
                    }
                }
                // 인식이 끝났거나 오류가 나도 바로 '대답 없음'으로 처리하지 않고,
                // 제한 시간(answerTimeoutMs)까지 기다린다. (시연 버튼으로도 대답할 수 있게)
                override fun onEndedRecognition() = Unit
                override fun onError(errorType: SttError) = log("음성 인식 오류: $errorType")
            })
        }
    }

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** 시연용: 안내 음성 볼륨을 최대치의 ratio(0~1)만큼으로 올린다. */
    fun boostVolume(ratio: Float = 0.9f) = safely {
        val stream = AudioManager.STREAM_MUSIC
        val target = (audio.getStreamMaxVolume(stream) * ratio).toInt()
        if (audio.getStreamVolume(stream) < target) {
            audio.setStreamVolume(stream, target, 0)
            log("🔈 음성 볼륨 ${(ratio * 100).toInt()}%로 조정")
        }
    }

    /** 말하기. listenAfter=true면 말이 끝난 뒤 대답을 듣는다. */
    fun speak(text: String, listenAfter: Boolean) {
        log("🔊 로드메이트: $text")
        listenAfterSpeech = listenAfter
        if (!ttsReady) {
            log("(TTS 준비 전이라 화면에만 표시)")
            if (listenAfter) startListening()
            return
        }
        safely { tts.speak(text, true) }
    }

    /** 시연용: 실제 마이크 대신 화면 버튼으로 대답을 넣는다. */
    fun simulateAnswer(text: String) {
        if (listening) { safely { stt.stop() } }
        answered = true
        finishListening(noAnswer = false)
        log("🎙 운전자(시연): $text")
        listener?.onAnswer(text)
    }

    private fun startListening() {
        listenAfterSpeech = false
        listening = true
        answered = false
        main.removeCallbacks(timeout)
        main.postDelayed(timeout, answerTimeoutMs)
        if (sttReady) {
            log("👂 대답을 기다리는 중... (${answerTimeoutMs / 1000}초)")
            safely { stt.request() }
        } else {
            log("👂 (STT 준비 전) 화면의 대답 버튼으로 시연할 수 있어요")
        }
    }

    private fun finishListening(noAnswer: Boolean) {
        main.removeCallbacks(timeout)
        if (!listening) return
        listening = false
        if (noAnswer) {
            log("… 대답 없음")
            listener?.onNoAnswer()
        }
    }

    fun release() {
        main.removeCallbacks(timeout)
        safely { tts.release() }
        safely { stt.release() }
    }

    private fun safely(block: () -> Unit) {
        try { block() } catch (e: Exception) { log("음성 기능 오류: ${e.message}") }
    }
}
