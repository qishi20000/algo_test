package com.example.carpusher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.carpusher.R
import com.example.carpusher.push.CarLivePushController
import com.example.carpusher.push.NetworkMonitor
import com.example.carpusher.push.PushState
import com.example.carpusher.ui.MainActivity
import kotlinx.coroutines.launch

/**
 * 车机推流前台服务。
 *
 * 职责：
 *  - 持有 [CarLivePushController]，保证推流引擎与 Activity 生命周期解耦（锁屏/退后台仍持续推流）；
 *  - 以前台服务 + 常驻通知运行，规避后台被系统回收；
 *  - 通过 [NetworkMonitor] 在车机网络切换后驱动自动重连；
 *  - 通过 [LocalBinder] 把控制器暴露给 UI 层。
 */
class CarLivePushService : LifecycleService() {

    val controller: CarLivePushController by lazy { CarLivePushController(applicationContext) }

    private lateinit var networkMonitor: NetworkMonitor

    inner class LocalBinder : Binder() {
        val service: CarLivePushService get() = this@CarLivePushService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        networkMonitor = NetworkMonitor(
            context = this,
            onAvailable = { controller.onNetworkAvailable() },
            onLost = { controller.onNetworkLost() }
        )
        networkMonitor.start()

        // 跟随推流状态刷新前台通知
        lifecycleScope.launch {
            controller.state.collect { state ->
                updateNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification(controller.currentState))
            ACTION_STOP -> {
                controller.stopPush()
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        networkMonitor.stop()
        controller.release()
        super.onDestroy()
    }

    // region 通知

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notification_channel_desc) }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: PushState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CarLivePushService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(stateText(state))
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .build()
    }

    private fun updateNotification(state: PushState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun stateText(state: PushState): String = when (state) {
        PushState.IDLE -> getString(R.string.state_idle)
        PushState.PREVIEWING -> getString(R.string.state_previewing)
        PushState.CONNECTING -> getString(R.string.state_connecting)
        PushState.PUSHING -> getString(R.string.state_pushing)
        PushState.PAUSED -> getString(R.string.state_paused)
        PushState.RECONNECTING -> getString(R.string.state_reconnecting)
        PushState.ERROR -> getString(R.string.state_error)
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    // endregion

    companion object {
        const val ACTION_START = "com.example.carpusher.action.START"
        const val ACTION_STOP = "com.example.carpusher.action.STOP"

        private const val CHANNEL_ID = "car_push_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, CarLivePushService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
