package com.conboi.plannerapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.conboi.feature.home.HomeScreenViewModel
import com.conboi.feature.home.homeScreen
import com.conboi.navigation.BottomNavigation
import com.conboi.navigation.NavigationKeys

@Composable
fun AppContent(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentScreenDestination = navController.currentBackStackEntryAsState().value?.destination
    hiltViewModel()
    Scaffold(
        modifier = modifier,
        content = { innerPadding ->
            NavHost(
                modifier = modifier.padding(innerPadding),
                navController = navController,
                startDestination = NavigationKeys.Home
            ) {
                homeScreen(
                    onNavigateToSearch = {
                        navController.navigate(NavigationKeys.Home.Search)
                    },
                    onNavigateToTaskDetail = { id ->
                        navController.navigate(NavigationKeys.Home.TaskDetail(id))
                    }
                )
            }
        },
        floatingActionButtonPosition =
            when {
                currentScreenDestination?.hasRoute<NavigationKeys.Home>() == true -> FabPosition.Center
                else -> FabPosition.End
            },
        bottomBar = {
            BottomNavigation()
        }
    )
}