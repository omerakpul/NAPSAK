package com.napsak.app.data.repository

import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.repository.RoomRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor() : RoomRepository {
    
    private val localRoomState = mutableMapOf<String, Room>()
    private var savedUserId: String? = null
    private var savedUsername: String? = null

    override fun createRoom(hostName: String): Flow<Result<Room>> = flow {
        val roomId = (100000..999999).random().toString()
        val userId = savedUserId ?: UUID.randomUUID().toString()
        savedUserId = userId
        savedUsername = hostName
        
        val newRoom = Room(
            id = roomId,
            hostId = userId,
            state = RoomState.WAITING,
            createdAt = System.currentTimeMillis()
        )
        localRoomState[roomId] = newRoom
        emit(Result.success(newRoom))
    }

    override fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>> = flow {
        val userId = savedUserId ?: UUID.randomUUID().toString()
        savedUserId = userId
        savedUsername = participantName
        
        val room = localRoomState[roomId]
        if (room != null) {
            emit(Result.success(room))
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
        // Stub participant state update
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

    override fun getSavedUserId(): Flow<String?> = flow {
        emit(savedUserId)
    }

    override fun getSavedUsername(): Flow<String?> = flow {
        emit(savedUsername)
    }

    override suspend fun saveUserCredentials(userId: String, username: String) {
        savedUserId = userId
        savedUsername = username
    }
}
