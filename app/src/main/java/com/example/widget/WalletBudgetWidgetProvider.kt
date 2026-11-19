package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.R

class WalletBudgetWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_wallet_budget)

            if (snapshot.hasBudgets) {
                val pct = snapshot.budgetPercent
                views.setTextViewText(R.id.tv_budget_percent, "$pct%")
                val pctColor = when {
                    pct >= 100 -> Color.parseColor("#F87171")
                    pct >= 80 -> Color.parseColor("#FBBF24")
                    else -> Color.parseColor("#60A5FA")
                }
                views.setTextColor(R.id.tv_budget_percent, pctColor)
                views.setTextViewText(
                    R.id.tv_budget_amounts,
                    if (hide) {
                        "Terpakai Rp ••••• / Rp •••••"
                    } else {
                        "Terpakai ${WidgetDataHelper.formatCompactOrMasked(snapshot.totalBudgetSpent, false)} / ${WidgetDataHelper.formatCompactOrMasked(snapshot.totalBudgetLimit, false)}"
                    }
                )
                views.setProgressBar(R.id.progress_widget_budget, 100, pct.coerceIn(0, 100), false)
            } else {
                views.setTextViewText(R.id.tv_budget_percent, "Belum Diatur")
                views.setTextColor(R.id.tv_budget_percent, Color.parseColor("#94A3B8"))
                views.setTextViewText(R.id.tv_budget_amounts, "Ketuk untuk mengatur anggaran bulan ini")
                views.setProgressBar(R.id.progress_widget_budget, 100, 0, false)
            }

            val wallets = snapshot.topWallets
            if (wallets.isEmpty()) {
                views.setViewVisibility(R.id.tv_wallets_empty, View.VISIBLE)
                views.setViewVisibility(R.id.row_wallet_1, View.GONE)
                views.setViewVisibility(R.id.row_wallet_2, View.GONE)
                views.setViewVisibility(R.id.row_wallet_3, View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_wallets_empty, View.GONE)
                val rowSlots = listOf(
                    Triple(R.id.row_wallet_1, R.id.tv_wallet_1_name, R.id.tv_wallet_1_balance),
                    Triple(R.id.row_wallet_2, R.id.tv_wallet_2_name, R.id.tv_wallet_2_balance),
                    Triple(R.id.row_wallet_3, R.id.tv_wallet_3_name, R.id.tv_wallet_3_balance)
                )
                rowSlots.forEachIndexed { idx, (rowId, nameId, balId) ->
                    val wallet = wallets.getOrNull(idx)
                    if (wallet != null) {
                        views.setViewVisibility(rowId, View.VISIBLE)
                        views.setTextViewText(nameId, wallet.name)
                        views.setTextViewText(
                            balId,
                            WidgetDataHelper.formatAmountOrMasked(wallet.balance, hide)
                        )
                    } else {
                        views.setViewVisibility(rowId, View.GONE)
                    }
                }
            }

            views.setOnClickPendingIntent(
                R.id.layout_budget_section,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_OPEN_BUDGET,
                    requestCode = 301
                )
            )
            views.setOnClickPendingIntent(
                R.id.layout_wallets_section,
                WidgetDataHelper.createLaunchPendingIntent(
                    context = context,
                    action = WidgetNavigation.ACTION_OPEN_ACCOUNTS,
                    requestCode = 302
                )
            )
            return views
        }
    }
}
