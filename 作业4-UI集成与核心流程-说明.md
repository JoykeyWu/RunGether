# 大作业 4 · UI 集成 + 核心流程

> 项目：**RunGether · 助盲跑伴侣**

---

## 一、作业要求与本项目实现对照

| # | 作业要求 | 本项目对应实现 | 关键文件 | 状态 |
|---|---|---|---|---|
| 1 | **实现主界面**：至少一个列表 / 网格 / 卡片布局，用于展示数据 | 双端各实现「历史记录列表」与「紧急联系人列表」，盲人端再额外增加「SOS 联系人列表」；统一使用 `RecyclerView + ListAdapter + DiffUtil`，每条 item 为独立卡片布局 | `ui/history/GuideHistoryAdapter.kt`<br>`ui/history/RunnerHistoryAdapter.kt`<br>`ui/contacts/GuideContactAdapter.kt`<br>`ui/contacts/RunnerContactAdapter.kt`<br>`ui/sos/RunnerSosContactAdapter.kt`<br>`res/layout/item_*.xml` | ✅ |
| 2 | **实现详情界面**：点击列表项跳转到详情页面 | 历史记录列表点击任意一项 → 跳转到对应详情页，携带记录 ID，详情页展示轨迹图 + 距离 / 配速 / 时长 / 步数等数据 | `ui/history/GuideHistoryListActivity.kt` → `GuideHistoryDetailActivity.kt`<br>`ui/history/RunnerHistoryListActivity.kt` → `RunnerHistoryDetailActivity.kt` | ✅ |
| 3 | **实现数据绑定**：通过 ViewModel + LiveData/StateFlow 将 Repository 中的数据展示到 UI | 严格 MVVM：Repository → ViewModel → LiveData → Activity `observe()`。各业务模块均有独立 ViewModel | `ui/guide/GuideMainViewModel.kt`<br>`ui/runner/RunnerMainViewModel.kt`<br>`ui/sos/RunnerSosViewModel.kt`<br>`data/repository/*` | ✅ |
| 4 | **确保无崩溃**：App 在模拟器 / 真机上可以打开，点击所有按钮不闪退 | 已出 release 包并通过真机走查；启动 → 角色选择 → 主界面 → 各模块按钮均可正常进入与返回 | `Android/app/release/app-release.apk` | ✅ |

---

## 二、核心流程描述（对应作业提交物 2）

> **打开 App → 角色选择 → 主界面 → 历史列表 → 历史详情**

详细步骤：

1. **打开 App**：进入「角色选择」页，提供「视障跑者 / 陪跑员」两个超大按钮入口。
2. **选择角色**：选择后角色身份持久化到 `SharedPreferences`，进入对应主界面。
   - 陪跑员端：`GuideMainActivity`（方向摇杆 + 实时数据面板 + Canvas 轨迹图）
   - 盲人端：`RunnerMainActivity`（纯黑高对比 + 超大按钮 + TTS 播报）
3. **进入历史列表**：点击主界面右上角「历史」按钮，进入历史记录列表页（`RecyclerView` 卡片布局展示距离、时长、配速、日期）。列表数据由 `ViewModel` 通过 `LiveData` 从 `Repository` 加载（**先读本地 Room → 异步请求 mockapi.io → 用响应更新本地 → LiveData 通知 UI**）。
4. **点击列表项**：点击任意一条历史记录 → `Intent` 携带 `recordId` 跳转到详情页 `HistoryDetailActivity`。
5. **查看详情**：详情页通过 `ViewModel.loadById(recordId)` 触发 Repository 查询，结果以 `LiveData` 推送给 UI，绘制轨迹图与各项指标。

---

## 三、数据绑定细节（MVVM 链路示意）

```
mockapi.io (远端)
       │
       ▼
data/remote/api/*ApiService.kt          Retrofit 接口
       │
       ▼
data/repository/*Repository.kt          唯一数据入口
       │  本地优先：Room → 远端兜底 → 写回 Room
       ▼
ui/<module>/<Module>ViewModel.kt        持有 LiveData<UiState>
       │
       │  Activity.lifecycleScope / viewModel.xxx.observe(this) { ... }
       ▼
ui/<module>/<Module>Activity.kt         ViewBinding 渲染
```

约束（已在代码中强制落地）：
- `ViewModel` 仅依赖 `Repository`，**不直接持有** `Dao` / `ApiService`；
- UI 层只 `observe` LiveData，**不持有业务状态**；
- 蓝牙、TTS、震动、定位等系统能力封装在 `service/` 包，对 ViewModel 暴露协程或 LiveData 接口。

---

## 四、列表 / 详情页清单（验收锚点）

| 列表页 | 详情页 | 入口位置 |
|---|---|---|
| `GuideHistoryListActivity`（陪跑员端历史列表） | `GuideHistoryDetailActivity` | 陪跑员主界面右上角「历史」 |
| `RunnerHistoryListActivity`（盲人端历史列表） | `RunnerHistoryDetailActivity` | 盲人端主界面「历史记录」按钮 |
| `GuideContactsActivity`（陪跑员紧急联系人列表） | 行内编辑对话框 | 陪跑员设置页 |
| `RunnerContactsActivity`（盲人端紧急联系人列表） | 行内编辑对话框 | 盲人端设置页 |
| `RunnerSosActivity`（SOS 联系人选择列表） | 跳转「模拟呼叫页」`RunnerCallActivity` | 盲人端紧急求助按钮 |

> 满足作业要求的"列表 → 详情"主线为：**历史列表 → 历史详情**（双端各一套）。其余列表为附加项，可一并展示。

---

## 五、运行环境与提交物

### 提交物 1：运行截图 / 录屏

> **此项需现场录制**。建议按以下顺序录一段 30~60 秒视频或截图 5 张：
>
> 1. 启动页 → 角色选择
> 2. 进入主界面（陪跑员端 / 盲人端任选其一）
> 3. 历史记录列表（`RecyclerView` 卡片）
> 4. 点击列表项 → 历史详情（轨迹图 + 数据）
> 5. 返回主界面，再点击「设置 / 联系人 / 关于」证明不闪退

### 提交物 2：核心流程文字说明

见本文档「二、核心流程描述」一节，可直接复制作为作业附文。

### 直装 APK

```
Android/app/release/app-release.apk
```

debug 与 release 共用同一份 keystore（store / key 密码 `123456`，alias `123456`），可覆盖安装、可直接联机调试。

---

## 六、源码导览（评审用）

| 评审关注点 | 推荐查看 |
|---|---|
| 列表布局与 Adapter | `app/src/main/java/com/rungether/app/ui/history/` |
| 列表 → 详情跳转 | `GuideHistoryListActivity.kt` 第 66 行附近 `startActivity(Intent(...))`<br>`RunnerHistoryListActivity.kt` 第 104 行附近 `startActivity(Intent(...))` |
| ViewModel + LiveData | `ui/guide/GuideMainViewModel.kt` / `ui/runner/RunnerMainViewModel.kt` |
| Repository 本地优先策略 | `data/repository/` 各 Repository 类 |
| 卡片 item 布局 | `res/layout/item_guide_history.xml` / `item_runner_history.xml` |
