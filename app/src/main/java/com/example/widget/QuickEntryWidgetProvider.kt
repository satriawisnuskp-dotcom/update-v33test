package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R

class QuickEntryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val views = buildRemoteViews(context)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun buildRemoteViews(context: Context): RemoteViews {
            val snapshot = WidgetDataHelper.loadSnapshot(context)
            val hide = snapshot.isBalanceHidden
            val todayText = if (hide) {
                "Hari ini: -Rp •••••"
            } else {
                "Hari ini: -${WidgetDataHelper.formatCompactOrMasked(snapshot.todayExpense, false)}"
            }

            val views = RemoteViews(context.packageName, R.layout.widget_quick_entry)
            views.setTextViewText(R.id.tv_quick_today_expense, todayText)

            views.setOnClickPendingIntent(
                R.id.btn_quick_expense,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_QUICK_ADD_EXPENSE,
                    requestCode = 201
                )
            )
            views.setOnClickPendingIntent(
                R.id.btn_quick_income,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_QUICK_ADD_INCOME,
                    requestCode = 202
                )
            )
            views.setOnClickPendingIntent(
                R.id.btn_quick_transfer,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_QUICK_ADD_TRANSFER,
                    requestCode = 203
                )
            )
            views.setOnClickPendingIntent(
                R.id.btn_quick_summary,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_OPEN_SUMMARY,
                    requestCode = 204
                )
            )
            return views
        }
    }
}
