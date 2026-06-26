package com.budging.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budging.app.ui.BudgingRoot
import com.budging.app.ui.BudgingViewModelFactory
import com.budging.app.ui.theme.BudgingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BudgingApp
        setContent {
            BudgingTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BudgingRoot(
                        viewModel = viewModel(
                            factory = BudgingViewModelFactory(app.container.budgetRepository),
                        ),
                    )
                }
            }
        }
    }
}
