package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class SetParticipantReadyUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    suspend operator fun invoke(roomId: String, userId: String, isReady: Boolean): Result<Unit> {
        return repository.setParticipantReady(roomId, userId, isReady)
    }
}
