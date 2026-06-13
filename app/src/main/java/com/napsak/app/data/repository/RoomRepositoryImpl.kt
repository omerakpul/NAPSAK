package com.napsak.app.data.repository

import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.domain.model.Participant
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.repository.RoomRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : RoomRepository {
    
    private val localRoomState = mutableMapOf<String, Room>()

    override fun createRoom(hostName: String): Flow<Result<Room>> = flow {
        val userId = userPreferencesDataSource.userIdFlow.firstOrNull() ?: UUID.randomUUID().toString()
        userPreferencesDataSource.saveUserCredentials(userId, hostName)
        
        val roomId = (100000..999999).random().toString()
        val host = Participant(id = userId, name = hostName, isReady = true)
        
        val newRoom = Room(
            id = roomId,
            hostId = userId,
            state = RoomState.WAITING,
            createdAt = System.currentTimeMillis(),
            participants = mapOf(userId to host)
        )
        localRoomState[roomId] = newRoom
        emit(Result.success(newRoom))
    }

    override fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>> = flow {
        val userId = userPreferencesDataSource.userIdFlow.firstOrNull() ?: UUID.randomUUID().toString()
        userPreferencesDataSource.saveUserCredentials(userId, participantName)
        
        val room = localRoomState[roomId]
        if (room != null) {
            val newParticipant = Participant(id = userId, name = participantName, isReady = false)
            val updatedParticipants = room.participants.toMutableMap().apply {
                put(userId, newParticipant)
            }
            val updatedRoom = room.copy(participants = updatedParticipants)
            localRoomState[roomId] = updatedRoom
            emit(Result.success(updatedRoom))
        } else {
            emit(Result.failure(Exception("Oda bulunamadı")))
        }
    }

    override fun observeRoom(roomId: String): Flow<Room?> = flow {
        while (true) {
            emit(localRoomState[roomId])
            delay(1000)
        }
    }

    override suspend fun setParticipantReady(
        roomId: String,
        userId: String,
        isReady: Boolean
    ): Result<Unit> {
        val room = localRoomState[roomId] ?: return Result.failure(Exception("Oda bulunamadı"))
        val participant = room.participants[userId] ?: return Result.failure(Exception("Katılımcı bulunamadı"))
        val updatedParticipants = room.participants.toMutableMap().apply {
            put(userId, participant.copy(isReady = isReady))
        }
        localRoomState[roomId] = room.copy(participants = updatedParticipants)
        return Result.success(Unit)
    }

    override suspend fun startVoting(roomId: String): Result<Unit> {
        val room = localRoomState[roomId] ?: return Result.failure(Exception("Oda bulunamadı"))
        localRoomState[roomId] = room.copy(state = RoomState.VOTING)
        return Result.success(Unit)
    }

    override suspend fun submitVote(
        roomId: String,
        userId: String,
        optionId: String,
        isLiked: Boolean
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getSavedUserId(): Flow<String?> {
        return userPreferencesDataSource.userIdFlow
    }

    override fun getSavedUsername(): Flow<String?> {
        return userPreferencesDataSource.usernameFlow
    }

    override suspend fun saveUserCredentials(userId: String, username: String) {
        userPreferencesDataSource.saveUserCredentials(userId, username)
    }
}
