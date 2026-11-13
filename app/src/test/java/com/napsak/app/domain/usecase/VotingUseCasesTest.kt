package com.napsak.app.domain.usecase

import com.napsak.app.domain.repository.RoomRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VotingUseCasesTest {

    private val repository: RoomRepository = mockk()

    @Test
    fun `end voting passes host pick and returns the server winner`() = runTest {
        coEvery { repository.endVotingAndSetWinner("1", "a") } returns Result.success("b")
        val result = EndVotingUseCase(repository)("1", "a")
        assertEquals("b", result.getOrNull())
        coVerify(exactly = 1) { repository.endVotingAndSetWinner("1", "a") }
    }

    @Test
    fun `end voting without a pick lets the server choose`() = runTest {
        coEvery { repository.endVotingAndSetWinner("1", null) } returns Result.success("c")
        assertEquals("c", EndVotingUseCase(repository)("1").getOrNull())
    }

    @Test
    fun `end voting surfaces failures`() = runTest {
        coEvery { repository.endVotingAndSetWinner(any(), any()) } returns Result.failure(Exception("boom"))
        assertTrue(EndVotingUseCase(repository)("1", "a").isFailure)
    }

    @Test
    fun `leave room delegates to repository`() = runTest {
        coEvery { repository.leaveRoom("1") } returns Result.success(Unit)
        assertTrue(LeaveRoomUseCase(repository)("1").isSuccess)
        coVerify { repository.leaveRoom("1") }
    }

    @Test
    fun `submit votes delegates the liked ids`() = runTest {
        coEvery { repository.submitVotes("1", listOf("a", "b")) } returns Result.success(Unit)
        assertTrue(SubmitVotesUseCase(repository)("1", listOf("a", "b")).isSuccess)
    }
}
