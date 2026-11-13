package com.napsak.app.domain.util

private val PERMANENT_MARKERS = listOf(
    "not in voting", "not a participant", "invalid choice", "pick exactly", "duplicate",
    "room not found", "only the host", "invalid vote mode"
)

/** Rule violations from the server: retrying can never succeed. */
fun isPermanentVoteError(error: Throwable?): Boolean {
    val message = error?.message?.lowercase().orEmpty()
    return PERMANENT_MARKERS.any { it in message }
}

/** The vote already went through earlier (e.g. the reply was lost on a flaky connection). */
fun isAlreadyVoted(error: Throwable?): Boolean =
    error?.message?.contains("already voted", ignoreCase = true) == true

/** Delay before the next attempt: 2s, 4s, 6s ... capped at [capMs]. */
fun retryDelayMs(attempt: Int, capMs: Long = 10_000L): Long = minOf(2_000L * attempt.coerceAtLeast(1), capMs)
