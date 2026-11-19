package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class BalanceSummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetNavigation.ACTION_TOGGLE_HIDE_BALANCE) {
            WidgetDataHelper.toggleBalanceHidden(context)
        }
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
            val views = RemoteViews(context.packageName, R.layout.widget_balance_summary)

            val subtitle = if (snapshot.walletCount > 0) {
                "${snapshot.walletCount} dompet aktif"
            } else {
                "Belum ada dompet"
            }
            views.setTextViewText(R.id.tv_widget_wallet_subtitle, subtitle)

            views.setTextViewText(
                R.id.tv_widget_total_balance,
                WidgetDataHelper.formatAmountOrMasked(snapshot.totalWalletBalance, hide)
            )
            views.setTextViewText(
                R.id.tv_widget_net_worth,
                "Nilai Bersih: ${WidgetDataHelper.formatCompactOrMasked(snapshot.netWorth, hide)}"
            )
            views.setTextViewText(
                R.id.tv_widget_income,
                if (hide) "+Rp •••••" else "+${WidgetDataHelper.formatCompactOrMasked(snapshot.monthlyIncome, false)}"
            )
            views.setTextViewText(
                R.id.tv_widget_expense,
                if (hide) "-Rp •••••" else "-${WidgetDataHelper.formatCompactOrMasked(snapshot.monthlyExpense, false)}"
            )
            views.setImageViewResource(
                R.id.btn_widget_toggle_hide,
                if (hide) R.drawable.ic_widget_eye_off else R.drawable.ic_widget_eye
            )

            val toggleIntent = Intent(context, BalanceSummaryWidgetProvider::class.java).apply {
                action = WidgetNavigation.ACTION_TOGGLE_HIDE_BALANCE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_toggle_hide, togglePendingIntent)

            views.setOnClickPendingIntent(
                R.id.btn_widget_quick_add,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_QUICK_ADD_EXPENSE,
                    requestCode = 102
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_balance_root,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_OPEN_ACCOUNTS,
                    requestCode = 103
                )
            )
            return views
        }
    }
}
