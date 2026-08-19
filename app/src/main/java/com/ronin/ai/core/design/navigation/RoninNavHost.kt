package com.ronin.ai.core.design.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ronin.ai.core.design.theme.RoninCyan
import com.ronin.ai.core.design.theme.RoninSurface
import com.ronin.ai.core.design.theme.RoninTextSecondary
import com.ronin.ai.feature.automation.AutomationScreen
import com.ronin.ai.feature.chat.ChatScreen
import com.ronin.ai.feature.dashboard.DashboardScreen
import com.ronin.ai.feature.device.DeviceScreen
import com.ronin.ai.feature.memory.MemoryScreen
import com.ronin.ai.feature.providers.AiProviderEditScreen
import com.ronin.ai.feature.providers.AiProvidersScreen
import com.ronin.ai.feature.providers.VoiceSettingsScreen
import com.ronin.ai.feature.settings.SettingsScreen
import com.ronin.ai.feature.skills.SkillsScreen
import com.ronin.ai.feature.voice.VoiceScreen

@Composable
fun RoninNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomDestinations = RoninDestination.bottomBarDestinations().map { it.route }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (currentRoute in bottomDestinations) {
                RoninBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = RoninDestination.DASHBOARD.route
            ) {
                composable(RoninDestination.DASHBOARD.route) {
                    DashboardScreen(onNavigate = { route -> navController.navigate(route) })
                }
                composable(RoninDestination.CHAT.route) { ChatScreen() }
                composable(RoninDestination.VOICE.route) { VoiceScreen() }
                composable(RoninDestination.MEMORY.route) { MemoryScreen() }
                composable(RoninDestination.DEVICE.route) { DeviceScreen() }
                composable(RoninDestination.AUTOMATION.route) { AutomationScreen() }
                composable(RoninDestination.SETTINGS.route) {
                    SettingsScreen(
                        onOpenAiProviders = { navController.navigate(RoninDestination.AI_PROVIDERS.route) },
                        onOpenVoiceSettings = { navController.navigate(RoninDestination.VOICE_SETTINGS.route) },
                        onOpenSkills = { navController.navigate(RoninDestination.SKILLS.route) }
                    )
                }
                composable(RoninDestination.SKILLS.route) {
                    SkillsScreen(onBack = { navController.popBackStack() })
                }
                composable(RoninDestination.AI_PROVIDERS.route) {
                    AiProvidersScreen(
                        onBack = { navController.popBackStack() },
                        onEditProvider = { type ->
                            navController.navigate(aiProviderEditRoute(type.name))
                        }
                    )
                }
                composable(
                    route = RoninDestination.AI_PROVIDER_EDIT.route,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) {
                    AiProviderEditScreen(onBack = { navController.popBackStack() })
                }
                composable(RoninDestination.VOICE_SETTINGS.route) {
                    VoiceSettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun RoninBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = RoninSurface.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        RoninDestination.bottomBarDestinations().forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) RoninCyan else RoninTextSecondary
                    )
                },
                label = {
                    Text(
                        destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) RoninCyan else RoninTextSecondary,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RoninCyan,
                    selectedTextColor = RoninCyan,
                    indicatorColor = RoninCyan.copy(alpha = 0.14f),
                    unselectedIconColor = RoninTextSecondary,
                    unselectedTextColor = RoninTextSecondary
                )
            )
        }
    }
}
