package com.napsak.app.data.model

import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Participant
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState

import com.google.firebase.database.PropertyName

data class ParticipantDto(
    var id: String = "",
    var name: String = "",
    @get:PropertyName("isReady") @set:PropertyName("isReady")
    var isReady: Boolean = false,
    @get:PropertyName("ready") @set:PropertyName("ready")
    var readyCompat: Boolean = false,
    var hasVoted: Boolean = false
) {
    fun toDomain(): Participant = Participant(
        id = id,
        name = name,
        isReady = isReady || readyCompat,
        hasVoted = hasVoted
    )
}

data class ChoiceDto(
    var id: String = "",
    var name: String = "",
    var details: String = "",
    var imageUrl: String? = null,
    var voteCount: Int = 0,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var category: String = ""
) {
    fun toDomain(): Choice = Choice(
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

data class RoomDto(
    var id: String = "",
    var hostId: String = "",
    var state: String = "WAITING",
    var winnerChoiceId: String? = null,
    var createdAt: Long = 0L,
    var participants: Map<String, ParticipantDto>? = null,
    var choices: Map<String, ChoiceDto>? = null
) {
    fun toDomain(): Room {
        val roomState = try {
            RoomState.valueOf(state)
        } catch (e: Exception) {
            RoomState.WAITING
        }
        return Room(
            id = id,
            hostId = hostId,
            state = roomState,
            winnerChoiceId = winnerChoiceId,
            createdAt = createdAt,
            participants = participants?.mapValues { it.value.toDomain() } ?: emptyMap(),
            choices = choices?.mapValues { it.value.toDomain() } ?: emptyMap()
        )
    }
}
