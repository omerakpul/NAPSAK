package com.napsak.app.ui.screens.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
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
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room.asStateFlow()

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
                _room.value = room
            }
        }
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
