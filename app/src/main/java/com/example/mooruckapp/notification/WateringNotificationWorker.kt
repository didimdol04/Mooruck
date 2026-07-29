package com.example.mooruckapp.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mooruckapp.MainActivity
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.domain.WateringNeedChecker
import com.example.mooruckapp.widget.WateringWidgetProvider

/**
 * 매일 실행되는 물주기 확인 워커
 *
 * 1) 오늘 물 줘야 하는 식물 목록을 계산해서 위젯을 항상 최신 상태로 갱신
 * 2) 사용자가 알림을 켜뒀고, 물 줄 식물이 하나 이상 있으면 "N개 식물에게 물을 주세요" 알림 1개만 표시
 */
class WateringNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)

        val checker = WateringNeedChecker(db.userPlantDao(), db.wateringRecordDao())
        val plantsNeedingWater = checker.getPlantsNeedingWaterToday()

        // 위젯은 알림 켜짐/꺼짐과 무관하게 항상 최신 정보 표시
        WateringWidgetProvider.updateAllWidgets(applicationContext)

        val user = db.userDao().getUser()
        if (user?.wateringNotificationEnabled == true && plantsNeedingWater.isNotEmpty()) {
            showNotification(plantCount = plantsNeedingWater.size)
        }

        return Result.success()
    }

    private fun showNotification(plantCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) return
        }

        // 알림 탭 시 앱(메인 화면)을 열도록 연결. 위젯 탭 처리와 동일한 방식(FLAG_IMMUTABLE)
        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, WateringNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("물 줄 시간이에요!")
            .setContentText("오늘 물 줄 식물이 ${plantCount}개 있어요")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
