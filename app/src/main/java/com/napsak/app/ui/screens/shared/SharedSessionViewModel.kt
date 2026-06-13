package com.napsak.app.ui.screens.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.repository.RoomRepository
import com.napsak.app.domain.usecase.EndVotingUseCase
import com.napsak.app.domain.usecase.ObserveRoomUseCase
import com.napsak.app.domain.usecase.SubmitVotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _choices = MutableStateFlow<List<Choice>>(emptyList())
    val choices: StateFlow<List<Choice>> = _choices.asStateFlow()

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
                // End voting and sync winner Choice to Firebase database
                endVotingUseCase(roomId, winner.id)
                _winnerChoice.value = winner
                onComplete()
            } else {
                onComplete()
            }
        }
    }

    fun clear() {
        _choices.value = emptyList()
        _winnerChoice.value = null
        _currentRoom.value = null
    }
}
