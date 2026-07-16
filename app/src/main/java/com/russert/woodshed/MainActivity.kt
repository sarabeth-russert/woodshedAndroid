package com.russert.woodshed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.russert.woodshed.data.preferences.UserPreferencesRepository
import com.russert.woodshed.ui.library.LibraryScreen
import com.russert.woodshed.ui.metadata.EditRecordingScreen
import com.russert.woodshed.ui.metadata.MetadataFormScreen
import com.russert.woodshed.ui.navigation.Screen
import com.russert.woodshed.ui.onboarding.OnboardingScreen
import com.russert.woodshed.ui.player.PlayerScreen
import com.russert.woodshed.ui.record.RecordScreen
import com.russert.woodshed.ui.settings.SettingsScreen
import com.russert.woodshed.ui.theme.Amber
import com.russert.woodshed.ui.theme.Cream
import com.russert.woodshed.ui.theme.DarkBrown
import com.russert.woodshed.ui.theme.WarmBrown
import com.russert.woodshed.ui.theme.WoodshedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferencesRepository

    private val rootTabs = listOf(Screen.Library, Screen.Record, Screen.Settings)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WoodshedTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val seen = prefs.hasSeenOnboarding.first()
                    startDestination = if (seen) Screen.Library.route else Screen.Onboarding.route
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (startDestination == null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(DarkBrown))
                } else {
                    val showBottomBar = currentRoute in rootTabs.map { it.route }

                Scaffold(
                    containerColor = DarkBrown,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(containerColor = DarkBrown) {
                                rootTabs.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (!selected) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Library.route) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (screen) {
                                                    Screen.Library  -> Icons.Default.VideoLibrary
                                                    Screen.Record   -> Icons.Default.FiberManualRecord
                                                    Screen.Settings -> Icons.Default.Settings
                                                    else            -> Icons.Default.VideoLibrary
                                                },
                                                contentDescription = screen.route,
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = when (screen) {
                                                    Screen.Library  -> "Library"
                                                    Screen.Record   -> "Record"
                                                    Screen.Settings -> "Settings"
                                                    else            -> ""
                                                }
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Amber,
                                            selectedTextColor = Amber,
                                            unselectedIconColor = Cream.copy(alpha = 0.5f),
                                            unselectedTextColor = Cream.copy(alpha = 0.5f),
                                            indicatorColor = WarmBrown.copy(alpha = 0.3f),
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(onComplete = {
                                scope.launch { prefs.setHasSeenOnboarding(true) }
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            })
                        }
                        composable(Screen.Library.route) {
                            LibraryScreen(navController = navController)
                        }
                        composable(Screen.Record.route) {
                            RecordScreen(navController = navController)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(Screen.Player.route) {
                            PlayerScreen(navController = navController)
                        }
                        composable(
                            route = Screen.MetadataForm.route,
                            arguments = listOf(
                                navArgument("videoPath") { type = NavType.StringType },
                                navArgument("duration")  { type = NavType.StringType; defaultValue = "0.0" },
                            ),
                        ) {
                            MetadataFormScreen(navController = navController)
                        }
                        composable(Screen.EditRecording.route) {
                            EditRecordingScreen(navController = navController)
                        }
                    }
                } // Scaffold
                } // else
            }
        }
    }
}
