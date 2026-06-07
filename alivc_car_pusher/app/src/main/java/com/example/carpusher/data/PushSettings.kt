package com.example.carpusher.data

import android.content.Context
import androidx.core.content.edit

/**
 * 车机推流配置的本地持久化。实际项目中推流地址通常由车云 AppServer 下发，
 * 这里用 SharedPreferences 做最简单的本地缓存 / 调试输入。
 */
class PushSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 推流地址。支持：
     *  - RTMP：rtmp://...
     *  - 超低延时 RTS：artc://...
     */
    var pushUrl: String
        get() = prefs.getString(KEY_PUSH_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PUSH_URL, value) }

    /** 是否使用前置摄像头（车机一般用面向驾驶员/外部的某一路，按需调整） */
    var useFrontCamera: Boolean
        get() = prefs.getBoolean(KEY_FRONT_CAMERA, false)
        set(value) = prefs.edit { putBoolean(KEY_FRONT_CAMERA, value) }

    /** 纯音频推流（如只做车内语音回传） */
    var audioOnly: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_ONLY, false)
        set(value) = prefs.edit { putBoolean(KEY_AUDIO_ONLY, value) }

    companion object {
        private const val PREF_NAME = "car_pusher_settings"
        private const val KEY_PUSH_URL = "push_url"
        private const val KEY_FRONT_CAMERA = "front_camera"
        private const val KEY_AUDIO_ONLY = "audio_only"
    }
}
