package com.napsak.app.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
import com.napsak.app.domain.usecase.LeaveRoomUseCase
import com.napsak.app.domain.usecase.ObserveRoomUseCase
import com.napsak.app.domain.usecase.SetParticipantReadyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val observeRoomUseCase: ObserveRoomUseCase,
    private val setParticipantReadyUseCase: SetParticipantReadyUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()

    /** True once a room we were in disappears (host left / room deleted). */
    private val _roomClosed = MutableStateFlow(false)
    val roomClosed: StateFlow<Boolean> = _roomClosed.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch {
            roomRepository.getSavedUserId().collect { id ->
                _currentUserId.value = id ?: ""
            }
        }
    }

    fun observeRoom(roomId: String) {
        viewModelScope.launch {
            observeRoomUseCase(roomId).collect { room ->
                if (room == null && _room.value != null) _roomClosed.value = true
                _room.value = room
            }
        }
    }

    fun suggestChoice(roomId: String, name: String, details: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(roomRepository.suggestChoice(roomId, name, details).isSuccess) }
    }

    fun dismissSuggestion(roomId: String, suggestionId: String) {
        viewModelScope.launch { roomRepository.dismissSuggestion(roomId, suggestionId) }
    }

    fun leaveRoom(roomId: String) {
        viewModelScope.launch { leaveRoomUseCase(roomId) }
    }

    fun toggleReady(roomId: String, isReady: Boolean) {
        val userId = _currentUserId.value
        if (roomId.isNotBlank() && userId.isNotBlank()) {
            viewModelScope.launch {
                setParticipantReadyUseCase(roomId, userId, isReady)
            }
        }
    }
}
