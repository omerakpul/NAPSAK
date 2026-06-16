package com.napsak.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.data.model.RoomDto
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Participant
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.repository.RoomRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : RoomRepository {

    private val db = FirebaseDatabase.getInstance("https://napsak-official-default-rtdb.europe-west1.firebasedatabase.app")
    private val roomsRef = db.getReference("rooms")
    private val auth = FirebaseAuth.getInstance()

    private suspend fun getOrSignInUserId(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        return suspendCancellableCoroutine { continuation ->
            auth.signInAnonymously()
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: UUID.randomUUID().toString()
                    continuation.resume(uid)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(exception))
                    }
                }
        }
    }

    private fun DataSnapshot.toRoom(): Room? {
        val dto = getValue(RoomDto::class.java) ?: return null
        return dto.toDomain()
    }

    override fun createRoom(hostName: String): Flow<Result<Room>> = callbackFlow {
        launch {
            val userId = try {
                getOrSignInUserId()
            } catch (e: Exception) {
                trySend(Result.failure(e))
                close()
                return@launch
            }
            userPreferencesDataSource.saveUserCredentials(userId, hostName)
            
            val host = Participant(id = userId, name = hostName, isReady = true)
            val maxRetries = 5
            
            fun tryCreateRoom(attempt: Int) {
                if (attempt > maxRetries) {
                    trySend(Result.failure(Exception("Oda oluşturulamadı: benzersiz ID bulunamadı")))
                    close()
                    return
                }
                
                val roomId = (100000..999999).random().toString()
                val roomRef = roomsRef.child(roomId)
                
                roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Room ID already exists, retry with a new ID
                            tryCreateRoom(attempt + 1)
                        } else {
                            val newRoom = Room(
                                id = roomId,
                                hostId = userId,
                                state = RoomState.WAITING,
                                createdAt = System.currentTimeMillis(),
                                participants = mapOf(userId to host)
                            )
                            
                            roomRef.setValue(newRoom)
                                .addOnSuccessListener {
                                    roomRef.child("participants").child(userId).onDisconnect().removeValue()
                                    trySend(Result.success(newRoom))
                                    close()
                                }
                                .addOnFailureListener { e ->
                                    trySend(Result.failure(e))
                                    close()
                                }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        trySend(Result.failure(error.toException()))
                        close()
                    }
                })
            }
            
            tryCreateRoom(1)
        }
        awaitClose { }
    }

    override fun joinRoom(roomId: String, participantName: String): Flow<Result<Room>> = callbackFlow {
        launch {
            val userId = try {
                getOrSignInUserId()
            } catch (e: Exception) {
                trySend(Result.failure(e))
                close()
                return@launch
            }
            userPreferencesDataSource.saveUserCredentials(userId, participantName)
            
            val roomRef = roomsRef.child(roomId)
            
            // First check if the room exists
            roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        trySend(Result.failure(Exception("Oda bulunamadı")))
                        close()
                        return
                    }
                    
                    // Write ONLY the participant node to avoid race conditions
                    val newParticipant = Participant(id = userId, name = participantName, isReady = false)
                    roomRef.child("participants").child(userId).setValue(newParticipant)
                        .addOnSuccessListener {
                            // Register onDisconnect on the specific participant path
                            roomRef.child("participants").child(userId).onDisconnect().removeValue()
                            
                            // Read the full room once more to return the result
                            roomRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(updatedSnapshot: DataSnapshot) {
                                    val updatedRoom = updatedSnapshot.toRoom()
                                    if (updatedRoom != null) {
                                        trySend(Result.success(updatedRoom))
                                    } else {
                                        trySend(Result.failure(Exception("Oda okunamadı")))
                                    }
                                    close()
                                }
                                
                                override fun onCancelled(error: DatabaseError) {
                                    trySend(Result.failure(error.toException()))
                                    close()
                                }
                            })
                        }
                        .addOnFailureListener { e ->
                            trySend(Result.failure(e))
                            close()
                        }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    trySend(Result.failure(error.toException()))
                    close()
                }
            })
        }
        awaitClose { }
    }

    override fun observeRoom(roomId: String): Flow<Room?> = callbackFlow {
        val roomRef = roomsRef.child(roomId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.toRoom()
                trySend(room)
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Ignore
            }
        }
        
        roomRef.addValueEventListener(listener)
        
        awaitClose {
            roomRef.removeEventListener(listener)
        }
    }

    override suspend fun setParticipantReady(
        roomId: String,
        userId: String,
        isReady: Boolean
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val participantRef = roomsRef.child(roomId).child("participants").child(userId)
        
        val updates = mapOf(
            "isReady" to isReady,
            "ready" to isReady
        )
        
        participantRef.updateChildren(updates)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
    }

    override suspend fun startVotingWithChoices(roomId: String, choices: List<Choice>): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val roomRef = roomsRef.child(roomId)
        
        val choicesMap = choices.associateBy { it.id }
        val updates = mapOf(
            "choices" to choicesMap,
            "state" to RoomState.VOTING.name
        )
        
        roomRef.updateChildren(updates)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
    }

    override suspend fun submitVotes(roomId: String, likedChoiceIds: List<String>): Result<Unit> {
        val userId = try {
            getOrSignInUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        roomsRef.child(roomId).child("participants").child(userId).child("hasVoted").setValue(true)
        
        if (likedChoiceIds.isEmpty()) {
            return Result.success(Unit)
        }
        
        return suspendCancellableCoroutine { continuation ->
            val choicesRef = roomsRef.child(roomId).child("choices")
            
            var completedCount = 0
            var hasFailed = false
            
            likedChoiceIds.forEach { choiceId ->
            choicesRef.child(choiceId).child("voteCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentVotes = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentVotes + 1
                    return Transaction.success(currentData)
                }
                
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    completedCount++
                    if (error != null) {
                        hasFailed = true
                    }
                    
                    if (completedCount == likedChoiceIds.size) {
                        if (hasFailed) {
                            if (continuation.isActive) continuation.resume(Result.failure(Exception("Bazı oylar kaydedilemedi")))
                        } else {
                            if (continuation.isActive) continuation.resume(Result.success(Unit))
                        }
                    }
                }
            })
        }
    }
}

    override suspend fun endVotingAndSetWinner(roomId: String, winnerChoiceId: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val roomRef = roomsRef.child(roomId)
        
        val updates = mapOf(
            "winnerChoiceId" to winnerChoiceId,
            "state" to RoomState.RESULT.name
        )
        
        roomRef.updateChildren(updates)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        roomsRef.child(roomId).removeValue()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
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
