package com.napsak.app.ui.screens.createchoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.Place
import com.napsak.app.domain.model.Suggestion
import com.napsak.app.domain.model.VoteMode
import com.napsak.app.domain.repository.PlaceSearchRepository
import com.napsak.app.domain.repository.RoomRepository
import com.napsak.app.domain.util.parseQuickChoices
import kotlinx.coroutines.Job
import com.napsak.app.domain.model.SavedChoiceList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.napsak.app.domain.usecase.StartVotingUseCase
import javax.inject.Inject

data class CreateChoicesUiState(
    val choices: List<Choice> = emptyList(),
    val currentName: String = "",
    val currentDetails: String = "",
    val currentCategory: String = "",
    val currentImageUrl: String = "",
    val currentLatitude: String = "",
    val currentLongitude: String = "",
    val isEditing: Boolean = false,
    val editingChoiceId: String? = null,
    val savedLists: List<SavedChoiceList> = emptyList(),
    val voteMode: VoteMode = VoteMode.LIKE,
    val quickEntryText: String = "",
    val placeQuery: String = "",
    val placeResults: List<Place> = emptyList(),
    val isSearchingPlaces: Boolean = false,
    val placeSearchFailed: Boolean = false,
    val suggestions: List<Suggestion> = emptyList()
)

@HiltViewModel
class CreateChoicesViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val startVotingUseCase: StartVotingUseCase,
    private val uploadImageUseCase: com.napsak.app.domain.usecase.UploadImageUseCase,
    private val roomRepository: RoomRepository,
    private val placeSearch: PlaceSearchRepository
) : ViewModel() {

    private var suggestionsJob: Job? = null

    /** Follows the room so the host sees friends' suggestions arrive live. */
    fun observeSuggestions(roomId: String) {
        if (suggestionsJob?.isActive == true) return
        suggestionsJob = viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                _uiState.update { it.copy(suggestions = room?.suggestions.orEmpty()) }
            }
        }
    }

    fun acceptSuggestion(roomId: String, suggestion: Suggestion) {
        val alreadyThere = _uiState.value.choices.any { it.name.equals(suggestion.name, ignoreCase = true) }
        if (!alreadyThere) {
            _uiState.update {
                it.copy(
                    choices = it.choices + Choice(
                        id = UUID.randomUUID().toString(),
                        name = suggestion.name,
                        details = suggestion.details
                    )
                )
            }
        }
        dismissSuggestion(roomId, suggestion)
    }

    fun dismissSuggestion(roomId: String, suggestion: Suggestion) {
        viewModelScope.launch { roomRepository.dismissSuggestion(roomId, suggestion.id) }
    }

    fun onVoteModeChange(mode: VoteMode) {
        _uiState.update { it.copy(voteMode = mode) }
    }

    fun onQuickEntryChange(text: String) {
        _uiState.update { it.copy(quickEntryText = text) }
    }

    /** Adds one option per pasted line (skipping names already in the list). Returns how many were added. */
    fun addQuickEntries(): Int {
        val state = _uiState.value
        val existing = state.choices.map { it.name.lowercase() }.toSet()
        val room = (50 - state.choices.size).coerceAtLeast(0)
        val fresh = parseQuickChoices(state.quickEntryText)
            .filter { it.name.lowercase() !in existing }
            .take(room)
        if (fresh.isNotEmpty()) {
            _uiState.update { it.copy(choices = it.choices + fresh, quickEntryText = "") }
        }
        return fresh.size
    }

    fun onPlaceQueryChange(query: String) {
        _uiState.update { it.copy(placeQuery = query, placeSearchFailed = false) }
    }

    fun searchPlaces() {
        val query = _uiState.value.placeQuery.trim()
        if (query.length < 2) return
        _uiState.update { it.copy(isSearchingPlaces = true, placeSearchFailed = false) }
        viewModelScope.launch {
            val result = placeSearch.search(query)
            _uiState.update {
                it.copy(
                    isSearchingPlaces = false,
                    placeResults = result.getOrDefault(emptyList()),
                    placeSearchFailed = result.isFailure
                )
            }
        }
    }

    /** Fills the option form from a map result so it can still be edited before adding. */
    fun selectPlace(place: Place) {
        _uiState.update {
            it.copy(
                currentName = place.name.take(100),
                currentDetails = place.address.take(500),
                currentLatitude = place.latitude.toString(),
                currentLongitude = place.longitude.toString(),
                placeQuery = "",
                placeResults = emptyList()
            )
        }
    }

    fun uploadImage(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (com.napsak.app.domain.model.UploadResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = uploadImageUseCase(context, uri)
            onResult(result)
        }
    }

    private val _uiState = MutableStateFlow(CreateChoicesUiState())
    val uiState: StateFlow<CreateChoicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesDataSource.savedListsFlow.collect { lists ->
                _uiState.update { it.copy(savedLists = lists) }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(currentName = name) }
    }

    fun onDetailsChange(details: String) {
        _uiState.update { it.copy(currentDetails = details) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(currentCategory = category) }
    }

    fun onImageUrlChange(imageUrl: String) {
        _uiState.update { it.copy(currentImageUrl = imageUrl) }
    }

    fun onLatitudeChange(latitude: String) {
        _uiState.update { it.copy(currentLatitude = latitude) }
    }

    fun onLongitudeChange(longitude: String) {
        _uiState.update { it.copy(currentLongitude = longitude) }
    }

    fun addChoice() {
        val state = _uiState.value
        if (state.currentName.isBlank()) return

        val newChoice = Choice(
            id = UUID.randomUUID().toString(),
            name = state.currentName.trim(),
            details = state.currentDetails.trim(),
            category = state.currentCategory.trim(),
            imageUrl = state.currentImageUrl.trim().takeIf { it.isNotBlank() },
            latitude = state.currentLatitude.trim().toDoubleOrNull(),
            longitude = state.currentLongitude.trim().toDoubleOrNull()
        )

        _uiState.update {
            it.copy(
                choices = it.choices + newChoice,
                currentName = "",
                currentDetails = "",
                currentCategory = "",
                currentImageUrl = "",
                currentLatitude = "",
                currentLongitude = ""
            )
        }
    }

    fun removeChoice(choiceId: String) {
        _uiState.update {
            it.copy(choices = it.choices.filter { c -> c.id != choiceId })
        }
    }

    fun updateChoiceImageUrl(choiceId: String, imageUrl: String) {
        _uiState.update { state ->
            state.copy(
                choices = state.choices.map { c ->
                    if (c.id == choiceId) c.copy(imageUrl = imageUrl.takeIf { it.isNotBlank() }) else c
                }
            )
        }
    }

    fun clearAllChoices() {
        _uiState.update {
            it.copy(choices = emptyList())
        }
    }

    fun startEditing(choice: Choice) {
        _uiState.update {
            it.copy(
                isEditing = true,
                editingChoiceId = choice.id,
                currentName = choice.name,
                currentDetails = choice.details,
                currentCategory = choice.category,
                currentImageUrl = choice.imageUrl ?: "",
                currentLatitude = choice.latitude?.toString() ?: "",
                currentLongitude = choice.longitude?.toString() ?: ""
            )
        }
    }

    fun saveEdit() {
        val state = _uiState.value
        if (state.currentName.isBlank() || state.editingChoiceId == null) return

        _uiState.update {
            it.copy(
                choices = it.choices.map { c ->
                    if (c.id == state.editingChoiceId) {
                        c.copy(
                            name = state.currentName.trim(),
                            details = state.currentDetails.trim(),
                            category = state.currentCategory.trim(),
                            imageUrl = state.currentImageUrl.trim().takeIf { it.isNotBlank() },
                            latitude = state.currentLatitude.trim().toDoubleOrNull(),
                            longitude = state.currentLongitude.trim().toDoubleOrNull()
                        )
                    } else c
                },
                isEditing = false,
                editingChoiceId = null,
                currentName = "",
                currentDetails = "",
                currentCategory = "",
                currentImageUrl = "",
                currentLatitude = "",
                currentLongitude = ""
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editingChoiceId = null,
                currentName = "",
                currentDetails = "",
                currentCategory = "",
                currentImageUrl = "",
                currentLatitude = "",
                currentLongitude = ""
            )
        }
    }

    fun loadPresetTemplate(templateName: String) {
        val preset = userPreferencesDataSource.getDefaultLists().find { it.category == templateName }
        val presetChoices = preset?.choices?.map {
            it.copy(id = UUID.randomUUID().toString())
        } ?: emptyList()

        if (presetChoices.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    choices = it.choices + presetChoices,
                    currentName = "",
                    currentDetails = "",
                    currentCategory = "",
                    isEditing = false,
                    editingChoiceId = null
                )
            }
        }
    }

    fun saveChoiceList(name: String, category: String = "", imageUrl: String? = null) {
        viewModelScope.launch {
            val currentChoices = _uiState.value.choices
            if (currentChoices.isNotEmpty() && name.isNotBlank()) {
                userPreferencesDataSource.saveChoiceList(name.trim(), category.trim(), currentChoices, imageUrl)
            }
        }
    }

    fun deleteChoiceList(listId: String) {
        viewModelScope.launch {
            userPreferencesDataSource.deleteChoiceList(listId)
        }
    }

    fun loadChoiceList(savedList: SavedChoiceList) {
        _uiState.update {
            it.copy(
                choices = savedList.choices,
                currentName = "",
                currentDetails = "",
                isEditing = false,
                editingChoiceId = null
            )
        }
    }

    fun startVoting(roomId: String, onFailure: () -> Unit = {}, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentChoices = _uiState.value.choices
            if (currentChoices.isNotEmpty()) {
                val result = startVotingUseCase(roomId, currentChoices, _uiState.value.voteMode)
                if (result.isSuccess) onComplete() else onFailure()
            }
        }
    }

    fun getChoices(): List<Choice> = _uiState.value.choices

    fun getDefaultLists(): List<SavedChoiceList> = userPreferencesDataSource.getDefaultLists()
}
