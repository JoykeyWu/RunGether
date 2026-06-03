# RunGether · 助盲跑伴侣

一款连接 **视障跑者（盲人端）** 与 **陪跑员（引导端）** 的 Android 协同跑步 App。双端通过经典蓝牙（SPP）传输 JSON 指令：陪跑员端用 **方向摇杆 + 实时数据面板 + Canvas 轨迹图** 操作，盲人端以 **TTS 语音播报 + 分级震动** 接收指令。

- 仅使用 Android 原生系统能力（蓝牙 / GPS / TTS / 震动 / 加速度 / 手电筒），**零第三方 SDK、零 API Key**。
- 后端走公网 Mock：`https://6a0d90e6769682b8ee766f58.mockapi.io/api/v1/`，无鉴权可直接联通。

---

## 运行环境要求

| 项 | 要求 |
|---|---|
| 最低 Android 版本 | **Android 7.0（API 24）** |
| 目标 Android 版本 | Android 15（API 36） |
| 推荐设备 | 双手机均为 Android 8.0+，且支持经典蓝牙（SPP）与 GPS |
| 必须权限 | 蓝牙（SCAN / CONNECT）、定位（FINE_LOCATION）、相机（手电筒）、震动 |

## APK 下载

直接放在仓库 `/apk/` 目录，克隆后即可安装：

| 文件 | 用途 | 路径 |
|---|---|---|
| `app-debug.apk` | 调试包（作业要求） | [`/apk/app-debug.apk`](./apk/app-debug.apk) |
| `app-release.apk` | 发布包（推荐安装，体积更小） | [`/apk/app-release.apk`](./apk/app-release.apk) |

> debug 与 release 共用同一份 keystore，**可互相覆盖安装**。

---

## 核心功能截图

> 截图由测试同学使用真机录制后补充，建议四张：
>
> 1. 角色选择页（共用入口）
> 2. 陪跑员端主界面（方向摇杆 + Canvas 轨迹图）
> 3. 盲人端主界面（纯黑高对比 + 超大按钮）
> 4. 历史详情页（轨迹图 + 距离 / 配速 / 时长）

```
[截图 1] 角色选择        [截图 2] 陪跑员主界面
[截图 3] 盲人端主界面    [截图 4] 历史详情
```

---

## 仓库结构

```
RunGether/
├── apk/                           APK 安装包（debug + release 各一份）
├── ARCHITECTURE.md                架构说明（MVVM 分层、类职责清单）
├── 作业4-UI集成与核心流程-说明.md      大作业 4 验收对照与核心流程
├── Android/                       应用源码（Gradle 工程根，所有构建命令在此目录执行）
│   ├── app/
│   │   ├── src/main/java/com/rungether/app/
│   │   │   ├── ui/                视图层（按业务模块分包）
│   │   │   │   ├── role/          角色选择
│   │   │   │   ├── guide/         陪跑员主界面
│   │   │   │   ├── runner/        盲人端主界面
│   │   │   │   ├── bluetooth/     蓝牙配对
│   │   │   │   ├── summary/       跑步结算
│   │   │   │   ├── history/       历史列表 / 详情
│   │   │   │   ├── sos/           紧急求助 + 模拟呼叫
│   │   │   │   ├── settings/      设置
│   │   │   │   ├── contacts/      紧急联系人
│   │   │   │   ├── about/         关于
│   │   │   │   ├── common/        基类与扩展
│   │   │   │   └── widget/        自定义控件（方向摇杆、轨迹图）
│   │   │   ├── data/
│   │   │   │   ├── local/         Room（database / entity / dao）
│   │   │   │   ├── remote/        Retrofit（api / dto）
│   │   │   │   ├── repository/    ViewModel 唯一数据入口
│   │   │   │   └── prefs/         SharedPreferences 包装
│   │   │   ├── bluetooth/
│   │   │   │   ├── connection/    SPP 连接生命周期（搜索 / 配对 / 重连 / 断线告警）
│   │   │   │   └── protocol/      JSON 指令协议
│   │   │   ├── service/           系统能力封装
│   │   │   │   ├── tts/           语音播报
│   │   │   │   ├── vibration/     分级震动反馈
│   │   │   │   ├── location/      GPS（含冷启动 + 抖动滤波）
│   │   │   │   ├── sensor/        加速度（摇一摇）
│   │   │   │   ├── audio/         警报音
│   │   │   │   └── torch/         手电筒（SOS 闪烁）
│   │   │   ├── util/
│   │   │   └── constant/
│   │   ├── src/main/res/          布局 / 资源（含 values-night/ 盲人端高对比主题）
│   │   ├── src/test/              单元测试
│   │   ├── src/androidTest/       仪表测试
│   │   ├── rungether.keystore     签名证书
│   │   └── release/app-release.apk 预编译可直装 APK
│   ├── gradle/libs.versions.toml  版本目录（Version Catalog）
│   └── build.gradle.kts / settings.gradle.kts
└── 助盲跑伴侣App_需求确认文档.md      v6 需求基线（权威）
```

---

## 快速试跑

预编译 APK 已随仓库提交，路径见 [APK 下载](#apk-下载) 一节（`/apk/app-debug.apk` 或 `/apk/app-release.apk`）。

直接 `adb install` 或拷贝到手机点击安装即可。debug 与 release 共用同一份 keystore，**可覆盖安装**，不会出现签名冲突。

安装后流程：
1. 首次启动进入「角色选择」。两台手机分别选 **视障跑者** / **陪跑员**。
2. 任一端进入「蓝牙配对」搜索对端并配对，配对成功后双端自动建立 SPP 通道。
3. 陪跑员端按住方向摇杆给出指令，盲人端将听到 TTS 播报并感知到分级震动。

> 只有一台手机时可单端体验：盲人端 SPP 监听已开放，可用任意支持 SPP 的蓝牙调试 App 发送 JSON 指令验收交互。

---

## 本地构建

所有 Gradle 命令在 `Android/` 子目录执行。

```bash
cd Android

./gradlew assembleDebug                     # 构建 debug APK
./gradlew assembleRelease                   # 构建 release APK（自动签名）
./gradlew installDebug                      # 安装到已连接设备

./gradlew test                              # 全部单元测试
./gradlew connectedAndroidTest              # 仪表测试（需连真机 / 模拟器）

./gradlew lint                              # 静态检查，报告在 app/build/reports/
./gradlew clean
```

### 签名信息

`app/build.gradle.kts` 中 `signingConfigs.release` 已落地，**debug 与 release 共用同一份 keystore**，避免调试包与发布包签名冲突无法覆盖安装。

| 项 | 值 |
|---|---|
| 文件 | `Android/app/rungether.keystore` |
| alias | `123456` |

> ⚠️ **注意**：以上为开发/演示用签名，**生产环境请替换为正式证书并妥善保管密码**。该 keystore 仅用于作业交付与本地调试。

---

## 技术栈

| 类别 | 选型 | 版本 |
|---|---|---|
| 构建 | AGP / Gradle / Kotlin | 9.2.1 / 9.4.1 / 2.2.10 |
| 注解处理 | KSP | 2.3.6 |
| SDK | minSdk / targetSdk / compileSdk | 24 / 36 / 36（minor API 1） |
| Java | source / target | 11 / 11 |
| UI | XML + ViewBinding · Material Components | Material 1.12.0 |
| 架构 | ViewModel + LiveData + Repository（MVVM） | Lifecycle 2.8.7 |
| 本地库 | Room | 2.7.0 |
| 网络 | Retrofit + OkHttp + Gson | 2.11.0 / 4.12.0 |
| 异步 | Kotlin Coroutines | 1.9.0 |
| 系统能力 | 蓝牙 / GPS / TTS / 震动 / 加速度 / 手电筒 | Android 原生 |

---

## 架构关键约束

1. **双端共用一个 APK，按角色分流**：启动后由「角色选择」决定主题与导航。盲人端走 `res/values-night/` 纯黑高对比 + 超大按钮主题；陪跑员端走常规 Material 主题。两端共享 Room 与 Repository。
2. **Repository 是 ViewModel 唯一数据入口**：跑步记录读取策略 = 先读本地 Room → 异步请求 mockapi.io → 用响应更新本地 → LiveData 通知 UI。ViewModel 不允许直连 `data/remote/` 或 `data/local/`。
3. **蓝牙是双端协同生命线**：`bluetooth/connection/` 管 SPP 连接生命周期（搜索、配对、自动重连、断线 >10 秒触发 TTS 报警）；`bluetooth/protocol/` 定义 JSON 指令格式（方向角度、障碍 / 减速 / 停止、SOS 等）。UI 经 Repository 间接收发指令，**不允许直接持有 BluetoothSocket**。
4. **`service/` 封装所有系统能力**：TTS / Vibrator / LocationManager / SensorManager / CameraManager 全部封装在 `service/` 下，对 ViewModel 暴露协程或 LiveData 接口。震动反馈分级规则（直行不震 / 微调短震1 / 转弯短震2 / 大转弯短震3 / 障碍长震1 / 减速短震2 / 停止持续 3 秒）集中实现在 `service/vibration/`。
5. **自定义 Canvas 控件位于 `ui/widget/`**：方向摇杆（圆形 + 角度计算 + 自动回中）、轨迹图（纯 Canvas 路径绘制，**无街道底图**，需求强制要求）。
6. **统一 XML + ViewBinding**：构建已移除 Jetpack Compose 依赖，所有页面以 XML 布局 + ViewBinding 实现。

---

## 编码与注释规范

- 命名遵循 Kotlin 官方约定（PascalCase 类 / camelCase 函数变量 / SCREAMING_SNAKE_CASE 常量）。
- 资源 ID 三段式：`viewType_module_purpose`（例：`btn_runner_start`、`tv_guide_pace`）；布局文件前缀 `activity_` / `fragment_` / `item_` / `dialog_`。
- JSON 字段统一 `snake_case`，与 mockapi.io 返回保持一致。
- 注释仅写**类注释**与**方法注释**；方法注释一律**单行**（`// 描述`），不使用 `/** */` 块。
- 注释禁止任何 HTML 标签，禁止作者 / 日期 / 修改时间等元信息（Git 历史是唯一权威来源）。

---

## 权限说明

| 权限 | 用途 | 触发时机 |
|---|---|---|
| 蓝牙（`BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` / 经典蓝牙） | 双端 SPP 通信 | 进入「蓝牙配对」页 |
| 定位（`ACCESS_FINE_LOCATION`） | GPS 实时轨迹与配速 | 开始一次跑步前 |
| 相机（手电筒控制） | 紧急求助闪烁 | 仅在 SOS 触发时 |
| 震动 | 方向 / 障碍 / 停止反馈 | 收到对端指令时 |
| 麦克风 / 通讯录 / 短信 / 电话 | **不申请，不使用** | — |

---

## 需求基线

详见仓库根 [`助盲跑伴侣App_需求确认文档.md`](./助盲跑伴侣App_需求确认文档.md)（v6 已确认版本），实现细节与本文档保持一致。
