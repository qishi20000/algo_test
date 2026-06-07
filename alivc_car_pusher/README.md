# 车机推流服务（基于 AliVCSDK）

使用阿里云 **AliVCSDK（音视频终端一体化 SDK）** 的推流能力 `AlivcLivePusher`，为「车机（车载 Android 主机）」实现的一个可后台运行、支持自动重连的 **RTMP / RTS 推流服务** 参考工程。

> 适用场景：行车记录回传、车内监控直播、远程查看车况、车内对讲（纯音频）等。

---

## 1. 它解决了什么

车机推流和普通手机直播相比，有几个关键差异，本工程都做了针对性处理：

| 车机特点 | 工程对应处理 |
| --- | --- |
| 横屏固定方向 | 推流配置为横屏（`ORIENTATION_LANDSCAPE_HOME_RIGHT`） |
| 行驶中网络剧烈抖动（Wi-Fi/4G/5G 切换、隧道、桥洞） | 开启码率/分辨率自适应 + 加大重连次数 + `NetworkMonitor` 监听链路切换主动重连 |
| 常驻后台 / 锁屏仍需推流 | 用**前台 Service**（`CarLivePushService`）承载引擎，与界面解耦 |
| 算力/散热有限 | 视频优先**硬编码**，默认 540P 起步 |
| 弱网/后台黑屏体验差 | 预留弱网占位图、后台占位图接口 |

---

## 2. 工程结构

```
alivc_car_pusher/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # 权限、前台服务、License meta-data
│       └── java/com/example/carpusher/
│           ├── CarPusherApp.kt          # Application：尽早注册 License
│           ├── data/PushSettings.kt     # 推流地址等本地配置
│           ├── push/
│           │   ├── AlivcLicenseRegistrar.kt   # License 注册（AlivcLiveBase）
│           │   ├── CarPushConfigFactory.kt    # 车机场景推流参数
│           │   ├── CarLivePushController.kt   # 核心：封装 AlivcLivePusher + 状态机 + 监听器 + 自动重连
│           │   ├── NetworkMonitor.kt          # 网络链路切换监听
│           │   ├── PushState.kt / PushEvent.kt
│           ├── service/CarLivePushService.kt  # 前台推流服务（保活 + 通知 + Binder）
│           └── ui/MainActivity.kt             # 控制界面（预览 / 起停 / 切摄像头）
└── build.gradle / settings.gradle ...
```

核心调用链：

```
Application.onCreate
  └─ AlivcLicenseRegistrar.register()         // 注册 License

MainActivity
  ├─ 申请 摄像头/麦克风/通知 权限
  ├─ bindService(CarLivePushService)
  │     └─ controller.prepare() → startPreview(surfaceView)
  └─ 点击「开始推流」
        ├─ CarLivePushService.start()         // 拉起前台服务保活
        └─ controller.startPush("rtmp://..." 或 "artc://...")
              └─ onPreviewStarted → startPushAsync(url)
```

---

## 3. 接入步骤

### 3.1 申请 License（必须）

1. 登录阿里云控制台，开通 **视频直播 / 音视频终端 SDK**，创建应用并申请 **推流 SDK License**，得到 `LicenseKey`。
   参考官方文档：[集成推流 SDK License](https://help.aliyun.com/zh/live/developer-reference/integrate-push-sdk-for-android)。
2. 在 `~/.gradle/gradle.properties` 或工程根 `gradle.properties` 中填入（**不要硬编码进代码/不要入库**）：

```properties
alivcLicenseKey=你的_LicenseKey
# 若使用证书文件方式才需要，纯 LicenseKey 方式留空即可
alivcLicenseFile=
```

构建时会通过 `manifestPlaceholders` 注入到 `AndroidManifest.xml` 的 meta-data，SDK 自动读取。

> License 校验是**异步**的，并且**只在实例化 `AlivcLivePusher` 之后才回调** `onLicenceCheck`，结果见 `AlivcLicenseRegistrar.isLicenseValid` 与日志 TAG `AlivcLicense`。

### 3.2 依赖

`app/build.gradle` 默认使用一体化 SDK：

```groovy
implementation "com.aliyun.sdk.android:AliVCSDK_InteractiveLive:7.11.0"
// 仅需 RTMP/RTS 单向推流、要更小包体，可改用基础版（API 一致）：
// implementation "com.alivc.pusher:AlivcLivePusher:7.11.0"
```

仓库地址（阿里云 Maven）已在 `settings.gradle` 配置。请到「阿里云控制台 > SDK 下载」核对**最新版本号**。

### 3.3 推流地址

- RTMP：`rtmp://your-domain/app/stream?auth_key=...`
- RTS 超低延时：`artc://your-domain/app/stream?auth_key=...`

地址通常由车云 `AppServer` 下发（含鉴权 `auth_key`）。本工程在界面输入框里临时填写，便于调试；正式环境请改为从服务端拉取。生成规则见官方文档「生成推流地址」。

---

## 4. 构建运行

本目录是独立的 Android 工程，需用 **Android Studio** 打开 `alivc_car_pusher/`（或命令行 Gradle）。

> 注意：仓库未提交 Gradle Wrapper 的二进制 `gradle-wrapper.jar`。首次请在该目录执行 `gradle wrapper`（或用 Android Studio 自动生成）后再 `./gradlew assembleDebug`。

```bash
cd alivc_car_pusher
gradle wrapper        # 生成 wrapper（仅首次）
./gradlew assembleDebug
```

---

## 5. 关键实现说明

### License 注册（`AlivcLicenseRegistrar`）
```kotlin
AlivcLiveBase.setListener(object : AlivcLiveBaseListener {
    override fun onLicenceCheck(result, reason) { /* 异步校验结果 */ }
})
AlivcLiveBase.registerSDK()
```

### 推流参数（`CarPushConfigFactory`）
横屏、硬编、AAC-LC、码率/分辨率自适应、加大重连容忍度，纯音频可选。

### 状态机（`PushState`）
`IDLE → PREVIEWING → CONNECTING → PUSHING`，异常进入 `RECONNECTING / ERROR`，通过 `StateFlow` 暴露给 UI 与通知栏。

### 自动重连（两层）
1. **SDK 内置**：`connectRetryCount/Interval` 范围内自动重连（`onReconnectStart/Succeed/Fail`）。
2. **业务兜底**：超过 SDK 上限后，`NetworkMonitor` 检测到新链路可用时调用 `reconnectPushAsync(url)` 续推——这是行车「网络切换」场景的关键。

### 前台服务（`CarLivePushService`）
`foregroundServiceType="camera|microphone"`，常驻通知展示推流状态，锁屏/退后台持续推流；通过 `LocalBinder` 把控制器暴露给界面。

---

## 6. 车机落地注意事项

- **横竖屏**：推流方向只能在推流前设置，过程中不可切换；并把 Activity 设为不随系统旋转（已设 `screenOrientation="landscape"`）。
- **硬编分辨率为 16 倍数**：如 540P 实际输出 544×960，播放端按输出分辨率等比缩放，避免黑边。
- **多摄像头车机**：系统可能有多路摄像头（DMS/前视/环视）。`AlivcLivePusher` 默认走系统 Camera 的前/后置；接外部 USB/特定通路摄像头时，建议改用**外部视频源**（`setExternMainStream` + `inputStreamVideoData`）自行送帧。
- **后台保活**：不同车机厂商（高通/AutoChips 等定制 Android）的后台策略不同，可能需要在系统层加白名单或常驻应用配置。
- **权限**：CAMERA / RECORD_AUDIO 为运行时权限；Android 14 起前台服务需 `FOREGROUND_SERVICE_CAMERA/MICROPHONE`。来电自动静音需 `READ_PHONE_STATE`。
- **本工程未集成美颜**（车机一般不需要），如需可按官方文档接入 Queen 美颜库。

---

## 7. 参考

- 推流 SDK 概览：https://help.aliyun.com/zh/live/developer-reference/push-sdk-overview
- 集成 Android 推流 SDK：https://help.aliyun.com/zh/live/developer-reference/integrate-push-sdk-for-android
- Android 推流 SDK 功能说明：https://help.aliyun.com/zh/live/developer-reference/use-push-sdk-for-android
- SDK 下载与版本：https://help.aliyun.com/zh/live/developer-reference/sdk-download-and-release-notes
