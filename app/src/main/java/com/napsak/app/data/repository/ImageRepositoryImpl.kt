package com.napsak.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.napsak.app.data.remote.SupabaseSession
import com.napsak.app.domain.model.UploadResult
import com.napsak.app.domain.repository.ImageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SupabaseSession
) : ImageRepository {

    private val bucket = "choice-images"
    private val maxFileSizeBytes = 10 * 1024 * 1024 // 10 MB (source file); compressed output is far smaller

    override suspend fun uploadImage(context: Context, uri: Uri): UploadResult = withContext(Dispatchers.IO) {
        var file: File? = null
        try {
            val fileSize = getUriFileSize(context, uri)
            if (fileSize > maxFileSizeBytes) {
                return@withContext UploadResult.FileTooLarge
            }

            file = compressUriToFile(context, uri) ?: return@withContext UploadResult.Failure
            val userId = session.userId()
            val path = "$userId/${UUID.randomUUID()}.jpg"

            val images = supabase.storage.from(bucket)
            images.upload(path, file.readBytes()) {
                contentType = ContentType.Image.JPEG
                upsert = false
            }
            UploadResult.Success(images.publicUrl(path))
        } catch (e: Exception) {
            e.printStackTrace()
            UploadResult.Failure
        } finally {
            file?.delete()
        }
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

    private fun compressUriToFile(context: Context, uri: Uri): File? {
        return try {
            val resolver = context.contentResolver
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

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

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
            }
            val bitmap = resolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val tempFile = File.createTempFile("upload_compressed_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            }
            bitmap.recycle()
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
