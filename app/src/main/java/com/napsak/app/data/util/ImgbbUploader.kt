package com.napsak.app.data.util

import android.content.Context
import android.net.Uri
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

    suspend fun uploadImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val file = uriToFile(context, uri) ?: return@withContext null
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
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                val jsonObject = json.parseToJsonElement(bodyString).jsonObject
                val data = jsonObject["data"]?.jsonObject ?: return@withContext null
                val displayUrl = data["url"]?.jsonPrimitive?.content
                file.delete() // clean up temp file
                return@withContext displayUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
