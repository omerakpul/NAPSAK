package com.napsak.app

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import com.napsak.app.data.remote.ConnectivityObserver
import com.napsak.app.ui.OfflineBanner
import javax.inject.Inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.napsak.app.ui.navigation.NapsakNavGraph
import com.napsak.app.ui.navigation.Screen
import com.napsak.app.ui.screens.shared.SharedSessionViewModel
import com.napsak.app.ui.theme.NAPSAKTheme

import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.res.stringResource
import com.napsak.app.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var connectivity: ConnectivityObserver

    /** Room code from a tapped invite link, consumed by the home screen. */
    private val pendingRoomCode = mutableStateOf<String?>(null)

    private fun roomCodeFrom(intent: Intent?): String? =
        intent?.data?.getQueryParameter("room")?.takeIf { code -> code.length == 6 && code.all { it.isDigit() } }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        roomCodeFrom(intent)?.let { pendingRoomCode.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoomCode.value = roomCodeFrom(intent)
        setContent {
            NAPSAKTheme {
                val online by connectivity.isOnline.collectAsState()
                val navController = rememberNavController()
                val sharedViewModel: SharedSessionViewModel = hiltViewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val tabs = listOf(
                    Triple(stringResource(R.string.tab_home), Icons.Default.Home, Screen.Home as Screen),
                    Triple(stringResource(R.string.tab_lists), Icons.Default.List, Screen.Lists as Screen),
                    Triple(stringResource(R.string.tab_history), Icons.Default.History, Screen.History as Screen)
                )
                fun isSelected(screen: Screen): Boolean = when (screen) {
                    Screen.Home -> currentDestination?.hasRoute<Screen.Home>() == true
                    Screen.Lists -> currentDestination?.hasRoute<Screen.Lists>() == true
                    Screen.History -> currentDestination?.hasRoute<Screen.History>() == true
                    else -> false
                }
                val openTab: (Screen) -> Unit = { screen ->
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                val showTabs = tabs.any { isSelected(it.third) }
                // short windows (phones in landscape, split screen) get a side rail instead of a bottom bar
                val useRail = LocalConfiguration.current.screenHeightDp < 500
                val accent = MaterialTheme.colorScheme.primary
                val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showTabs && !useRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                tabs.forEach { (label, icon, screen) ->
                                    NavigationBarItem(
                                        selected = isSelected(screen),
                                        onClick = { openTab(screen) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = accent,
                                            selectedTextColor = accent,
                                            indicatorColor = accent.copy(alpha = 0.15f),
                                            unselectedIconColor = muted,
                                            unselectedTextColor = muted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                            .imePadding() // content stays above the keyboard
                    ) {
                        if (showTabs && useRail) {
                            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                tabs.forEach { (label, icon, screen) ->
                                    NavigationRailItem(
                                        selected = isSelected(screen),
                                        onClick = { openTab(screen) },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = accent,
                                            selectedTextColor = accent,
                                            indicatorColor = accent.copy(alpha = 0.15f),
                                            unselectedIconColor = muted,
                                            unselectedTextColor = muted
                                        )
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // pushes the screen down instead of covering its title
                            AnimatedVisibility(visible = !online) { OfflineBanner() }
                            // phones use the full width; tablets, foldables and landscape get a centred column
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                NapsakNavGraph(
                                    navController = navController,
                                    sharedViewModel = sharedViewModel,
                                    initialRoomCode = pendingRoomCode.value,
                                    modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
