package com.gymbuddy.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gymbuddy.GymBuddyApp
import com.gymbuddy.MainActivity
import com.gymbuddy.domain.StreakCalculator
import kotlinx.coroutines.flow.first

class GymWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as GymBuddyApp
        val timestamps = runCatching {
            app.container.workoutRepository.observeCompletedTimestamps().first()
        }.getOrDefault(emptyList())
        val streak = StreakCalculator.currentStreak(timestamps)
        val total = timestamps.size

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start,
                ) {
                    Text(
                        "Gym Buddy",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "🔥 $streak day streak",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        "$total workouts logged",
                        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer),
                    )
                }
            }
        }
    }
}

class GymWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GymWidget()
}
