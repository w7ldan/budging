package com.budging.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.budging.app.BudgingApp
import com.budging.app.R
import com.budging.app.data.model.DashboardState
import com.budging.app.quickaccess.OpenDashboardActivity
import com.budging.app.quickaccess.OpenLogExpenseActivity
import com.budging.app.ui.format.formatCurrency

private fun widgetColor(color: Color) = ColorProvider(day = color, night = color)

private val AppBackground = widgetColor(Color(0xFFFCF8FA))
private val AppSurfaceLow = widgetColor(Color(0xFFF6F3F5))
private val AppText = widgetColor(Color(0xFF1B1B1D))
private val AppMutedText = widgetColor(Color(0xFF45464D))
private val AppPrimary = widgetColor(Color(0xFF000000))

class BudgingStatusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as BudgingApp).container.budgetRepository
        val snapshot = repository.getDashboardSnapshot()
        provideContent {
            val size = LocalSize.current
            WidgetContent(
                state = snapshot,
                context = context,
                containerWidth = size.width,
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
    containerWidth: androidx.compose.ui.unit.Dp,
) {
    val isCompact = containerWidth < 260.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(AppBackground)
            .clickable(actionStartActivity(OpenDashboardActivity::class.java))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        // Header row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Budging",
                style = TextStyle(
                    color = AppText,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.defaultWeight())
            // Log button
            Row(
                modifier = GlanceModifier
                    .background(AppPrimary)
                    .cornerRadius(12.dp)
                    .clickable(actionStartActivity(OpenLogExpenseActivity::class.java))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_tile_log_expense),
                    contentDescription = "Log",
                    modifier = GlanceModifier.width(14.dp).height(14.dp),
                    colorFilter = ColorFilter.tint(widgetColor(Color.White)),
                )
                if (!isCompact) {
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = "Log",
                        style = TextStyle(
                            color = widgetColor(Color.White),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(if (isCompact) 6.dp else 10.dp))

        if (!state.hasActiveBudget) {
            Text(
                text = "No active budget",
                style = TextStyle(color = AppText, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Tap to set budget",
                style = TextStyle(color = AppMutedText),
            )
        } else {
            Text(
                text = formatCurrency(state.totalRemainingMinor, state.currencyCode),
                style = TextStyle(color = AppText, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = "remaining",
                style = TextStyle(color = AppMutedText),
            )

            if (!isCompact) {
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier
                            .background(AppSurfaceLow)
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${formatCurrency(state.safeDailyMinor, state.currencyCode)}/day",
                            style = TextStyle(color = AppText, fontWeight = FontWeight.Medium),
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Row(
                        modifier = GlanceModifier
                            .background(AppSurfaceLow)
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "${state.daysRemainingInclusive}d left",
                            style = TextStyle(color = AppMutedText),
                        )
                    }
                }
            }
        }
    }
}
