package com.roadmate.app.core

/**
 * 로드메이트가 하는 말(대사) 모음.
 *
 * 1차 제출 단계에서는 미리 준비한 문장을 돌아가며 사용하고,
 * 다음 단계에서 이 부분을 AI(LLM) 대화 생성으로 바꾼다. (지나가는 지역 이야기 등)
 */
class DialogueScript {

    private val openers = listOf(
        "잠깐 저랑 이야기 좀 할까요? 요즘 제일 맛있게 드신 음식이 뭐예요?",
        "창밖 풍경 어때요? 오늘 가시는 곳에서 제일 기대되는 게 뭔지 궁금해요.",
        "휴게소 하면 떠오르는 간식 있으세요? 저는 호두과자가 궁금해요.",
        "노래 한 곡 떠올려 볼까요? 요즘 자주 듣는 노래 제목이 뭐예요?"
    )
    private var openerIndex = 0

    private val replies = listOf(
        "오, 좋네요! 계속 이렇게 이야기 나눠요.",
        "그렇군요. 말씀해 주시니 저도 즐거워요.",
        "좋아요! 졸리면 언제든 '쉬고 싶어'라고 말씀하세요."
    )
    private var replyIndex = 0

    /** 먼저 말 걸기. 상황(운전 시간, 순정 졸음 경고)에 맞는 첫마디를 붙인다. */
    fun opener(continuousMinutes: Int, dawWarningOn: Boolean, inHotspot: Boolean = false): String {
        val intro = when {
            dawWarningOn -> "차가 졸음 경고를 보냈어요."
            inHotspot -> "이 구간은 다른 운전자분들도 자주 졸았던 곳이에요."
            continuousMinutes >= 60 -> "쉬지 않고 ${formatMinutes(continuousMinutes)} 운전하셨어요."
            else -> "운전하시느라 고생 많으세요."
        }
        val line = openers[openerIndex % openers.size]
        openerIndex++
        return "$intro $line"
    }

    fun replyTo(@Suppress("UNUSED_PARAMETER") answer: String): String {
        val line = replies[replyIndex % replies.size]
        replyIndex++
        return line
    }

    /** 운전자가 쉬고 싶다는 뜻을 말했는지 */
    fun wantsRest(answer: String): Boolean =
        listOf("졸려", "졸리", "피곤", "쉬고", "쉴래", "쉬자", "휴게소", "쉼터").any { answer.contains(it) }

    fun restGuide(): String =
        "많이 피곤하신 것 같아요. 가까운 졸음쉼터로 안내할게요. 도착해서 주차하시면 편하게 쉬실 수 있게 준비해 둘게요."

    fun napStart(napMs: Long): String =
        "도착했어요. ${formatMinutes((napMs / 60_000L).toInt().coerceAtLeast(1))} 뒤에 깨워 드릴게요. 시트를 뒤로 눕히고 편하게 쉬세요."

    fun wakeUp(): String =
        "잘 쉬셨어요? 시원한 바람 틀어 드렸어요. 기지개 한 번 켜고 천천히 출발해요."

    private fun formatMinutes(m: Int): String {
        val h = m / 60
        val min = m % 60
        return when {
            h > 0 && min > 0 -> "${h}시간 ${min}분"
            h > 0 -> "${h}시간"
            else -> "${min}분"
        }
    }
}
