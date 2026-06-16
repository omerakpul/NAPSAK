package com.napsak.app.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream

import com.napsak.app.BuildConfig

object ImgbbUploader {
    private val API_KEY = BuildConfig.IMGBB_API_KEY
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

    sealed class UploadResult {
        data class Success(val url: String) : UploadResult()
        object FileTooLarge : UploadResult()
        object Failure : UploadResult()
    }

    private fun getUriFileSize(context: Context, uri: Uri): Long {
        var size: Long = -1
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (size == -1L) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                    size = fd.length
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return size
    }

    suspend fun uploadImage(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        try {
            val fileSize = getUriFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext UploadResult.FileTooLarge
            }

            val file = compressUriToFile(context, uri) ?: return@withContext UploadResult.Failure
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/*".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=$API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext UploadResult.Failure
                val bodyString = response.body?.string() ?: return@withContext UploadResult.Failure
                val jsonObject = json.parseToJsonElement(bodyString).jsonObject
                val data = jsonObject["data"]?.jsonObject ?: return@withContext UploadResult.Failure
                val displayUrl = data["url"]?.jsonPrimitive?.content ?: return@withContext UploadResult.Failure
                file.delete() // clean up temp file
                return@withContext UploadResult.Success(displayUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UploadResult.Failure
        }
    }

    private fun compressUriToFile(context: Context, uri: Uri): File? {
        return try {
            val resolver = context.contentResolver
            
            // 1. Get dimensions first without loading full bitmap to RAM (prevent OOM)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            // Downscale if dimension is larger than 1024px
            val reqWidth = 1024
            val reqHeight = 1024
            var inSampleSize = 1
            if (width > reqWidth || height > reqHeight) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                while ((halfWidth / inSampleSize) >= reqWidth && (halfHeight / inSampleSize) >= reqHeight) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
            }
            val bitmap = resolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Compress and write to temp file
            val tempFile = File.createTempFile("upload_compressed_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            }
            bitmap.recycle() // release native memory
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
