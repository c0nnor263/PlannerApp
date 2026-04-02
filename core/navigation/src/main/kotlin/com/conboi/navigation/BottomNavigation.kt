package com.conboi.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigation() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
    ) {
        BottomNavigationPanel()

        // Floating button positioned over the cutout
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.Blue),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton({
//                        viewModel.insertTask()
            }, modifier = Modifier.padding(16.dp)) {
            }
        }
    }
}