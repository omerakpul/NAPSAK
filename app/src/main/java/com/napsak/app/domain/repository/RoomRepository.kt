package com.napsak.app.domain.repository

import com.napsak.app.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun createRoom(hostName: String): Flow<Result<Room>>
    fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>>
    fun observeRoom(roomId: String): Flow<Room?>
    suspend fun setParticipantReady(roomId: String, userId: String, isReady: Boolean): Result<Unit>
    suspend fun startVoting(roomId: String): Result<Unit>
    suspend fun submitVote(roomId: String, userId: String, optionId: String, isLiked: Boolean): Result<Unit>
    
    // Local User Credentials (DataStore)
    fun getSavedUserId(): Flow<String?>
    fun getSavedUsername(): Flow<String?>
    suspend fun saveUserCredentials(userId: String, username: String)
}
