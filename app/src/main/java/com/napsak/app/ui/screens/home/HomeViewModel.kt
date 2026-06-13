package com.napsak.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
import com.napsak.app.domain.usecase.CreateRoomUseCase
import com.napsak.app.domain.usecase.JoinRoomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createRoomUseCase: CreateRoomUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _savedUsername = MutableStateFlow("")
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    init {
        viewModelScope.launch {
            roomRepository.getSavedUsername().collect { name ->
                _savedUsername.value = name ?: ""
            }
        }
    }

    fun createRoom(name: String, onResult: (Result<Room>) -> Unit) {
        viewModelScope.launch {
            createRoomUseCase(name).collect { result ->
                onResult(result)
            }
        }
    }

    fun joinRoom(roomId: String, name: String, onResult: (Result<Room>) -> Unit) {
        viewModelScope.launch {
            joinRoomUseCase(roomId, name).collect { result ->
                onResult(result)
            }
        }
    }
}
