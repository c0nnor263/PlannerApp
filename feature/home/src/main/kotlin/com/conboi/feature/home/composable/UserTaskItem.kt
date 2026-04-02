package com.conboi.feature.home.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conboi.core.domain.source.local.model.UserTaskDTO
import com.conboi.core.ui.R
import com.conboi.core.ui.theme.PlannerAppTheme

@Composable
fun UserTaskItem(
    modifier: Modifier = Modifier,
    task: UserTaskDTO,
    onNavigateToTaskDetail: (Int) -> Unit
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(55.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        // fun setPriorityColor(priority: Priority?) {
//    setBackgroundResource(
//        when (priority) {
//            Priority.LEISURELY -> com.conboi.core.ui.R.drawable.gradient_priority_leisurely
//            Priority.DEFAULT -> com.conboi.core.ui.R.drawable.gradient_priority_default
//            Priority.ADVISABLE -> com.conboi.core.ui.R.drawable.gradient_priority_advisable
//            Priority.IMPORTANT -> com.conboi.core.ui.R.drawable.gradient_priority_important
//            else -> com.conboi.core.ui.R.drawable.gradient_priority_default
//        }
//    )
// }
//
// if (title.isBlank()) {
//    alpha = 1.0F
//    setBackgroundResource(R.color.secondaryDarkColorWater)
// } else {
//    alpha =
//        if (checked) {
//            if (total > 1) {
//                setPriorityColor(priority)
//                1.0F
//            } else {
//                setBackgroundResource(0)
//                0.5F
//            }
//        } else {
//            setPriorityColor(priority)
//            1.0F
//        }
// }
// background?.alpha = 200
// if (missed) {
//    alpha = 1.0F
//    setBackgroundResource(R.color.secondaryDarkColorFire)
// }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(
                        if (task.isCompleted) {
                            0.5F
                        } else {
                            1.0F
                        }
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CheckboxInfo(task)
            TitleTaskEditing(
                task = task
            )
            Image(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier =
                    Modifier.clickable {
                        onNavigateToTaskDetail(task.id)
                    }
            )
            SavingIndicator()
        }
    }
}

@Composable
fun CheckboxInfo(
    task: UserTaskDTO,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { isChecked ->
            },
            modifier = Modifier.align(Alignment.Center)
        )
        if (task.totalCompleted > 1) {
            Text(
                text =
                    if (task.totalCompleted < 100) {
                        task.totalCompleted.toString()
                    } else {
                        stringResource(R.string.max_total_check, 99)
                    },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun TitleTaskEditing(task: UserTaskDTO) {
    val titleTextState = rememberTextFieldState()
    BasicTextField(
        state = titleTextState,
        textStyle =
            TextStyle.Default.copy(
                textDecoration =
                    if (task.isCompleted && task.totalCompleted > 1) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
            )
    )
}

@Composable
fun SavingIndicator() {
    /**
     * <ImageView
     *                 android:id="@+id/iv_saving_title_indicator"
     *                 android:layout_width="24dp"
     *                 android:layout_height="match_parent"
     *                 android:layout_margin="12dp"
     *                 android:contentDescription="@string/saving_title_indicator"
     *                 android:src="@drawable/saving_anim"
     *                 android:visibility="gone"
     *                 app:layout_constraintBottom_toBottomOf="@+id/til_title"
     *                 app:layout_constraintEnd_toEndOf="parent"
     *                 app:layout_constraintTop_toTopOf="@+id/til_title"
     *                 tools:visibility="visible" />
     */
}

@Preview
@Composable
private fun UserTaskItemPreview() {
    val exampleTask =
        remember {
            UserTaskDTO(
                id = 1,
                title = "Task 1",
                description = "Description 1",
                isCompleted = false
            )
        }
    PlannerAppTheme {
        UserTaskItem(
            task = exampleTask,
            onNavigateToTaskDetail = {}
        )
    }
}