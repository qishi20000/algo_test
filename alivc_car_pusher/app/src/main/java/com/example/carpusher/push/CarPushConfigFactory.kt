package com.example.carpusher.push

import com.alivc.live.annotations.AlivcLiveMode
import com.alivc.live.pusher.AlivcAudioAACProfileEnum
import com.alivc.live.pusher.AlivcEncodeModeEnum
import com.alivc.live.pusher.AlivcFpsEnum
import com.alivc.live.pusher.AlivcLivePushCameraTypeEnum
import com.alivc.live.pusher.AlivcLivePushConfig
import com.alivc.live.pusher.AlivcPreviewDisplayMode
import com.alivc.live.pusher.AlivcPreviewOrientationEnum
import com.alivc.live.pusher.AlivcQualityModeEnum
import com.alivc.live.pusher.AlivcResolutionEnum
import com.alivc.live.pusher.AlivcVideoEncodeGopEnum

/**
 * 车机推流场景的推流参数工厂。
 *
 * 车机相比手机直播的差异：
 *  1. 屏幕基本是横屏固定方向 -> 使用横屏推流；
 *  2. 网络在行驶中波动剧烈（4G/5G 切换、隧道、桥洞）-> 开启码率/分辨率自适应 + 较大重连容忍度；
 *  3. 算力/散热有限 -> 优先硬编码，分辨率 540P 起步；
 *  4. 常驻后台 -> 配合前台 Service + 弱网/后台占位图，避免观众端黑屏。
 *
 * 说明：这里统一使用显式 setXxx() 调用（而非 Kotlin 属性赋值），
 * 因为部分配置项只有 setter 没有 getter，用属性语法无法编译。
 */
object CarPushConfigFactory {

    fun create(
        useFrontCamera: Boolean,
        audioOnly: Boolean,
        resolution: AlivcResolutionEnum = AlivcResolutionEnum.RESOLUTION_540P
    ): AlivcLivePushConfig {
        val config = AlivcLivePushConfig()

        // 基础推流模式：RTMP（rtmp://）或 RTS（artc://）
        config.setLivePushMode(AlivcLiveMode.AlivcLiveBasicMode)

        // 画面参数
        config.setResolution(resolution)
        config.setFps(AlivcFpsEnum.FPS_25)
        config.setVideoEncodeGop(AlivcVideoEncodeGopEnum.GOP_TWO)

        // 横屏推流（车机典型）：home 键在右侧的横屏；按车机实际安装方向选择 LEFT/RIGHT
        config.setPreviewOrientation(AlivcPreviewOrientationEnum.ORIENTATION_LANDSCAPE_HOME_RIGHT)

        // 编码：视频优先硬编（省电省 CPU），音频软编 + AAC-LC
        config.setVideoEncodeMode(AlivcEncodeModeEnum.Encode_MODE_HARD)
        config.setAudioEncodeMode(AlivcEncodeModeEnum.Encode_MODE_SOFT)
        config.setAudioProfile(AlivcAudioAACProfileEnum.AAC_LC)

        // 码控：开启码率控制 + 清晰度优先；行驶弱网时自动降码率/降分辨率保流畅
        config.setEnableBitrateControl(true)
        config.setQualityMode(AlivcQualityModeEnum.QM_RESOLUTION_FIRST)
        config.setEnableAutoResolution(true)

        // 预览显示：保持比例，避免车机异形屏拉伸变形
        config.setPreviewDisplayMode(AlivcPreviewDisplayMode.ALIVC_LIVE_PUSHER_PREVIEW_ASPECT_FIT)

        // 摄像头：车机按需选择某一路（默认后置/外部摄像头）
        config.setCameraType(
            if (useFrontCamera) AlivcLivePushCameraTypeEnum.CAMERA_TYPE_FRONT
            else AlivcLivePushCameraTypeEnum.CAMERA_TYPE_BACK
        )

        // 弱网时容忍更多重连，适配行车场景的频繁网络抖动
        config.setConnectRetryCount(MAX_RETRY_COUNT)
        config.setConnectRetryInterval(RETRY_INTERVAL_MS)

        // 纯音频回传场景（如车内对讲）
        config.setAudioOnly(audioOnly)

        return config
    }

    /** 弱网/网络切换重连次数（行车场景建议放大） */
    const val MAX_RETRY_COUNT = 5

    /** 每次重连间隔（毫秒） */
    const val RETRY_INTERVAL_MS = 2000
}
