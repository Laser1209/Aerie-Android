package top.etta.aerie.data.chat

import kotlin.math.roundToLong
import kotlin.random.Random

class SseReconnectBackoff(
    private val random: () -> Double = { Random.nextDouble() },
) {
    fun delayMillis(attempt: Int): Long {
        val baseMillis = BASE_DELAYS_MILLIS[attempt.coerceIn(0, BASE_DELAYS_MILLIS.lastIndex)]
        val normalized = random().coerceIn(0.0, 1.0) * 2.0 - 1.0
        val jitter = (baseMillis * JITTER_RATIO * normalized).roundToLong()
        return (baseMillis + jitter).coerceAtLeast(MINIMUM_DELAY_MILLIS)
    }

    private companion object {
        val BASE_DELAYS_MILLIS = longArrayOf(1_000, 2_000, 4_000, 8_000, 30_000)
        const val JITTER_RATIO = 0.20
        const val MINIMUM_DELAY_MILLIS = 250L
    }
}
