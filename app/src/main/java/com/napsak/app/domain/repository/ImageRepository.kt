package com.napsak.app.domain.repository

import android.content.Context
import android.net.Uri
import com.napsak.app.domain.model.UploadResult

interface ImageRepository {
    suspend fun uploadImage(context: Context, uri: Uri): UploadResult
}
