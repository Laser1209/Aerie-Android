package top.etta.aerie.data.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class SseReconnectBackoffTest {
    @Test
    fun `zero jitter follows the required capped schedule`() {
        val backoff = SseReconnectBackoff(random = { 0.5 })

        assertEquals(1_000L, backoff.delayMillis(0))
        assertEquals(2_000L, backoff.delayMillis(1))
        assertEquals(4_000L, backoff.delayMillis(2))
        assertEquals(8_000L, backoff.delayMillis(3))
        assertEquals(30_000L, backoff.delayMillis(4))
        assertEquals(30_000L, backoff.delayMillis(99))
    }

    @Test
    fun `jitter stays within twenty percent of the base delay`() {
        val low = SseReconnectBackoff(random = { 0.0 })
        val high = SseReconnectBackoff(random = { 1.0 })

        assertEquals(800L, low.delayMillis(0))
        assertEquals(1_200L, high.delayMillis(0))
        assertEquals(24_000L, low.delayMillis(4))
        assertEquals(36_000L, high.delayMillis(4))
    }
}
