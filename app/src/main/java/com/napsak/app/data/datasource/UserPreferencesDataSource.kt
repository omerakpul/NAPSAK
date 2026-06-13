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
            emptyList()
        }
    }

    suspend fun saveUserCredentials(userId: String, username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USERNAME] = username
        }
    }

    suspend fun saveChoiceList(name: String, choices: List<Choice>) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_SAVED_LISTS]
            val currentLists = if (jsonStr != null) {
                try {
                    Json.decodeFromString<List<SavedChoiceList>>(jsonStr).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            val newList = SavedChoiceList(
                id = UUID.randomUUID().toString(),
                name = name,
                choices = choices
            )
            currentLists.add(newList)

            preferences[KEY_SAVED_LISTS] = Json.encodeToString(currentLists.toList())
        }
    }

    suspend fun deleteChoiceList(listId: String) {
        context.dataStore.edit { preferences ->
            val jsonStr = preferences[KEY_SAVED_LISTS]
            if (jsonStr != null) {
                try {
                    val currentLists = Json.decodeFromString<List<SavedChoiceList>>(jsonStr)
                    val updatedLists = currentLists.filter { it.id != listId }
                    preferences[KEY_SAVED_LISTS] = Json.encodeToString(updatedLists)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
}
