package com.example.mooruckapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 물주기 알림용 NotificationChannel 생성 + 매일 오전 9시 전후로 실행되는
 * WateringNotificationWorker를 WorkManager에 등록
 *
 * WorkManager의 PeriodicWorkRequest는 정확히 9시 정각을 보장하지 않고
 * 기기 배터리 절약 정책에 따라 몇 분~1시간 정도 오차가 날 수 있음
 */
object WateringNotificationScheduler {

    const val CHANNEL_ID = "watering_reminder"
    private const val UNIQUE_WORK_NAME = "watering_notification_daily_check"
    private const val TARGET_HOUR = 9

    /**
     * 매일 물주기 확인 작업 등록 - 이미 등록되어 있으면 동작 X
     */
    fun schedule(context: Context) {
        createNotificationChannelIfNeeded(context)

        val request = PeriodicWorkRequestBuilder<WateringNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelayMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** 지금부터 다음 오전 9시까지 남은 시간(ms) */
    private fun calculateInitialDelayMillis(): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, TARGET_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    private fun createNotificationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "물주기 알림",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "오늘 물을 줘야 하는 식물이 있을 때 알려줍니다"
        }
        manager.createNotificationChannel(channel)
    }
}
