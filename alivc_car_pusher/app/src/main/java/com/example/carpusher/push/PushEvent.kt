package com.example.carpusher.push

/**
 * 一次性事件（吐司 / 日志用），区别于持续的 [PushState]。
 */
sealed class PushEvent {
    data class Info(val message: String) : PushEvent()
    data class NetworkPoor(val message: String) : PushEvent()
    data class Error(val code: String, val message: String) : PushEvent()
    /** 推流鉴权过期，需要业务侧下发新的推流地址 */
    object AuthExpired : PushEvent()
}
