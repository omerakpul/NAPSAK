package com.napsak.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.napsak.app.ui.screens.createchoices.CreateChoicesScreen
import com.napsak.app.ui.screens.home.HomeScreen
import com.napsak.app.ui.screens.lists.ListsScreen
import com.napsak.app.ui.screens.lists.EditListScreen
import com.napsak.app.ui.screens.lobby.LobbyScreen
import com.napsak.app.ui.screens.shared.SharedSessionViewModel
import com.napsak.app.ui.screens.voting.VotingScreen
import com.napsak.app.ui.screens.result.ResultScreen

@Composable
fun NapsakNavGraph(
    navController: NavHostController,
    sharedViewModel: SharedSessionViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onNavigateToLobby = { roomId ->
                    navController.navigate(Screen.Lobby(roomId))
                }
            )
        }
        composable<Screen.Lists> {
            ListsScreen(
                onNavigateToEditList = { listId ->
                    navController.navigate(Screen.EditList(listId))
                }
            )
        }
        composable<Screen.EditList> { backStackEntry ->
            val editList = backStackEntry.toRoute<Screen.EditList>()
            EditListScreen(
                listId = editList.listId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Screen.Lobby> { backStackEntry ->
            val lobby = backStackEntry.toRoute<Screen.Lobby>()
            LobbyScreen(
                roomId = lobby.roomId,
                sharedViewModel = sharedViewModel,
                onNavigateToCreateChoices = { roomId ->
                    navController.navigate(Screen.CreateChoices(roomId)) {
                        popUpTo(Screen.Home) { saveState = true }
                    }
                },
                onNavigateToVoting = { roomId ->
                    navController.navigate(Screen.Voting(roomId)) {
                        popUpTo(Screen.Home) { saveState = true }
                    }
                }
            )
        }
        composable<Screen.CreateChoices> { backStackEntry ->
            val createChoices = backStackEntry.toRoute<Screen.CreateChoices>()
            CreateChoicesScreen(
                roomId = createChoices.roomId,
                onNavigateToVoting = { roomId, choices ->
                    sharedViewModel.setChoices(choices)
                    navController.navigate(Screen.Voting(roomId)) {
                        popUpTo(Screen.Home) { saveState = true }
                    }
                }
            )
        }
        composable<Screen.Voting> { backStackEntry ->
            val voting = backStackEntry.toRoute<Screen.Voting>()
            VotingScreen(
                roomId = voting.roomId,
                sharedViewModel = sharedViewModel,
                onNavigateToResult = { roomId ->
                    navController.navigate(Screen.Result(roomId)) {
                        popUpTo(Screen.Home) { saveState = true }
                    }
                }
            )
        }
        composable<Screen.Result> { backStackEntry ->
            val result = backStackEntry.toRoute<Screen.Result>()
            ResultScreen(
                roomId = result.roomId,
                sharedViewModel = sharedViewModel,
                onNavigateToHome = {
                    sharedViewModel.clear()
                    navController.navigate(Screen.Home) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
    }
}
