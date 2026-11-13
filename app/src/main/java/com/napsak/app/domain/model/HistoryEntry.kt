package com.napsak.app.domain.model

import kotlinx.serialization.Serializable

/** A finished decision, kept on the device only. */
@Serializable
data class HistoryEntry(
    val id: String,
    val roomId: String,
    val winnerName: String,
    val winnerDetails: String = "",
    val winnerCategory: String = "",
    val winnerImageUrl: String? = null,
    val winnerVotes: Int = 0,
    val mode: VoteMode = VoteMode.LIKE,
    val participantCount: Int = 1,
    val options: List<String> = emptyList(),
    val finishedAt: Long = 0L
)
