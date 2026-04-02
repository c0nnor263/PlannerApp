package com.conboi.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.conboi.core.domain.source.local.model.UserTaskDTO
import com.conboi.feature.home.composable.UserTaskItem

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userTasks: List<UserTaskDTO>,
    onNavigateToSearch: () -> Unit,
    onNavigateToTaskDetail: (Int) -> Unit
) {
    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val (imageBg, taskList) = createRefs()
        Image(
            painter = painterResource(id = R.drawable.main_fragment_bg),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier =
                Modifier.constrainAs(imageBg) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        )

        // TODO: Add Material Shared Element Transition
        // TODO: Add There is no tasks to show
        LazyColumn(
            modifier =
                Modifier
                    .constrainAs(taskList) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
                    .statusBarsPadding()
        ) {
            items(userTasks) { item ->
                UserTaskItem(
                    task = item,
                    onNavigateToTaskDetail = onNavigateToTaskDetail
                )
            }
        }

        // TODO: Add Snackbar

        // TODO: Add AdView
    }
}