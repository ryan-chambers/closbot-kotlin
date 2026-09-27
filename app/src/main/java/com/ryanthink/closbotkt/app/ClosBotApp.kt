package com.ryanthink.closbotkt.app

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ryanthink.closbotkt.feature.chat.ChatScreen
import com.ryanthink.closbotkt.feature.notes.AddNoteScreen
import com.ryanthink.closbotkt.feature.notes.EditNoteScreen
import com.ryanthink.closbotkt.feature.notes.GalleryScreen
import com.ryanthink.closbotkt.feature.vintage.VintageScreen

@Composable
fun ClosBotApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { top ->
        currentDestination?.hierarchy?.any { it.hasRoute(top.route::class) } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { top ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy
                                ?.any { it.hasRoute(top.route::class) } == true,
                            onClick = { navController.navigateToTopLevel(top) },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(stringResource(top.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ChatRoute,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<ChatRoute> { ChatScreen() }
            composable<AddNoteRoute> { AddNoteScreen() }
            composable<GalleryRoute> {
                GalleryScreen(onNoteClick = { navController.navigate(EditNoteRoute(it)) })
            }
            composable<VintageRoute> { VintageScreen() }
            composable<EditNoteRoute> { entry ->
                EditNoteScreen(noteId = entry.toRoute<EditNoteRoute>().noteId)
            }
        }
    }
}

/** Switches tab without stacking copies, and restores each tab's own back stack and state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
