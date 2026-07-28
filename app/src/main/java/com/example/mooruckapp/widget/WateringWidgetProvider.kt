package com.example.mooruckapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.domain.WateringNeedChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 홈 화면 위젯: 오늘 물을 줘야 하는 식물 리스트 표시
 * 보여주기 전용 - 위젯을 탭하면 앱 실행
 * 이후 메인 화면으로 연결 예정
 */
class WateringWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { widgetId ->
                    safeRefreshWidget(context, appWidgetManager, widgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        private const val TAG = "WateringWidget"

        /** 알림 워커, 마이페이지 등 다른 곳에서 위젯을 최신 상태로 갱신하고 싶을 때 호출 */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WateringWidgetProvider::class.java),
            )
            if (widgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                widgetIds.forEach { widgetId ->
                    safeRefreshWidget(context, appWidgetManager, widgetId)
                }
            }
        }

        /** DB 조회 등에서 예외가 날 경우 위젯 갱신 실패 처리 */
        private suspend fun safeRefreshWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            try {
                refreshWidget(context, appWidgetManager, widgetId)
            } catch (e: Exception) {
                Log.w(TAG, "위젯($widgetId) 갱신 실패", e)
            }
        }

        private suspend fun refreshWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val db = AppDatabase.getInstance(context)
            val checker = WateringNeedChecker(db.userPlantDao(), db.wateringRecordDao())
            val plantsNeedingWater = checker.getPlantsNeedingWaterToday()

            val views = RemoteViews(context.packageName, R.layout.widget_watering)

            val listText = if (plantsNeedingWater.isEmpty()) {
                "오늘은 물 줄 식물이 없어요"
            } else {
                plantsNeedingWater.joinToString(", ") { it.nickname ?: it.plantName }
            }
            views.setTextViewText(R.id.tvWidgetPlantList, listText)

            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launchIntent ->
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
