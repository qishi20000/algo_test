package com.example.carpusher

import android.app.Application
import com.example.carpusher.push.AlivcLicenseRegistrar

class CarPusherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 尽早注册 License，确保后续推流可用
        AlivcLicenseRegistrar.register(this)
    }
}
