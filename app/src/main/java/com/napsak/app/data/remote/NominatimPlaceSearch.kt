package com.napsak.app.data.remote

import com.napsak.app.domain.model.Place
import com.napsak.app.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class NominatimItem(
    val lat: String = "",
    val lon: String = "",
    val name: String = "",
    @SerialName("display_name") val displayName: String = ""
)

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Parses a Nominatim `format=jsonv2` search response; entries without usable coordinates are skipped. */
fun parsePlaces(json: String): List<Place> =
    lenientJson.decodeFromString<List<NominatimItem>>(json).mapNotNull { item ->
        val lat = item.lat.toDoubleOrNull() ?: return@mapNotNull null
        val lon = item.lon.toDoubleOrNull() ?: return@mapNotNull null
        val title = item.name.ifBlank { item.displayName.substringBefore(",") }.trim()
        if (title.isEmpty()) return@mapNotNull null
        val address = item.displayName.substringAfter(",", "").trim().trim(',').trim()
        Place(name = title, address = address, latitude = lat, longitude = lon)
    }

/**
 * Free place search backed by OpenStreetMap Nominatim (no API key). Usage policy: identify the app
 * with a User-Agent and keep to ~1 request/second, which a user-typed search does naturally.
 */
@Singleton
class NominatimPlaceSearchRepository @Inject constructor() : PlaceSearchRepository {

    override suspend fun search(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        runCatching {
            val q = query.trim()
            if (q.length < 2) return@runCatching emptyList()
            val url = URL(
                "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6&accept-language=tr&q=" +
                    URLEncoder.encode(q, "UTF-8")
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "NAPSAK-Android/1.0 (https://github.com/omerakpul/NAPSAK)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
                parsePlaces(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }
    }
}
