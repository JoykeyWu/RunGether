package com.rungether.app.ui.runner

/**
 * 盲人端跑步阶段
 *
 * 与 UI 双态切换一一对应：
 * IDLE 显示「开始跑步」+「紧急求助」超大按钮，
 * RUNNING 显示「结束跑步」+「紧急求助」并展示已跑时长与距离。
 */
enum class RunnerRunPhase {
    IDLE,
    RUNNING
}
