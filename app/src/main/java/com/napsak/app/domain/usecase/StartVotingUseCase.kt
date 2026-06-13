package com.napsak.app.domain.usecase

import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class StartVotingUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    suspend operator fun invoke(roomId: String, choices: List<Choice>): Result<Unit> {
        return repository.startVotingWithChoices(roomId, choices)
    }
}
