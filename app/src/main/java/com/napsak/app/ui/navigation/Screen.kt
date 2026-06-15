package com.napsak.app.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Lists : Screen

    @Serializable
    data class EditList(val listId: String = "") : Screen

    @Serializable
    data class Lobby(val roomId: String) : Screen

    @Serializable
    data class CreateChoices(val roomId: String) : Screen

    @Serializable
    data class Voting(val roomId: String) : Screen

    @Serializable
    data class Result(val roomId: String) : Screen
}
