package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class EndVotingUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    suspend operator fun invoke(roomId: String, winnerChoiceId: String): Result<Unit> {
        return repository.endVotingAndSetWinner(roomId, winnerChoiceId)
    }
}
