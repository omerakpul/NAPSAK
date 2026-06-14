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
    val currentCategory: String = "",
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

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(currentCategory = category) }
    }

    fun addChoice() {
        val state = _uiState.value
        if (state.currentName.isBlank()) return

        val newChoice = Choice(
            id = UUID.randomUUID().toString(),
            name = state.currentName.trim(),
            details = state.currentDetails.trim(),
            category = state.currentCategory.trim()
        )

        _uiState.update {
            it.copy(
                choices = it.choices + newChoice,
                currentName = "",
                currentDetails = "",
                currentCategory = ""
            )
        }
    }

    fun removeChoice(choiceId: String) {
        _uiState.update {
            it.copy(choices = it.choices.filter { c -> c.id != choiceId })
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
                currentCategory = choice.category
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
                            category = state.currentCategory.trim()
                        )
                    } else c
                },
                isEditing = false,
                editingChoiceId = null,
                currentName = "",
                currentDetails = "",
                currentCategory = ""
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
                currentCategory = ""
            )
        }
    }

    fun loadPresetTemplate(templateName: String) {
        val presetChoices = when (templateName) {
            "Yemek" -> listOf(
                Choice(id = UUID.randomUUID().toString(), name = "Pizzacı", details = "Taş fırında İtalyan pizzası", category = "Yemek"),
                Choice(id = UUID.randomUUID().toString(), name = "Burgerci", details = "Gurme hamburgerler ve çıtır patates", category = "Yemek"),
                Choice(id = UUID.randomUUID().toString(), name = "Kebapçı", details = "Zengin meze ve enfes Adana kebap", category = "Yemek"),
                Choice(id = UUID.randomUUID().toString(), name = "Sushi Bar", details = "Uzak doğu lezzetleri ve taze roll'lar", category = "Yemek"),
                Choice(id = UUID.randomUUID().toString(), name = "Starbucks", details = "Kahve ve leziz tatlı molası", category = "Yemek")
            )
            "Aktivite" -> listOf(
                Choice(id = UUID.randomUUID().toString(), name = "Bowling", details = "Grupça bowling turnuvası", category = "Aktivite"),
                Choice(id = UUID.randomUUID().toString(), name = "Sinema", details = "Vizyondaki en yeni aksiyon filmi", category = "Aktivite"),
                Choice(id = UUID.randomUUID().toString(), name = "Kafe Sohbeti", details = "Loş bir kafede koyu muhabbet", category = "Aktivite"),
                Choice(id = UUID.randomUUID().toString(), name = "Konser", details = "Açık hava rock konseri coşkusu", category = "Aktivite"),
                Choice(id = UUID.randomUUID().toString(), name = "Tiyatro", details = "Sezonun popüler komedi oyunu", category = "Aktivite")
            )
            "Film" -> listOf(
                Choice(id = UUID.randomUUID().toString(), name = "Bilim Kurgu", details = "Yıldızlararası yolculuk ve uzay temalı", category = "Film"),
                Choice(id = UUID.randomUUID().toString(), name = "Komedi", details = "Gülme garantili yerli komedi", category = "Film"),
                Choice(id = UUID.randomUUID().toString(), name = "Korku", details = "Gerilim dolu karanlık bir ev hikayesi", category = "Film"),
                Choice(id = UUID.randomUUID().toString(), name = "Aksiyon / Macera", details = "Nefes kesen kovalamaca ve dövüş sahneleri", category = "Film"),
                Choice(id = UUID.randomUUID().toString(), name = "Romantik", details = "Duygusal ve sıcak bir aşk öyküsü", category = "Film")
            )
            "Eğlence" -> listOf(
                Choice(id = UUID.randomUUID().toString(), name = "PlayStation Kafe", details = "FC 24 ve dövüş oyunları kapışması", category = "Eğlence"),
                Choice(id = UUID.randomUUID().toString(), name = "Karaoke", details = "Detone olmayı göze alanlar kulübü", category = "Eğlence"),
                Choice(id = UUID.randomUUID().toString(), name = "Kutu Oyunları (Boardgames)", details = "Catan, Tabu veya Monopoly gecesi", category = "Eğlence"),
                Choice(id = UUID.randomUUID().toString(), name = "Bilardo / Dart", details = "Hassas atışlar ve rekabet", category = "Eğlence")
            )
            else -> emptyList()
        }

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
