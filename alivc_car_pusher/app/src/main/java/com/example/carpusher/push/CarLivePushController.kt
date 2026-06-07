package com.example.carpusher.push

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import com.alivc.live.pusher.AlivcLivePushError
import com.alivc.live.pusher.AlivcLivePushErrorListener
import com.alivc.live.pusher.AlivcLivePushInfoListener
import com.alivc.live.pusher.AlivcLivePushNetworkListener
import com.alivc.live.pusher.AlivcLivePushStatsInfo
import com.alivc.live.pusher.AlivcLivePusher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车机推流核心控制器：封装 [AlivcLivePusher] 的完整生命周期、状态机、回调监听与自动重连。
 *
 * 线程说明：SDK 回调可能在内部线程触发，[MutableStateFlow.value] / [MutableSharedFlow.tryEmit]
 * 均为线程安全，UI 层通过 collect 切回主线程消费即可。
 *
 * 典型调用顺序：
 *   prepare() -> startPreview(surfaceView) -> (onPreviewStarted) -> startPush(url)
 */
class CarLivePushController(private val appContext: Context) {

    private val _state = MutableStateFlow(PushState.IDLE)
    val state: StateFlow<PushState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PushEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<PushEvent> = _events.asSharedFlow()

    private var pusher: AlivcLivePusher? = null

    private var pushUrl: String? = null

    /** 业务希望保持推流（用于网络恢复后自动续推、避免误触发） */
    @Volatile
    private var wantPushing = false

    @Volatile
    private var previewStarted = false

    @Volatile
    private var isPrepared = false

    val currentState: PushState get() = _state.value

    // region 生命周期控制

    /** 初始化推流引擎与配置。重复调用安全。 */
    fun prepare(useFrontCamera: Boolean, audioOnly: Boolean) {
        if (isPrepared) return
        val config = CarPushConfigFactory.create(useFrontCamera, audioOnly)
        val p = AlivcLivePusher()
        try {
            p.init(appContext, config)
            p.setLivePushInfoListener(infoListener)
            p.setLivePushNetworkListener(networkListener)
            p.setLivePushErrorListener(errorListener)
            pusher = p
            isPrepared = true
            Log.i(TAG, "推流引擎初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "推流引擎初始化失败", e)
            emitError("INIT", e.message ?: "init failed")
        }
    }

    /** 开始摄像头预览。需传入车机界面上的 [SurfaceView]。 */
    fun startPreview(surfaceView: SurfaceView) {
        val p = pusher ?: run {
            Log.w(TAG, "startPreview 前请先 prepare()")
            return
        }
        try {
            p.startPreview(surfaceView)
        } catch (e: Exception) {
            Log.e(TAG, "startPreview 失败", e)
            emitError("PREVIEW", e.message ?: "preview failed")
        }
    }

    /**
     * 开始推流。
     * @param url 推流地址，rtmp:// 或 artc://（RTS 超低延时）
     *
     * 若预览尚未就绪，会记录意图，待 onPreviewStarted 后自动开始。
     */
    fun startPush(url: String) {
        if (url.isBlank()) {
            emitError("URL", "推流地址为空")
            return
        }
        pushUrl = url
        wantPushing = true
        val p = pusher ?: return
        if (!previewStarted) {
            Log.i(TAG, "预览未就绪，待 onPreviewStarted 后自动推流")
            return
        }
        doStartPush(p, url)
    }

    private fun doStartPush(p: AlivcLivePusher, url: String) {
        try {
            _state.value = PushState.CONNECTING
            // 使用异步接口，避免阻塞调用线程
            p.startPushAsync(url)
            Log.i(TAG, "startPushAsync: $url")
        } catch (e: Exception) {
            Log.e(TAG, "startPush 失败", e)
            emitError("PUSH", e.message ?: "push failed")
        }
    }

    fun stopPush() {
        wantPushing = false
        val p = pusher ?: return
        try {
            p.stopPush()
        } catch (e: Exception) {
            Log.e(TAG, "stopPush 失败", e)
        }
        _state.value = if (previewStarted) PushState.PREVIEWING else PushState.IDLE
    }

    fun pause() {
        val p = pusher ?: return
        runCatching { p.pause() }.onFailure { Log.e(TAG, "pause 失败", it) }
    }

    fun resume() {
        val p = pusher ?: return
        runCatching { p.resumeAsync() }.onFailure { Log.e(TAG, "resume 失败", it) }
    }

    fun switchCamera() {
        val p = pusher ?: return
        runCatching { p.switchCamera() }.onFailure { Log.e(TAG, "switchCamera 失败", it) }
    }

    fun setMute(mute: Boolean) {
        val p = pusher ?: return
        runCatching { p.setMute(mute) }.onFailure { Log.e(TAG, "setMute 失败", it) }
    }

    /** 销毁引擎，释放摄像头/麦克风等资源。 */
    fun release() {
        wantPushing = false
        val p = pusher ?: return
        runCatching { if (p.isPushing) p.stopPush() }
        runCatching { p.stopPreview() }
        runCatching { p.destroy() }
        pusher = null
        isPrepared = false
        previewStarted = false
        _state.value = PushState.IDLE
        Log.i(TAG, "推流引擎已销毁")
    }

    // endregion

    // region 网络事件（由 NetworkMonitor 驱动）

    /** 底层网络可用（含 Wi-Fi/蜂窝切换后的新链路）：若期望推流但当前异常，则主动重连。 */
    fun onNetworkAvailable() {
        val p = pusher ?: return
        val url = pushUrl ?: return
        if (!wantPushing) return
        if (_state.value == PushState.RECONNECTING || _state.value == PushState.ERROR) {
            Log.i(TAG, "网络恢复，主动 reconnectPushAsync")
            runCatching { p.reconnectPushAsync(url) }
                .onFailure { Log.e(TAG, "reconnect 失败", it) }
        }
    }

    fun onNetworkLost() {
        if (wantPushing && _state.value == PushState.PUSHING) {
            _state.value = PushState.RECONNECTING
        }
    }

    // endregion

    private fun emitError(code: String, message: String) {
        _state.value = PushState.ERROR
        _events.tryEmit(PushEvent.Error(code, message))
    }

    // region SDK 回调监听器

    private val infoListener = object : AlivcLivePushInfoListener {
        override fun onPreviewStarted(pusher: AlivcLivePusher?) {
            previewStarted = true
            if (_state.value == PushState.IDLE) _state.value = PushState.PREVIEWING
            // 预览就绪后，如已有推流意图则立即开始
            val url = pushUrl
            if (wantPushing && url != null) {
                doStartPush(pusher ?: return, url)
            }
        }

        override fun onPreviewStopped(pusher: AlivcLivePusher?) {
            previewStarted = false
        }

        override fun onPushStarted(pusher: AlivcLivePusher?) {
            _state.value = PushState.PUSHING
            _events.tryEmit(PushEvent.Info("推流已开始"))
        }

        override fun onFirstFramePreviewed(pusher: AlivcLivePusher?) {}

        override fun onFirstFramePushed(pusher: AlivcLivePusher?) {
            _events.tryEmit(PushEvent.Info("首帧已发送，推流成功"))
        }

        override fun onPushPaused(pusher: AlivcLivePusher?) {
            _state.value = PushState.PAUSED
        }

        override fun onPushResumed(pusher: AlivcLivePusher?) {
            _state.value = PushState.PUSHING
        }

        override fun onPushRestarted(pusher: AlivcLivePusher?) {
            _state.value = PushState.PUSHING
        }

        override fun onPushStopped(pusher: AlivcLivePusher?) {
            _state.value = if (previewStarted) PushState.PREVIEWING else PushState.IDLE
        }

        override fun onPushStatistics(pusher: AlivcLivePusher?, statistics: AlivcLivePushStatsInfo?) {}

        override fun onAdjustBitrate(pusher: AlivcLivePusher?, currentBitrate: Int, targetBitrate: Int) {}

        override fun onAdjustFps(pusher: AlivcLivePusher?, currentFps: Int, targetFps: Int) {}

        override fun onDropFrame(pusher: AlivcLivePusher?, beforeCount: Int, afterCount: Int) {}
    }

    private val networkListener = object : AlivcLivePushNetworkListener {
        override fun onNetworkPoor(pusher: AlivcLivePusher?) {
            _events.tryEmit(PushEvent.NetworkPoor("当前网络较差，已自动降码率"))
        }

        override fun onNetworkRecovery(pusher: AlivcLivePusher?) {
            _events.tryEmit(PushEvent.Info("网络已恢复"))
        }

        override fun onConnectFail(pusher: AlivcLivePusher?) {
            emitError("CONNECT_FAIL", "推流连接失败，请检查推流地址/鉴权")
        }

        override fun onConnectionLost(pusher: AlivcLivePusher?) {
            if (wantPushing) _state.value = PushState.RECONNECTING
        }

        override fun onReconnectStart(pusher: AlivcLivePusher?) {
            _state.value = PushState.RECONNECTING
        }

        override fun onReconnectSucceed(pusher: AlivcLivePusher?) {
            _state.value = PushState.PUSHING
            _events.tryEmit(PushEvent.Info("重连成功"))
        }

        override fun onReconnectFail(pusher: AlivcLivePusher?) {
            // 超过 SDK 重连上限：保持期望推流标记，等待 NetworkMonitor 在新链路可用时再触发
            _state.value = PushState.ERROR
            _events.tryEmit(PushEvent.Error("RECONNECT_FAIL", "自动重连失败，等待网络恢复"))
        }

        override fun onSendDataTimeout(pusher: AlivcLivePusher?) {
            _events.tryEmit(PushEvent.NetworkPoor("发送数据超时"))
        }

        override fun onPacketsLost(pusher: AlivcLivePusher?) {}

        override fun onSendMessage(pusher: AlivcLivePusher?) {}

        override fun onPushURLAuthenticationOverdue(pusher: AlivcLivePusher?): String {
            // 鉴权过期：通知业务层下发新地址。此处返回空串，由上层重新 startPush 新 URL。
            _events.tryEmit(PushEvent.AuthExpired)
            return pushUrl ?: ""
        }
    }

    private val errorListener = object : AlivcLivePushErrorListener {
        override fun onSystemError(livePusher: AlivcLivePusher?, error: AlivcLivePushError?) {
            // 系统级错误：官方建议销毁引擎重建
            emitError("SYSTEM", describe(error))
        }

        override fun onSDKError(livePusher: AlivcLivePusher?, error: AlivcLivePushError?) {
            emitError("SDK", describe(error))
        }

        private fun describe(error: AlivcLivePushError?): String =
            error?.toString() ?: "unknown error"
    }

    // endregion

    companion object {
        private const val TAG = "CarLivePushController"
    }
}
