package com.napsak.app.ui.screens.createchoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.domain.model.Choice
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
    val isEditing: Boolean = false,
    val editingChoiceId: String? = null,
    val savedLists: List<SavedChoiceList> = emptyList()
)

@HiltViewModel
class CreateChoicesViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val startVotingUseCase: StartVotingUseCase
) : ViewModel() {

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

    fun addChoice() {
        val state = _uiState.value
        if (state.currentName.isBlank()) return

        val newChoice = Choice(
            id = UUID.randomUUID().toString(),
            name = state.currentName.trim(),
            details = state.currentDetails.trim()
        )

        _uiState.update {
            it.copy(
                choices = it.choices + newChoice,
                currentName = "",
                currentDetails = "",
            )
        }
    }

    fun removeChoice(choiceId: String) {
        _uiState.update {
            it.copy(choices = it.choices.filter { c -> c.id != choiceId })
        }
    }

    fun startEditing(choice: Choice) {
        _uiState.update {
            it.copy(
                isEditing = true,
                editingChoiceId = choice.id,
                currentName = choice.name,
                currentDetails = choice.details
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
                            details = state.currentDetails.trim()
                        )
                    } else c
                },
                isEditing = false,
                editingChoiceId = null,
                currentName = "",
                currentDetails = ""
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editingChoiceId = null,
                currentName = "",
                currentDetails = ""
            )
        }
    }

    fun saveChoiceList(name: String) {
        viewModelScope.launch {
            val currentChoices = _uiState.value.choices
            if (currentChoices.isNotEmpty() && name.isNotBlank()) {
                userPreferencesDataSource.saveChoiceList(name.trim(), currentChoices)
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

    fun startVoting(roomId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentChoices = _uiState.value.choices
            if (currentChoices.isNotEmpty()) {
                val result = startVotingUseCase(roomId, currentChoices)
                if (result.isSuccess) {
                    onComplete()
                }
            }
        }
    }

    fun getChoices(): List<Choice> = _uiState.value.choices
}
