package com.budging.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.budging.app.quickaccess.OpenHistoryActivity
import com.budging.app.quickaccess.OpenLogExpenseActivity
import com.budging.app.ui.format.formatCurrency
import kotlin.math.roundToInt

private fun widgetColor(color: Color) = ColorProvider(day = color, night = color)

private val WidgetOuter = widgetColor(Color(0xFFFCF8FA))
private val WidgetSurface = widgetColor(Color(0xFFFFFFFF))
private val WidgetSurfaceAlt = widgetColor(Color(0xFFF6F3F5))
private val WidgetSurfaceStrong = widgetColor(Color(0xFFEAE7E9))
private val WidgetText = widgetColor(Color(0xFF1B1B1D))
private val WidgetMutedText = widgetColor(Color(0xFF45464D))
private val WidgetAccent = widgetColor(Color(0xFF000000))
private val WidgetAccentSoft = widgetColor(Color(0xFFD0E1FB))
private val WidgetAccentOnSoft = widgetColor(Color(0xFF000000))

class BudgingStatusWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as BudgingApp).container.budgetRepository
        val snapshot = repository.getDashboardSnapshot()
        provideContent {
            WidgetContent(
                state = snapshot,
                containerWidth = LocalSize.current.width,
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
    containerWidth: androidx.compose.ui.unit.Dp,
) {
    val compactButtons = containerWidth < 250.dp
    val progress = widgetProgress(state)
    val progressWidth = (containerWidth - if (compactButtons) 132.dp else 140.dp).coerceAtLeast(116.dp)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetOuter)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(WidgetSurface)
                .clickable(actionStartActivity(OpenDashboardActivity::class.java))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start,
            ) {
                if (state.hasActiveBudget) {
                    Text(
                        text = "TOTAL REMAINING",
                        style = TextStyle(
                            color = WidgetMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = formatCurrency(state.totalRemainingMinor, state.currencyCode),
                        style = TextStyle(
                            color = WidgetText,
                            fontSize = if (containerWidth < 280.dp) 24.sp else 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2,
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    ProgressLabels(
                        left = "${progress.spentPercent}% spent",
                        right = "${progress.leftPercent}% left",
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    ProgressBar(progress.fillFraction, progressWidth)
                } else {
                    Text(
                        text = "NO ACTIVE BUDGET",
                        style = TextStyle(
                            color = WidgetText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = "Tap to set budget",
                        style = TextStyle(
                            color = WidgetMutedText,
                            fontSize = 13.sp,
                        ),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    ProgressLabels(left = "0% spent", right = "0% left")
                    Spacer(GlanceModifier.height(8.dp))
                    ProgressBar(0f, progressWidth)
                }
            }

            Spacer(GlanceModifier.width(14.dp))

            Column(
                modifier = GlanceModifier.width(if (compactButtons) 88.dp else 96.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                ActionCard(
                    iconRes = R.drawable.ic_tile_log_expense,
                    label = "Log\nExpense",
                    onClick = actionStartActivity(OpenLogExpenseActivity::class.java),
                    fill = WidgetAccentSoft,
                    content = WidgetAccentOnSoft,
                )
                Spacer(GlanceModifier.height(10.dp))
                ActionCard(
                    iconRes = R.drawable.ic_widget_details,
                    label = if (state.hasActiveBudget) "View\nDetails" else "Set\nBudget",
                    onClick = actionStartActivity(if (state.hasActiveBudget) OpenHistoryActivity::class.java else OpenDashboardActivity::class.java),
                    fill = WidgetSurfaceAlt,
                    content = WidgetText,
                )
            }
        }
    }
}

@Composable
private fun ProgressLabels(
    left: String,
    right: String,
) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = left,
            style = TextStyle(color = WidgetMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = right,
            style = TextStyle(color = WidgetMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun ProgressBar(
    fillFraction: Float,
    trackWidth: Dp,
) {
    val fillWidth = (trackWidth.value * fillFraction.coerceIn(0f, 1f)).dp
    Box(
        modifier = GlanceModifier
            .width(trackWidth)
            .height(10.dp)
            .cornerRadius(999.dp)
            .background(WidgetSurfaceStrong),
    ) {
        Box(
            modifier = GlanceModifier
                .width(fillWidth)
                .fillMaxHeight()
                .cornerRadius(999.dp)
                .background(WidgetAccent),
        ) {}
    }
}

@Composable
private fun ActionCard(
    iconRes: Int,
    label: String,
    onClick: androidx.glance.action.Action,
    fill: androidx.glance.unit.ColorProvider,
    content: androidx.glance.unit.ColorProvider,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(18.dp)
            .background(fill)
            .clickable(onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.width(18.dp).height(18.dp),
            colorFilter = ColorFilter.tint(content),
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = label,
            style = TextStyle(
                color = content,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
        )
    }
}

private data class WidgetProgress(
    val spentPercent: Int,
    val leftPercent: Int,
    val fillFraction: Float,
)

private fun widgetProgress(state: DashboardState): WidgetProgress {
    val total = state.totalBudgetMinor.coerceAtLeast(0L)
    if (total == 0L) return WidgetProgress(0, 0, 0f)
    val spent = state.totalSpentMinor.coerceAtLeast(0L).coerceAtMost(total)
    val remaining = state.totalRemainingMinor.coerceAtLeast(0L).coerceAtMost(total)
    val spentPercent = ((spent.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    val leftPercent = ((remaining.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    return WidgetProgress(
        spentPercent = spentPercent,
        leftPercent = leftPercent,
        fillFraction = spent.toFloat() / total.toFloat(),
    )
}
