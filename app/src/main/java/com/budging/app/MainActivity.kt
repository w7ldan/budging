package com.budging.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budging.app.quickaccess.QuickAccessNavigation
import com.budging.app.ui.BudgingRoot
import com.budging.app.ui.BudgingViewModelFactory
import com.budging.app.ui.theme.BudgingTheme

class MainActivity : ComponentActivity() {
    private var externalRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalRoute = QuickAccessNavigation.routeFromIntent(intent)
        val app = application as BudgingApp
        setContent {
            BudgingTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BudgingRoot(
                        viewModel = viewModel(
                            factory = BudgingViewModelFactory(
                                app.container.budgetRepository,
                                app.container.backupRepository,
                            ),
                        ),
                        externalRoute = externalRoute,
                        onExternalRouteConsumed = { externalRoute = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalRoute = QuickAccessNavigation.routeFromIntent(intent)
    }
}
