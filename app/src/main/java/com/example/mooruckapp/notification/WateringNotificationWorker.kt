package com.example.mooruckapp.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mooruckapp.R
import com.example.mooruckapp.data.local.AppDatabase
import com.example.mooruckapp.domain.WateringNeedChecker

/**
 * 매일 실행되는 물주기 알림 워커
 *
 * 1) 사용자가 알림을 꺼뒀으면 아무것도 하지 않음
 * 2) 오늘 물 줘야 하는 식물이 없으면 아무것도 하지 않음
 * 3) 하나 이상 있으면 "N개 식물에게 물을 주세요" 형태로 알림 1개만 표시
 */
class WateringNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)

        val user = db.userDao().getUser() ?: return Result.success()
        if (!user.wateringNotificationEnabled) return Result.success()

        val checker = WateringNeedChecker(db.userPlantDao(), db.wateringRecordDao())
        val plantsNeedingWater = checker.getPlantsNeedingWaterToday()
        if (plantsNeedingWater.isEmpty()) return Result.success()

        showNotification(plantCount = plantsNeedingWater.size)
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

        // TODO: 알림 탭 시 메인 화면(main-page)으로 이동하도록 setContentIntent() 연결 예정
        // 화면 연결되면 PendingIntent 추가하기
        val notification = NotificationCompat.Builder(applicationContext, WateringNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("물 줄 시간이에요!")
            .setContentText("오늘 물 줄 식물이 ${plantCount}개 있어요")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
