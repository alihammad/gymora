package com.gymora.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.gymora.LaunchIntents
import com.gymora.R
import com.gymora.domain.usecase.Engagement
import com.gymora.domain.usecase.GetEngagementUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen widget: weekly streak, progress towards the weekly goal, and a
 * button that starts the suggested next routine. Refreshed hourly by the system
 * (to roll over into a new week) and whenever the app goes to the background.
 */
@AndroidEntryPoint
class GymoraWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var getEngagement: GetEngagementUseCase

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val engagement = runCatching { getEngagement() }.getOrNull()
                val views = render(context, engagement)
                appWidgetIds.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun render(context: Context, engagement: Engagement?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_gymora).apply {
            setOnClickPendingIntent(R.id.widget_root, LaunchIntents.openApp(context))
            if (engagement == null) return@apply
            val progress = engagement.progress
            setTextViewText(R.id.widget_streak, progress.streakWeeks.toString())
            setTextViewText(R.id.widget_progress, "${progress.done} of ${progress.goal} this week")
            val routine = engagement.nextRoutine
            if (routine == null) {
                setViewVisibility(R.id.widget_start, View.GONE)
            } else {
                setViewVisibility(R.id.widget_start, View.VISIBLE)
                setTextViewText(R.id.widget_start, "▶  Start ${routine.name}")
                setOnClickPendingIntent(R.id.widget_start, LaunchIntents.startRoutine(context, routine.id))
            }
        }

    companion object {
        /** Re-render every placed widget, e.g. after a workout or routine change. */
        fun refresh(context: Context) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, GymoraWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, GymoraWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }
}
