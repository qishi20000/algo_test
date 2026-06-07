package com.example.carpusher.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.carpusher.data.PushSettings
import com.example.carpusher.databinding.ActivityMainBinding
import com.example.carpusher.push.CarLivePushController
import com.example.carpusher.push.PushEvent
import com.example.carpusher.push.PushState
import com.example.carpusher.service.CarLivePushService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: PushSettings

    private var controller: CarLivePushController? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CarLivePushService.LocalBinder
            val ctrl = binder.service.controller
            controller = ctrl
            bound = true
            observeController(ctrl)
            // 服务连接后准备引擎并起预览
            ctrl.prepare(useFrontCamera = settings.useFrontCamera, audioOnly = settings.audioOnly)
            ctrl.startPreview(binding.surfacePreview)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            controller = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) {
            startAndBindService()
        } else {
            toast("缺少摄像头/麦克风权限，无法推流")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = PushSettings(this)

        binding.editUrl.setText(settings.pushUrl)

        binding.btnStart.setOnClickListener { onStartClicked() }
        binding.btnStop.setOnClickListener { controller?.stopPush() }
        binding.btnSwitchCamera.setOnClickListener { controller?.switchCamera() }

        ensurePermissionsThenBind()
    }

    private fun onStartClicked() {
        val url = binding.editUrl.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) {
            toast("请输入推流地址（rtmp:// 或 artc://）")
            return
        }
        settings.pushUrl = url
        // 拉起前台服务，保证后台/锁屏持续推流
        CarLivePushService.start(this)
        controller?.startPush(url)
    }

    private fun ensurePermissionsThenBind() {
        val perms = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = perms.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            startAndBindService()
        }
    }

    private fun startAndBindService() {
        val intent = Intent(this, CarLivePushService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun observeController(ctrl: CarLivePushController) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ctrl.state.collect { renderState(it) }
                }
                launch {
                    ctrl.events.collect { handleEvent(it) }
                }
            }
        }
    }

    private fun renderState(state: PushState) {
        binding.txtStatus.text = state.name
        binding.btnStart.isEnabled = !state.isActive
        binding.btnStop.isEnabled = state.isActive
    }

    private fun handleEvent(event: PushEvent) {
        when (event) {
            is PushEvent.Info -> toast(event.message)
            is PushEvent.NetworkPoor -> toast(event.message)
            is PushEvent.Error -> toast("错误[${event.code}]: ${event.message}")
            PushEvent.AuthExpired -> toast("推流鉴权过期，请更新推流地址后重试")
        }
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
