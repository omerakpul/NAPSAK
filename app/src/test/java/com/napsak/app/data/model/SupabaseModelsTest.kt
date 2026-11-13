package com.napsak.app.data.model

import com.napsak.app.domain.model.RoomState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `room row from PostgREST maps to domain with participants and ordered choices`() {
        val room = json.decodeFromString<RoomDto>(
            """{"id":"123456","host_id":"u1","state":"VOTING","winner_choice_id":null,
                "created_at":"2026-11-13T09:45:21.635537+00:00","extra":"ignored"}"""
        )
        val participants = json.decodeFromString<List<ParticipantDto>>(
            """[{"room_id":"123456","user_id":"u1","name":"Ali","is_ready":true,"has_voted":false},
                {"room_id":"123456","user_id":"u2","name":"Veli","is_ready":false,"has_voted":true}]"""
        )
        val choices = json.decodeFromString<List<ChoiceDto>>(
            """[{"id":"b","name":"Sushi","vote_count":1,"latitude":41.0,"longitude":29.0},
                {"id":"a","name":"Pizza","vote_count":2,"image_url":"https://x/y.jpg"}]"""
        )

        val domain = room.toDomain(participants, choices)

        assertEquals(RoomState.VOTING, domain.state)
        assertEquals("u1", domain.hostId)
        assertTrue(domain.createdAt > 0)
        assertEquals(setOf("u1", "u2"), domain.participants.keys)
        assertEquals(true, domain.participants["u1"]?.isReady)
        assertEquals(true, domain.participants["u2"]?.hasVoted)
        // server order (position) is preserved
        assertEquals(listOf("b", "a"), domain.choices.keys.toList())
        assertEquals(2, domain.choices["a"]?.voteCount)
        assertEquals(41.0, domain.choices["b"]?.latitude)
        assertNull(domain.choices["b"]?.imageUrl)
    }

    @Test
    fun `unknown state falls back to WAITING and bad timestamp to zero`() {
        val room = RoomDto(id = "1", hostId = "h", state = "WHATEVER", createdAt = "not-a-date")
        val domain = room.toDomain(emptyList(), emptyList())
        assertEquals(RoomState.WAITING, domain.state)
        assertEquals(0L, domain.createdAt)
    }
}
