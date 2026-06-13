package com.napsak.app.ui.screens.shared

import androidx.lifecycle.ViewModel
import com.napsak.app.domain.model.Choice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Shared ViewModel scoped to the Activity, used to pass data between
 * CreateChoices → Voting → Result screens.
 */
@HiltViewModel
class SharedSessionViewModel @Inject constructor() : ViewModel() {

    private val _choices = MutableStateFlow<List<Choice>>(emptyList())
    val choices: StateFlow<List<Choice>> = _choices.asStateFlow()

    private val _winnerChoice = MutableStateFlow<Choice?>(null)
    val winnerChoice: StateFlow<Choice?> = _winnerChoice.asStateFlow()

    fun setChoices(choices: List<Choice>) {
        _choices.value = choices
    }

    fun setWinner(choice: Choice) {
        _winnerChoice.value = choice
    }

    fun clear() {
        _choices.value = emptyList()
        _winnerChoice.value = null
    }
}
