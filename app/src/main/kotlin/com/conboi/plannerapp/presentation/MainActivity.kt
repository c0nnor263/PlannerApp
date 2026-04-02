package com.conboi.plannerapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.conboi.core.ui.theme.PlannerAppTheme
import com.conboi.plannerapp.navigation.AppContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlannerAppTheme {
                AppContent()
            }
        }
    }
}