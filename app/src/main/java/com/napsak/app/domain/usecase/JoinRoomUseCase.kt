package com.napsak.app.domain.usecase

import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    operator fun invoke(roomId: String, participantName: String): Flow<Result<Room>> {
        return repository.joinRoom(roomId, participantName)
    }
}
