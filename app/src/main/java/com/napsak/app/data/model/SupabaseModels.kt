package com.napsak.app.data.model

import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Participant
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.model.Suggestion
import com.napsak.app.domain.model.VoteMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ParticipantDto(
    @SerialName("room_id") val roomId: String = "",
    @SerialName("user_id") val userId: String,
    val name: String = "",
    @SerialName("is_ready") val isReady: Boolean = false,
    @SerialName("has_voted") val hasVoted: Boolean = false
) {
    fun toDomain() = Participant(id = userId, name = name, isReady = isReady, hasVoted = hasVoted)
}

@Serializable
data class ChoiceDto(
    val id: String,
    val name: String = "",
    val details: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("vote_count") val voteCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = ""
) {
    fun toDomain() = Choice(
        id = id,
        name = name,
        details = details,
        imageUrl = imageUrl,
        voteCount = voteCount,
        latitude = latitude,
        longitude = longitude,
        category = category
    )
}

@Serializable
data class SuggestionDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    val details: String = ""
) {
    fun toDomain() = Suggestion(id = id, userId = userId, name = name, details = details)
}

@Serializable
data class RoomDto(
    val id: String,
    @SerialName("host_id") val hostId: String,
    val state: String = "WAITING",
    @SerialName("winner_choice_id") val winnerChoiceId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("vote_mode") val voteMode: String = "LIKE"
) {
    fun toDomain(
        participants: List<ParticipantDto>,
        choices: List<ChoiceDto>,
        suggestions: List<SuggestionDto> = emptyList()
    ) = Room(
        id = id,
        hostId = hostId,
        state = runCatching { RoomState.valueOf(state) }.getOrDefault(RoomState.WAITING),
        winnerChoiceId = winnerChoiceId,
        createdAt = createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L,
        participants = participants.associate { it.userId to it.toDomain() },
        choices = choices.associate { it.id to it.toDomain() }, // keeps server order (position)
        voteMode = runCatching { VoteMode.valueOf(voteMode) }.getOrDefault(VoteMode.LIKE),
        suggestions = suggestions.map { it.toDomain() }
    )
}
