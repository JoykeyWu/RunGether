# 架构说明 · ARCHITECTURE

> 项目：**RunGether · 助盲跑伴侣**
> 架构模式：**MVVM**（ViewModel + LiveData + Repository）

---

## 一、总体分层

```
┌─────────────────────────────────────────────────────────┐
│  UI 层 (Activity / Fragment / RecyclerView Adapter)     │
│  - 只持有 ViewBinding 与 ViewModel 引用                  │
│  - 通过 viewModel.xxx.observe(this) { ... } 渲染状态     │
└──────────────────────────┬──────────────────────────────┘
                           │ observe LiveData
┌──────────────────────────┴──────────────────────────────┐
│  ViewModel 层 (com.rungether.app.ui.<module>.*ViewModel) │
│  - 持有业务状态 (MutableLiveData)                        │
│  - 仅依赖 Repository，不直接调用 Dao / Api               │
│  - 协程作用域：viewModelScope                            │
└──────────────────────────┬──────────────────────────────┘
                           │ suspend fun call
┌──────────────────────────┴──────────────────────────────┐
│  Repository 层 (data/repository/*Repository.kt)         │
│  - ViewModel 唯一数据入口                                │
│  - 本地优先策略：Room → mockapi.io → 写回 Room → 通知    │
└────────────┬──────────────────────────┬─────────────────┘
             │                          │
┌────────────┴─────────────┐   ┌────────┴────────────────┐
│ data/local (Room)        │   │ data/remote (Retrofit)  │
│ - database / entity / dao│   │ - api / dto             │
└──────────────────────────┘   └─────────────────────────┘

         ┌────────────────────────────────────┐
         │  横切：系统能力封装  service/       │
         │  tts / vibration / location /       │
         │  sensor / audio / torch             │
         └────────────────────────────────────┘

         ┌────────────────────────────────────┐
         │  横切：双端通信  bluetooth/         │
         │  connection (SPP 生命周期)          │
         │  protocol   (JSON 指令协议)          │
         └────────────────────────────────────┘
```

---

## 二、关键约束

1. **单向依赖**：UI → ViewModel → Repository → (Room / Retrofit)。下层不感知上层。
2. **Repository 是 ViewModel 唯一数据入口**：禁止 ViewModel 直接持有 `Dao` 或 `ApiService`。
3. **读取策略写死**：先读本地 Room → 异步请求 mockapi.io → 用响应更新本地 → LiveData 通知 UI（避免离线场景白屏）。
4. **系统能力封装在 `service/`**：TTS / Vibrator / LocationManager / SensorManager / CameraManager 全部集中封装，对 ViewModel 暴露协程或 LiveData 接口。
5. **蓝牙不允许漏出底层 Socket**：UI 经 Repository 间接收发指令，**任何 Activity 都不允许直接持有 `BluetoothSocket`**。
6. **统一 XML + ViewBinding**：构建中已移除 Jetpack Compose，所有页面都是 XML 布局 + ViewBinding。

---

## 三、主要类职责

### 3.1 UI 层

| 包 | 关键类 | 职责 |
|---|---|---|
| `ui/role` | `RoleSelectActivity` | 角色选择（首启动入口），将角色身份持久化到 SharedPreferences |
| `ui/guide` | `GuideMainActivity` / `GuideMainViewModel` | 陪跑员主界面：方向摇杆、实时数据面板、Canvas 轨迹图 |
| `ui/runner` | `RunnerMainActivity` / `RunnerMainViewModel` | 盲人端主界面：超大按钮 + TTS 播报 + 分级震动反馈 |
| `ui/bluetooth` | `BluetoothPairActivity` | 蓝牙搜索 / 配对 / 已配对设备列表 |
| `ui/summary` | `GuideSummaryActivity` / `RunnerSummaryActivity` | 跑步结算页：距离、时长、配速、路线缩略 |
| `ui/history` | `*HistoryListActivity` / `*HistoryDetailActivity` / `*HistoryAdapter` | 历史记录列表 + 详情；`RecyclerView + ListAdapter + DiffUtil` 卡片布局 |
| `ui/sos` | `RunnerSosActivity` / `RunnerSosViewModel` / `RunnerCallActivity` | 紧急求助列表 + 模拟呼叫页 |
| `ui/settings` | `GuideSettingsActivity` / `RunnerSettingsActivity` | 设置入口：切换角色、TTS 测试、关于、紧急联系人管理 |
| `ui/contacts` | `*ContactsActivity` / `*ContactAdapter` | 紧急联系人 CRUD（本地 Room） |
| `ui/about` | `GuideAboutActivity` / `RunnerAboutActivity` | 关于页 |
| `ui/common` | 基类 / 扩展函数 | 沉浸式状态栏、权限弹窗封装 |
| `ui/widget` | `JoystickView` / `TrackView` | 自定义 Canvas 控件：方向摇杆（角度计算 + 自动回中）、轨迹图（纯 Canvas 路径绘制，无街道底图） |

### 3.2 ViewModel 层

| 类 | 暴露 LiveData |
|---|---|
| `GuideMainViewModel` | 连接状态、跑步状态、实时配速 / 距离 / 时长、轨迹点列表 |
| `RunnerMainViewModel` | 连接状态、当前指令、电池电量、TTS 是否启用 |
| `RunnerSosViewModel` | 联系人列表、呼叫状态 |

### 3.3 Repository 层

| 类 | 数据源 | 职责 |
|---|---|---|
| `RunRecordRepository` | Room (`RunRecordDao`) + Retrofit (`RunRecordApiService`) | 跑步记录读写、远端同步 |
| `EmergencyContactRepository` | Room (`EmergencyContactDao`) | 紧急联系人 CRUD（本地） |
| `UserRepository` | SharedPreferences (`UserPreferences`) | 角色身份、用户偏好 |

### 3.4 数据层

| 包 | 类型 | 主要内容 |
|---|---|---|
| `data/local/database` | Room | `AppDatabase`（聚合所有 Dao） |
| `data/local/entity` | 实体 | `RunRecordEntity` / `EmergencyContactEntity` |
| `data/local/dao` | DAO | `RunRecordDao` / `EmergencyContactDao` |
| `data/remote/api` | Retrofit 接口 | `RunRecordApiService` |
| `data/remote/dto` | DTO | mockapi.io 返回字段（snake_case） |
| `data/prefs` | SharedPreferences 包装 | `UserPreferences` |

### 3.5 系统能力 service/

| 包 | 类 | 职责 |
|---|---|---|
| `service/tts` | `TtsService` | 文本转语音播报，复用同一 `TextToSpeech` 实例 |
| `service/vibration` | `VibrationService` | 分级震动：微调短震1 / 转弯短震2 / 大转弯短震3 / 障碍长震1 / 减速短震2 / 停止持续 3 秒 |
| `service/location` | `LocationService` | GPS 实时定位，含冷启动等待与抖动滤波 |
| `service/sensor` | `ShakeDetector` | 加速度传感器 → 摇一摇手势识别 |
| `service/audio` | `AlertSoundPlayer` | 警报音播放（`res/raw/`） |
| `service/torch` | `TorchService` | 手电筒控制，SOS 触发时闪烁 |

### 3.6 蓝牙 bluetooth/

| 包 | 类 | 职责 |
|---|---|---|
| `bluetooth/connection` | `BluetoothConnectionManager` | SPP 搜索 / 配对 / 自动重连 / 断线 >10 秒触发 TTS 报警；以 LiveData 暴露连接状态 |
| `bluetooth/protocol` | `BluetoothCommand` / `CommandCodec` | JSON 指令编解码（方向角度、障碍 / 减速 / 停止、SOS） |

---

## 四、生命周期与协程

- **ViewModel 协程**：统一使用 `viewModelScope.launch { ... }`，随 ViewModel 销毁自动取消。
- **Repository 协程**：所有 IO 操作均为 `suspend fun`，由 ViewModel 在 `viewModelScope` 中调用，避免阻塞主线程。
- **Activity 协程**：仅用于 UI 事件流（如倒计时），不发起业务请求。
- **蓝牙读循环**：在 `BluetoothConnectionManager` 内自维护 `Job`，连接生命周期由该类负责。

---

## 五、为什么选 MVVM 而非 MVP / MVI

- **MVVM**：ViewModel + LiveData 是 AndroidX Lifecycle 官方推荐，能自动响应配置变更（旋转 / 切换主题）；学习曲线低，与 Room / Retrofit 协程整合自然。
- **MVP**：Presenter 与 View 1:1 接口绑定，在双端 UI 差异较大的本项目里会写出大量重复 Contract 接口。
- **MVI**：单向数据流更适合复杂状态机，但本项目状态相对简单（连接 / 未连接 / 跑步中 / 结算中），引入 Intent / Reducer 反而增加心智成本。

最终在简洁性、可测试性、与 Jetpack 生态契合度三者间，**MVVM** 是最优选择。
