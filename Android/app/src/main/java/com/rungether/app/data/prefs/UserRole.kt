package com.rungether.app.data.prefs

/**
 * 用户角色枚举
 *
 * 表达「视障跑者」、「陪跑员」与「未选择」三种业务态；
 * 未选择状态下应当强制跳转角色选择页，由用户主动确认。
 */
enum class UserRole {
    UNSELECTED,
    GUIDE,
    RUNNER
}
