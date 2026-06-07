package com.example.carpusher.push

import android.content.Context
import android.util.Log
import com.alivc.live.pusher.AlivcLiveBase
import com.alivc.live.pusher.AlivcLiveBaseListener
import com.alivc.live.pusher.AlivcLivePushConstants

/**
 * AliVCSDK License 注册。
 *
 * 注意：
 *  - 必须在创建/使用 [com.alivc.live.pusher.AlivcLivePusher] 之前调用 [register]，建议放在 Application.onCreate。
 *  - LicenseKey 通过 AndroidManifest 的 meta-data 配置（见 build.gradle 的 manifestPlaceholders），
 *    SDK 会自动读取，无需在代码里再次传入。
 *  - [AlivcLiveBaseListener.onLicenceCheck] 是异步回调，仅在「实例化 pusher 之后」才会触发。
 */
object AlivcLicenseRegistrar {

    private const val TAG = "AlivcLicense"

    @Volatile
    var isLicenseValid: Boolean = false
        private set

    @Volatile
    private var registered = false

    fun register(context: Context) {
        if (registered) return
        registered = true

        AlivcLiveBase.setListener(object : AlivcLiveBaseListener {
            override fun onLicenceCheck(
                result: AlivcLivePushConstants.AlivcLiveLicenseCheckResultCode?,
                reason: String?
            ) {
                isLicenseValid =
                    result == AlivcLivePushConstants.AlivcLiveLicenseCheckResultCode.AlivcLiveLicenseCheckResultCodeSuccess
                if (isLicenseValid) {
                    Log.i(TAG, "License 校验成功")
                } else {
                    Log.e(TAG, "License 校验失败: result=$result, reason=$reason")
                }
            }
        })

        // 触发注册；LicenseKey 由 SDK 从 Manifest meta-data 自动读取
        AlivcLiveBase.registerSDK()
        Log.i(TAG, "已调用 registerSDK，等待异步校验结果（实例化 pusher 后回调）")
    }
}
