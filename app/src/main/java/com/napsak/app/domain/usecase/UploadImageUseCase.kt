package com.napsak.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.napsak.app.domain.model.UploadResult
import com.napsak.app.domain.repository.ImageRepository
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val repository: ImageRepository
) {
    suspend operator fun invoke(context: Context, uri: Uri): UploadResult {
        return repository.uploadImage(context, uri)
    }
}
