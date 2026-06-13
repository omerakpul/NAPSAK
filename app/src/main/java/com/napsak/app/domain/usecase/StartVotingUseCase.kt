package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class StartVotingUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    suspend operator fun invoke(roomId: String): Result<Unit> {
        return repository.startVoting(roomId)
    }
}
