package com.napsak.app.domain.repository

import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.VoteMode
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun createRoom(hostName: String): Flow<Result<Room>>
    fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>>
    fun observeRoom(roomId: String): Flow<Room?>
    suspend fun setParticipantReady(roomId: String, userId: String, isReady: Boolean): Result<Unit>
    suspend fun startVotingWithChoices(roomId: String, choices: List<Choice>, mode: VoteMode = VoteMode.LIKE): Result<Unit>

    /** A participant proposes an option to the host (lobby only, max 5 each). */
    suspend fun suggestChoice(roomId: String, name: String, details: String = ""): Result<Unit>

    /** The host can dismiss any suggestion; a participant only their own. */
    suspend fun dismissSuggestion(roomId: String, suggestionId: String): Result<Unit>
    suspend fun submitVotes(roomId: String, likedChoiceIds: List<String>): Result<Unit>
    /** Ends voting. [winnerChoiceId] is the host's raffle pick (null = let the server pick). Returns the winner id the server stored. */
    suspend fun endVotingAndSetWinner(roomId: String, winnerChoiceId: String?): Result<String>
    suspend fun leaveRoom(roomId: String): Result<Unit>

    /** A room I am still in that I can step back into (lobby, or voting I have not finished), if any. */
    suspend fun findResumableRoomId(): String?
    suspend fun deleteRoom(roomId: String): Result<Unit>
    
    // Local User Credentials (DataStore)
    fun getSavedUserId(): Flow<String?>
    fun getSavedUsername(): Flow<String?>
    suspend fun saveUserCredentials(userId: String, username: String)
}
