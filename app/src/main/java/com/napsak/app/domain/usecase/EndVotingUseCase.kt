package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class EndVotingUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    /** Returns the winner choice id stored by the server. */
    suspend operator fun invoke(roomId: String, winnerChoiceId: String? = null): Result<String> {
        return repository.endVotingAndSetWinner(roomId, winnerChoiceId)
    }
}
