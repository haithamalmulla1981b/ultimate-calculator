package com.hmalmulla.calculator.system

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hmalmulla.calculator.MainActivity
import com.hmalmulla.calculator.R

/**
 * Home-screen widget: shows the last result and opens the calculator,
 * scientific mode or the unit converter with one tap.
 */
class QuickCalcWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        private const val PREFS = "widget"
        private const val KEY = "last_result"

        fun saveLastResult(context: Context, result: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, result).apply()
            val m = AppWidgetManager.getInstance(context)
            val ids = m.getAppWidgetIds(ComponentName(context, QuickCalcWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { m.updateAppWidget(it, views(context)) }
        }

        private fun open(context: Context, route: String, requestCode: Int): PendingIntent {
            val i = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_ROUTE, route)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun views(context: Context): RemoteViews {
            val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "0") ?: "0"
            return RemoteViews(context.packageName, R.layout.widget_quick_calc).apply {
                setTextViewText(R.id.widget_result, last)
                setOnClickPendingIntent(R.id.widget_root, open(context, "calculator", 1))
                setOnClickPendingIntent(R.id.widget_btn_calc, open(context, "calculator", 2))
                setOnClickPendingIntent(R.id.widget_btn_sci, open(context, "scientific", 3))
                setOnClickPendingIntent(R.id.widget_btn_conv, open(context, "converter", 4))
            }
        }
    }
}
