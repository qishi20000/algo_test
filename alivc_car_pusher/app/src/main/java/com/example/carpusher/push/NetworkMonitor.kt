package com.example.carpusher.push

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * 监听车机网络可用性 / 链路切换（Wi-Fi <-> 4G/5G）。
 *
 * 行车过程中网络抖动频繁，SDK 自身有自动重连，但当系统底层网络发生「切换」
 * （如驶出车库由 Wi-Fi 切到蜂窝）时，旧链路上的连接通常无法自愈，需要业务侧
 * 在新链路可用时主动触发 reconnect。
 */
class NetworkMonitor(
    context: Context,
    private val onAvailable: () -> Unit,
    private val onLost: () -> Unit
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "网络可用: $network")
            onAvailable()
        }

        override fun onLost(network: Network) {
            Log.w(TAG, "网络断开: $network")
            onLost()
        }
    }

    fun start() {
        if (registered) return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, callback) }
            .onSuccess { registered = true }
            .onFailure { Log.e(TAG, "注册网络回调失败", it) }
    }

    fun stop() {
        if (!registered) return
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        registered = false
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
