package com.budging.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.budging.app.BudgingApp
import com.budging.app.data.model.DashboardState
import com.budging.app.quickaccess.OpenDashboardActivity
import com.budging.app.quickaccess.OpenLogExpenseActivity
import com.budging.app.ui.format.formatCurrency

private fun widgetColor(color: Color) = ColorProvider(day = color, night = color)

class BudgingStatusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as BudgingApp).container.budgetRepository
        val snapshot = repository.getDashboardSnapshot()
        provideContent {
            WidgetContent(
                state = snapshot,
                context = context,
            )
        }
    }
}

class BudgingStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgingStatusWidget()
}

@Composable
private fun WidgetContent(
    state: DashboardState,
    context: Context,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetColor(Color.White))
            .clickable(actionStartActivity(OpenDashboardActivity::class.java))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "Budging",
                style = TextStyle(
                    color = widgetColor(Color(0xFF1B1B1D)),
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "Log",
                modifier = GlanceModifier
                    .background(widgetColor(Color(0xFFF6F3F5)))
                    .clickable(actionStartActivity(OpenLogExpenseActivity::class.java))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = TextStyle(color = widgetColor(Color.Black), fontWeight = FontWeight.Medium),
            )
        }
        Spacer(GlanceModifier.height(12.dp))
        if (!state.hasActiveBudget) {
            Text("No active budget", style = TextStyle(color = widgetColor(Color.Black), fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text("Tap to set budget", style = TextStyle(color = widgetColor(Color(0xFF45464D))))
        } else {
            Text("Total Remaining", style = TextStyle(color = widgetColor(Color(0xFF45464D))))
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = formatCurrency(state.totalRemainingMinor, state.currencyCode),
                style = TextStyle(color = widgetColor(Color.Black), fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = "${formatCurrency(state.safeDailyMinor, state.currencyCode)}/day",
                style = TextStyle(color = widgetColor(Color(0xFF1B1B1D)), fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "${state.daysRemainingInclusive} days left",
                style = TextStyle(color = widgetColor(Color(0xFF45464D))),
            )
        }
    }
}
