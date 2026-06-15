package com.napsak.app.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.SavedChoiceList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_SAVED_LISTS = stringPreferencesKey("saved_lists")
    }

    val userIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val existingId = preferences[KEY_USER_ID]
        if (existingId != null) {
            existingId
        } else {
            // If no ID exists, we will generate one.
            // Note: Since this flow is read-only, we save it permanently during name entry.
            UUID.randomUUID().toString()
        }
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USERNAME]
    }

    val savedListsFlow: Flow<List<SavedChoiceList>> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_SAVED_LISTS]
        if (jsonStr != null) {
            try {
                Json.decodeFromString<List<SavedChoiceList>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            getDefaultLists()
        }
    }

    suspend fun saveUserCredentials(userId: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USERNAME] = username
        }
    }

    fun getDefaultLists(): List<SavedChoiceList> {
        return listOf(
            SavedChoiceList(
                id = "preset-yemek",
                name = "Yemek",
                category = "Yemek",
                choices = listOf(
                    Choice(id = "y1", name = "Pizzacı", details = "Taş fırında İtalyan pizzası", category = "Yemek"),
                    Choice(id = "y2", name = "Burgerci", details = "Gurme hamburgerler ve çıtır patates", category = "Yemek"),
                    Choice(id = "y3", name = "Kebapçı", details = "Zengin meze ve enfes Adana kebap", category = "Yemek"),
                    Choice(id = "y4", name = "Sushi Bar", details = "Uzak doğu lezzetleri ve taze roll'lar", category = "Yemek"),
                    Choice(id = "y5", name = "Starbucks", details = "Kahve ve leziz tatlı molası", category = "Yemek")
                )
            ),
            SavedChoiceList(
                id = "preset-aktivite",
                name = "Aktivite",
                category = "Aktivite",
                choices = listOf(
                    Choice(id = "a1", name = "Bowling", details = "Grupça bowling turnuvası", category = "Aktivite"),
                    Choice(id = "a2", name = "Sinema", details = "Vizyondaki en yeni aksiyon filmi", category = "Aktivite"),
                    Choice(id = "a3", name = "Kafe Sohbeti", details = "Loş bir kafede koyu muhabbet", category = "Aktivite"),
                    Choice(id = "a4", name = "Konser", details = "Açık hava rock konseri coşkusu", category = "Aktivite"),
                    Choice(id = "a5", name = "Tiyatro", details = "Sezonun popüler komedi oyunu", category = "Aktivite")
                )
            ),
            SavedChoiceList(
                id = "preset-film",
                name = "Film",
                category = "Film",
                choices = listOf(
                    Choice(id = "f1", name = "Bilim Kurgu", details = "Yıldızlararası yolculuk ve uzay temalı", category = "Film"),
                    Choice(id = "f2", name = "Komedi", details = "Gülme garantili yerli komedi", category = "Film"),
                    Choice(id = "f3", name = "Korku", details = "Gerilim dolu karanlık bir ev hikayesi", category = "Film"),
                    Choice(id = "f4", name = "Aksiyon / Macera", details = "Nefes kesen kovalamaca ve dövüş sahneleri", category = "Film"),
                    Choice(id = "f5", name = "Romantik", details = "Duygusal ve sıcak bir aşk öyküsü", category = "Film")
                )
            ),
            SavedChoiceList(
                id = "preset-eglence",
                name = "Eğlence",
                category = "Eğlence",
                choices = listOf(
                    Choice(id = "e1", name = "PlayStation Kafe", details = "FC 24 ve dövüş oyunları kapışması", category = "Eğlence"),
                    Choice(id = "e2", name = "Karaoke", details = "Detone olmayı göze alanlar kulübü", category = "Eğlence"),
                    Choice(id = "e3", name = "Kutu Oyunları (Boardgames)", details = "Catan, Tabu veya Monopoly gecesi", category = "Eğlence"),
                    Choice(id = "e4", name = "Bilardo / Dart", details = "Hassas atışlar ve rekabet", category = "Eğlence")
                )
            )
        )
    }

    suspend fun saveChoiceList(name: String, category: String, choices: List<Choice>, imageUrl: String? = null) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_SAVED_LISTS]
            val currentLists = if (jsonStr != null) {
                try {
                    Json.decodeFromString<List<SavedChoiceList>>(jsonStr).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                getDefaultLists().toMutableList()
            }

            val newList = SavedChoiceList(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                choices = choices,
                imageUrl = imageUrl
            )
            currentLists.add(newList)

            preferences[KEY_SAVED_LISTS] = Json.encodeToString(currentLists.toList())
        }
    }

    suspend fun updateChoiceList(listId: String, name: String, category: String, choices: List<Choice>, imageUrl: String? = null) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_SAVED_LISTS]
            val currentLists = if (jsonStr != null) {
                try {
                    Json.decodeFromString<List<SavedChoiceList>>(jsonStr).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                getDefaultLists().toMutableList()
            }

            val index = currentLists.indexOfFirst { it.id == listId }
            if (index != -1) {
                currentLists[index] = SavedChoiceList(
                    id = listId,
                    name = name,
                    category = category,
                    choices = choices,
                    imageUrl = imageUrl
                )
                preferences[KEY_SAVED_LISTS] = Json.encodeToString(currentLists.toList())
            }
        }
    }

    suspend fun deleteChoiceList(listId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_SAVED_LISTS]
            val currentLists = if (jsonStr != null) {
                try {
                    Json.decodeFromString<List<SavedChoiceList>>(jsonStr).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                getDefaultLists().toMutableList()
            }

            val updatedLists = currentLists.filter { it.id != listId }
            preferences[KEY_SAVED_LISTS] = Json.encodeToString(updatedLists)
        }
    }
}
