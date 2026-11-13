package com.napsak.app.data.repository

import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.data.model.ChoiceDto
import com.napsak.app.data.model.ParticipantDto
import com.napsak.app.data.model.RoomDto
import com.napsak.app.data.remote.SupabaseSession
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SupabaseSession,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : RoomRepository {

    private suspend fun rpc(function: String, params: JsonObject = JsonObject(emptyMap())) =
        supabase.postgrest.rpc(function, params)

    private fun Throwable.friendly(): Throwable = when {
        message?.contains("room not found", ignoreCase = true) == true -> Exception("Oda bulunamadı")
        message?.contains("voting already started", ignoreCase = true) == true -> Exception("Oylama başladı, odaya girilemez")
        message?.contains("room is full", ignoreCase = true) == true -> Exception("Oda dolu")
        else -> this
    }

    private suspend fun fetchRoom(roomId: String): Room? {
        val room = supabase.from("rooms").select { filter { eq("id", roomId) } }
            .decodeList<RoomDto>().firstOrNull() ?: return null
        val participants = supabase.from("participants").select { filter { eq("room_id", roomId) } }
            .decodeList<ParticipantDto>()
        val choices = supabase.from("choices").select {
            filter { eq("room_id", roomId) }
            order("position", Order.ASCENDING)
        }.decodeList<ChoiceDto>()
        return room.toDomain(participants, choices)
    }

    override fun createRoom(hostName: String): Flow<Result<Room>> = flow {
        emit(runCatching {
            val userId = session.userId()
            userPreferencesDataSource.saveUserCredentials(userId, hostName)
            val code = rpc("create_room", buildJsonObject { put("p_name", hostName) }).decodeAs<String>()
            fetchRoom(code) ?: throw Exception("Oda okunamadı")
        }.recoverCatching { throw it.friendly() })
    }

    override fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>> = flow {
        emit(runCatching {
            val userId = session.userId()
            userPreferencesDataSource.saveUserCredentials(userId, participantName)
            rpc("join_room", buildJsonObject {
                put("p_room", roomId)
                put("p_name", participantName)
            })
            fetchRoom(roomId) ?: throw Exception("Oda okunamadı")
        }.recoverCatching { throw it.friendly() })
    }

    /**
     * Emits the full room whenever anything in it changes. Realtime events are only a trigger;
     * the snapshot is always re-read, plus a slow safety refresh in case an event is missed.
     */
    override fun observeRoom(roomId: String): Flow<Room?> = channelFlow {
        session.userId() // make sure the realtime socket is authenticated before subscribing
        val channel = supabase.channel("room-$roomId-${System.nanoTime()}")
        val roomChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "rooms"
            filter("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, roomId)
        }
        val participantChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "participants"
            filter("room_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, roomId)
        }
        val choiceChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "choices"
            filter("room_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, roomId)
        }

        val refresh = Channel<Unit>(Channel.CONFLATED)
        launch { roomChanges.collect { refresh.trySend(Unit) } }
        launch { participantChanges.collect { refresh.trySend(Unit) } }
        launch { choiceChanges.collect { refresh.trySend(Unit) } }
        launch { while (true) { delay(15_000); refresh.trySend(Unit) } }
        launch {
            for (ignored in refresh) {
                runCatching { fetchRoom(roomId) }.onSuccess { send(it) }
            }
        }

        try {
            runCatching { channel.subscribe(blockUntilSubscribed = true) }
            refresh.trySend(Unit)
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }.distinctUntilChanged()

    override suspend fun setParticipantReady(roomId: String, userId: String, isReady: Boolean): Result<Unit> =
        runCatching {
            rpc("set_ready", buildJsonObject {
                put("p_room", roomId)
                put("p_ready", isReady)
            })
            Unit
        }

    override suspend fun startVotingWithChoices(roomId: String, choices: List<Choice>): Result<Unit> =
        runCatching {
            val payload = JsonArray(choices.map { c ->
                buildJsonObject {
                    put("id", c.id)
                    put("name", c.name)
                    put("details", c.details)
                    put("image_url", c.imageUrl?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("latitude", c.latitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("longitude", c.longitude?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("category", c.category)
                }
            })
            rpc("start_voting", buildJsonObject {
                put("p_room", roomId)
                put("p_choices", payload)
            })
            Unit
        }

    override suspend fun submitVotes(roomId: String, likedChoiceIds: List<String>): Result<Unit> =
        runCatching {
            rpc("submit_votes", buildJsonObject {
                put("p_room", roomId)
                put("p_choice_ids", JsonArray(likedChoiceIds.map { JsonPrimitive(it) }))
            })
            Unit
        }

    override suspend fun endVotingAndSetWinner(roomId: String, winnerChoiceId: String?): Result<String> =
        runCatching {
            rpc("end_voting", buildJsonObject {
                put("p_room", roomId)
                put("p_winner", winnerChoiceId?.let { JsonPrimitive(it) } ?: JsonNull)
            }).decodeAs<String>()
        }

    override suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
        rpc("leave_room", buildJsonObject { put("p_room", roomId) })
        Unit
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
        rpc("delete_room", buildJsonObject { put("p_room", roomId) })
        Unit
    }

    // Local user credentials (DataStore)
    // The signed-in Supabase user is the single source of truth for "who am I".
    override fun getSavedUserId(): Flow<String?> = supabase.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()
    override fun getSavedUsername(): Flow<String?> = userPreferencesDataSource.usernameFlow
    override suspend fun saveUserCredentials(userId: String, username: String) {
        userPreferencesDataSource.saveUserCredentials(userId, username)
    }
}
