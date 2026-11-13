package com.napsak.app.ui.screens.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.HistoryEntry
import com.napsak.app.domain.model.VoteMode
import java.util.UUID
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.repository.RoomRepository
import com.napsak.app.domain.usecase.DeleteRoomUseCase
import com.napsak.app.domain.usecase.EndVotingUseCase
import com.napsak.app.domain.usecase.LeaveRoomUseCase
import com.napsak.app.domain.usecase.ObserveRoomUseCase
import com.napsak.app.domain.usecase.SubmitVotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.napsak.app.domain.util.isAlreadyVoted
import com.napsak.app.domain.util.isPermanentVoteError
import com.napsak.app.domain.util.retryDelayMs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel scoped to the Activity, used to pass data and synchronize states
 * in real time between CreateChoices → Voting → Result screens.
 */
@HiltViewModel
class SharedSessionViewModel @Inject constructor(
    private val observeRoomUseCase: ObserveRoomUseCase,
    private val submitVotesUseCase: SubmitVotesUseCase,
    private val endVotingUseCase: EndVotingUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val preferences: UserPreferencesDataSource,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _choices = MutableStateFlow<List<Choice>>(emptyList())
    val choices: StateFlow<List<Choice>> = _choices.asStateFlow()

    private val _voteMode = MutableStateFlow(VoteMode.LIKE)
    val voteMode: StateFlow<VoteMode> = _voteMode.asStateFlow()

    fun setVoteMode(mode: VoteMode) {
        _voteMode.value = mode
    }

    /** Saves the finished decision to the on-device history (once per room). */
    fun recordHistory(roomId: String) {
        val winner = _winnerChoice.value ?: return
        val room = _currentRoom.value
        viewModelScope.launch {
            preferences.addHistory(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    roomId = roomId,
                    winnerName = winner.name,
                    winnerDetails = winner.details,
                    winnerCategory = winner.category,
                    winnerImageUrl = winner.imageUrl,
                    winnerVotes = winner.voteCount,
                    mode = room?.voteMode ?: _voteMode.value,
                    participantCount = room?.participants?.size ?: 1,
                    options = (room?.choices?.values?.toList() ?: _choices.value).map { it.name },
                    finishedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private val _winnerChoice = MutableStateFlow<Choice?>(null)
    val winnerChoice: StateFlow<Choice?> = _winnerChoice.asStateFlow()

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            roomRepository.getSavedUserId().collect { id ->
                _currentUserId.value = id ?: ""
            }
        }
    }

    fun setChoices(choices: List<Choice>) {
        _choices.value = choices
    }

    fun setWinner(choice: Choice) {
        _winnerChoice.value = choice
    }

    // Start observing room updates in real time
    fun observeRoom(roomId: String) {
        viewModelScope.launch {
            observeRoomUseCase(roomId).collect { room ->
                _currentRoom.value = room
                if (room != null) {
                    _voteMode.value = room.voteMode
                    val isHost = room.hostId == _currentUserId.value
                    // If participant and voting state transitions to RESULT, load the declared winner
                    if (!isHost && room.state == RoomState.RESULT && room.winnerChoiceId != null) {
                        val winner = room.choices[room.winnerChoiceId]
                        if (winner != null) {
                            _winnerChoice.value = winner
                        }
                    }
                }
            }
        }
    }

    // Submit user votes and, if current user is the host, finalize voting room-wide
    fun submitVotesAndDeclareWinner(
        roomId: String,
        likedChoices: List<Choice>,
        winner: Choice?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val likedIds = likedChoices.map { it.id }
            submitVotesUseCase(roomId, likedIds)
            
            val room = _currentRoom.value
            val isHost = room?.hostId == _currentUserId.value
            if (isHost && winner != null) {
                // End voting; the server stores (and returns) the authoritative winner
                val result = endVotingUseCase(roomId, winner.id)
                _winnerChoice.value = resolveWinner(result.getOrNull(), winner)
                onComplete()
            } else {
                onComplete()
            }
        }
    }

    private val _votePending = MutableStateFlow(false)

    /** True while a vote could not be delivered yet (e.g. offline) and is being retried. */
    val votePending: StateFlow<Boolean> = _votePending.asStateFlow()
    private var submitJob: Job? = null

    /** Sends the vote and keeps retrying on network failures; rule violations are not retried. */
    fun submitVotes(roomId: String, likedIds: List<String>) {
        if (submitJob?.isActive == true) return
        submitJob = viewModelScope.launch {
            var attempt = 0
            while (true) {
                attempt++
                val result = submitVotesUseCase(roomId, likedIds)
                val error = result.exceptionOrNull()
                if (result.isSuccess || isAlreadyVoted(error)) {
                    _votePending.value = false
                    return@launch
                }
                if (isPermanentVoteError(error) || attempt >= 30) {
                    _votePending.value = false
                    return@launch
                }
                _votePending.value = true
                delay(retryDelayMs(attempt))
            }
        }
    }

    private suspend fun endVotingWithRetry(roomId: String, winnerId: String?): Result<String> {
        var last: Result<String> = Result.failure(IllegalStateException("not attempted"))
        for (attempt in 1..8) {
            last = endVotingUseCase(roomId, winnerId)
            if (last.isSuccess || isPermanentVoteError(last.exceptionOrNull())) return last
            delay(retryDelayMs(attempt, capMs = 8_000L))
        }
        return last
    }

    fun declareWinner(roomId: String, winner: Choice, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = endVotingWithRetry(roomId, winner.id)
            _winnerChoice.value = resolveWinner(result.getOrNull(), winner)
            onComplete()
        }
    }

    /** Host-only: close voting now (e.g. someone left or went idle). Server picks among the top-voted. */
    fun finishVotingNow(roomId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = endVotingWithRetry(roomId, null)
            val winner = resolveWinner(result.getOrNull(), null)
            if (winner != null) {
                _winnerChoice.value = winner
                onComplete()
            }
        }
    }

    private fun resolveWinner(winnerId: String?, fallback: Choice?): Choice? =
        winnerId?.let { _currentRoom.value?.choices?.get(it) } ?: fallback

    fun leaveRoom(roomId: String) {
        viewModelScope.launch { leaveRoomUseCase(roomId) }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            deleteRoomUseCase(roomId)
        }
    }

    fun clear() {
        _choices.value = emptyList()
        _winnerChoice.value = null
        _currentRoom.value = null
    }
}
