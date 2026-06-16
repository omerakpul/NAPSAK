package com.napsak.app.domain.model

sealed class UploadResult {
    data class Success(val url: String) : UploadResult()
    object FileTooLarge : UploadResult()
    object Failure : UploadResult()
}
