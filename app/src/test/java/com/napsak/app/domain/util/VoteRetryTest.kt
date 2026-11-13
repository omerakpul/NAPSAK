package com.napsak.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoteRetryTest {

    @Test
    fun `network failures are retried`() {
        assertFalse(isPermanentVoteError(java.io.IOException("Unable to resolve host")))
        assertFalse(isPermanentVoteError(RuntimeException("HTTP request timeout")))
        assertFalse(isPermanentVoteError(null))
    }

    @Test
    fun `server rule violations are permanent`() {
        listOf("room is not in voting state", "not a participant", "invalid choice id", "pick exactly one choice", "duplicate choice in vote", "only the host can end voting")
            .forEach { assertTrue(it, isPermanentVoteError(RuntimeException(it))) }
    }

    @Test
    fun `already voted counts as delivered`() {
        assertTrue(isAlreadyVoted(RuntimeException("already voted")))
        assertFalse(isAlreadyVoted(RuntimeException("timeout")))
        assertFalse(isPermanentVoteError(RuntimeException("already voted")))
    }

    @Test
    fun `retry delay grows and is capped`() {
        assertEquals(2_000L, retryDelayMs(1))
        assertEquals(6_000L, retryDelayMs(3))
        assertEquals(10_000L, retryDelayMs(50))
        assertEquals(2_000L, retryDelayMs(0))
        assertEquals(8_000L, retryDelayMs(10, capMs = 8_000L))
    }
}
