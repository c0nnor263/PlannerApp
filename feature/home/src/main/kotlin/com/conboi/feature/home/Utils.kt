package com.conboi.feature.home

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.conboi.navigation.NavigationKeys

fun NavGraphBuilder.homeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSearch: () -> Unit,
    onNavigateToTaskDetail: (Int) -> Unit
) {
    composable<NavigationKeys.Home>(
        enterTransition = {
            fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        exitTransition = {
            fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        val viewModel: HomeScreenViewModel = hiltViewModel()
        val userTasks by viewModel.sortedTasks.collectAsStateWithLifecycle(emptyList())
        HomeScreen(
            modifier = modifier,
            userTasks = userTasks,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToTaskDetail = onNavigateToTaskDetail
        )
    }
}