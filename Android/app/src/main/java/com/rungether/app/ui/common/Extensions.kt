package com.rungether.app.ui.common

import android.content.Context
import android.util.TypedValue
import android.view.View

/**
 * 视图相关的通用 Kotlin 扩展
 *
 * 提供 dp 换算与显隐切换等常用便捷方法，避免布局/控件代码反复写样板。
 */

// 将 dp 数值换算为像素
fun Context.dp(value: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}

// 将 dp 数值换算为像素整型
fun Context.dpInt(value: Float): Int = dp(value).toInt()

// 完全隐藏视图（不占位）
fun View.gone() {
    if (visibility != View.GONE) visibility = View.GONE
}
