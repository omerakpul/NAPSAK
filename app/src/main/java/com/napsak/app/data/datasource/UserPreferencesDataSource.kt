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
import com.napsak.app.domain.model.HistoryEntry
import com.napsak.app.domain.model.SavedChoiceList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.napsak.app.R

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_SAVED_LISTS = stringPreferencesKey("saved_lists")
        private val KEY_HISTORY = stringPreferencesKey("history")
        private const val HISTORY_LIMIT = 50
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

    /** Finished decisions, newest first. Stored only on this device. */
    val historyFlow: Flow<List<HistoryEntry>> = context.dataStore.data.map { preferences ->
        preferences[KEY_HISTORY]?.let {
            try { Json.decodeFromString<List<HistoryEntry>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    private fun Preferences.history(): List<HistoryEntry> = this[KEY_HISTORY]?.let {
        try { Json.decodeFromString<List<HistoryEntry>>(it) } catch (e: Exception) { emptyList() }
    } ?: emptyList()

    /** Adds [entry]; a decision for the same room is replaced instead of duplicated. */
    suspend fun addHistory(entry: HistoryEntry) {
        context.dataStore.edit { preferences ->
            val updated = (listOf(entry) + preferences.history().filter { it.roomId != entry.roomId }).take(HISTORY_LIMIT)
            preferences[KEY_HISTORY] = Json.encodeToString(updated)
        }
    }

    suspend fun deleteHistory(id: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HISTORY] = Json.encodeToString(preferences.history().filter { it.id != id })
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences -> preferences.remove(KEY_HISTORY) }
    }

    suspend fun saveUserCredentials(@Suppress("UNUSED_PARAMETER") userId: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USERNAME] = username
        }
    }

    fun getDefaultLists(): List<SavedChoiceList> {
        // list names and options follow the app language; the category stays the stable Turkish key
        return listOf(
            SavedChoiceList(
                id = "preset-yemek",
                name = context.getString(R.string.cat_yemek),
                category = "Yemek",
                choices = listOf(
                    Choice(id = "y1", name = context.getString(R.string.preset_y1_name), details = context.getString(R.string.preset_y1_details), category = "Yemek"),
                    Choice(id = "y2", name = context.getString(R.string.preset_y2_name), details = context.getString(R.string.preset_y2_details), category = "Yemek"),
                    Choice(id = "y3", name = context.getString(R.string.preset_y3_name), details = context.getString(R.string.preset_y3_details), category = "Yemek"),
                    Choice(id = "y4", name = context.getString(R.string.preset_y4_name), details = context.getString(R.string.preset_y4_details), category = "Yemek"),
                    Choice(id = "y5", name = context.getString(R.string.preset_y5_name), details = context.getString(R.string.preset_y5_details), category = "Yemek")
                )
            ),
            SavedChoiceList(
                id = "preset-aktivite",
                name = context.getString(R.string.cat_aktivite),
                category = "Aktivite",
                choices = listOf(
                    Choice(id = "a1", name = context.getString(R.string.preset_a1_name), details = context.getString(R.string.preset_a1_details), category = "Aktivite"),
                    Choice(id = "a2", name = context.getString(R.string.preset_a2_name), details = context.getString(R.string.preset_a2_details), category = "Aktivite"),
                    Choice(id = "a3", name = context.getString(R.string.preset_a3_name), details = context.getString(R.string.preset_a3_details), category = "Aktivite"),
                    Choice(id = "a4", name = context.getString(R.string.preset_a4_name), details = context.getString(R.string.preset_a4_details), category = "Aktivite"),
                    Choice(id = "a5", name = context.getString(R.string.preset_a5_name), details = context.getString(R.string.preset_a5_details), category = "Aktivite")
                )
            ),
            SavedChoiceList(
                id = "preset-film",
                name = context.getString(R.string.cat_film),
                category = "Film",
                choices = listOf(
                    Choice(id = "f1", name = context.getString(R.string.preset_f1_name), details = context.getString(R.string.preset_f1_details), category = "Film"),
                    Choice(id = "f2", name = context.getString(R.string.preset_f2_name), details = context.getString(R.string.preset_f2_details), category = "Film"),
                    Choice(id = "f3", name = context.getString(R.string.preset_f3_name), details = context.getString(R.string.preset_f3_details), category = "Film"),
                    Choice(id = "f4", name = context.getString(R.string.preset_f4_name), details = context.getString(R.string.preset_f4_details), category = "Film"),
                    Choice(id = "f5", name = context.getString(R.string.preset_f5_name), details = context.getString(R.string.preset_f5_details), category = "Film")
                )
            ),
            SavedChoiceList(
                id = "preset-eglence",
                name = context.getString(R.string.cat_eglence),
                category = "Eğlence",
                choices = listOf(
                    Choice(id = "e1", name = context.getString(R.string.preset_e1_name), details = context.getString(R.string.preset_e1_details), category = "Eğlence"),
                    Choice(id = "e2", name = context.getString(R.string.preset_e2_name), details = context.getString(R.string.preset_e2_details), category = "Eğlence"),
                    Choice(id = "e3", name = context.getString(R.string.preset_e3_name), details = context.getString(R.string.preset_e3_details), category = "Eğlence"),
                    Choice(id = "e4", name = context.getString(R.string.preset_e4_name), details = context.getString(R.string.preset_e4_details), category = "Eğlence")
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
