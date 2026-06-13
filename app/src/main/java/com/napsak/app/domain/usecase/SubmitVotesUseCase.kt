package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import javax.inject.Inject

class SubmitVotesUseCase @Inject constructor(
    private val repository: RoomRepository
) {
    suspend operator fun invoke(roomId: String, likedChoiceIds: List<String>): Result<Unit> {
        return repository.submitVotes(roomId, likedChoiceIds)
    }
}
