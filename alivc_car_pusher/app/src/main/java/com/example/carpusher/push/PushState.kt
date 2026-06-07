package com.example.carpusher.push

/**
 * 推流状态机。车机端 UI / 通知栏据此展示当前状态。
 */
enum class PushState {
    /** 初始，未初始化引擎 */
    IDLE,

    /** 已 init 并开始预览，未推流 */
    PREVIEWING,

    /** 正在连接推流服务器 */
    CONNECTING,

    /** 推流中 */
    PUSHING,

    /** 推流暂停（视频定格，音频可继续/静音） */
    PAUSED,

    /** 网络异常，SDK 正在自动重连 */
    RECONNECTING,

    /** 发生错误，等待人工/自动恢复 */
    ERROR;

    val isActive: Boolean
        get() = this == CONNECTING || this == PUSHING || this == PAUSED || this == RECONNECTING
}
