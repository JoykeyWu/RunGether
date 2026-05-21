package com.rungether.app.ui.guide

/**
 * 陪跑主界面运行阶段
 *
 * Idle 表示尚未开始陪跑，可见准备卡片与开始按钮；
 * Running 表示陪跑进行中，可见数据面板、轨迹与摇杆。
 */
enum class GuideRunPhase {
    IDLE,
    RUNNING
}
