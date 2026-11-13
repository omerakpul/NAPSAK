package com.napsak.app.domain.model

import kotlinx.serialization.Serializable

enum class RoomState {
    WAITING,
    READY,
    VOTING,
    RESULT
}

/** LIKE: like any options. SINGLE: pick exactly one. RANK: order all options (Borda points). */
enum class VoteMode { LIKE, SINGLE, RANK }

/** An option proposed by a participant; only the host (and the author) can see it. */
@Serializable
data class Suggestion(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val details: String = ""
)

@Serializable
data class Participant(
    val id: String = "",
    val name: String = "",
    val isReady: Boolean = false,
    val hasVoted: Boolean = false
)

@Serializable
data class Choice(
    val id: String = "",
    val name: String = "",
    val details: String = "",
    val imageUrl: String? = null,
    val voteCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = ""
)

@Serializable
data class Room(
    val id: String = "",
    val hostId: String = "",
    val state: RoomState = RoomState.WAITING,
    val winnerChoiceId: String? = null,
    val createdAt: Long = 0L,
    val participants: Map<String, Participant> = emptyMap(),
    val choices: Map<String, Choice> = emptyMap(),
    val voteMode: VoteMode = VoteMode.LIKE,
    val suggestions: List<Suggestion> = emptyList()
)

@Serializable
data class SavedChoiceList(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val choices: List<Choice> = emptyList(),
    val imageUrl: String? = null
)
