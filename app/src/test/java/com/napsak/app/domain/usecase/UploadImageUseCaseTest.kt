package com.napsak.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.napsak.app.domain.model.UploadResult
import com.napsak.app.domain.repository.ImageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadImageUseCaseTest {

    private lateinit var useCase: UploadImageUseCase
    private val repository: ImageRepository = mockk()
    private val context: Context = mockk()
    private val uri: Uri = mockk()

    @Before
    fun setUp() {
        useCase = UploadImageUseCase(repository)
    }

    @Test
    fun `invoke should call repository uploadImage and return success`() = runTest {
        // Arrange
        val expectedUrl = "https://imgbb.com/sample.jpg"
        coEvery { repository.uploadImage(context, uri) } returns UploadResult.Success(expectedUrl)

        // Act
        val result = useCase(context, uri)

        // Assert
        assertEquals(UploadResult.Success(expectedUrl), result)
        coVerify(exactly = 1) { repository.uploadImage(context, uri) }
    }

    @Test
    fun `invoke should return FileTooLarge when repository returns FileTooLarge`() = runTest {
        // Arrange
        coEvery { repository.uploadImage(context, uri) } returns UploadResult.FileTooLarge

        // Act
        val result = useCase(context, uri)

        // Assert
        assertEquals(UploadResult.FileTooLarge, result)
        coVerify(exactly = 1) { repository.uploadImage(context, uri) }
    }

    @Test
    fun `invoke should return Failure when repository returns Failure`() = runTest {
        // Arrange
        coEvery { repository.uploadImage(context, uri) } returns UploadResult.Failure

        // Act
        val result = useCase(context, uri)

        // Assert
        assertEquals(UploadResult.Failure, result)
        coVerify(exactly = 1) { repository.uploadImage(context, uri) }
    }
}
