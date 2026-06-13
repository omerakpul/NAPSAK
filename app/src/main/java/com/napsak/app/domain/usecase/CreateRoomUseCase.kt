package com.napsak.app.domain.usecase

import com.napsak.app.domain.model.Room
import com.napsak.app.domain.repository.RoomRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateRoomUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    operator fun invoke(hostName: String): Flow<Result<Room>> {
        return repository.createRoom(hostName)
    }
}
