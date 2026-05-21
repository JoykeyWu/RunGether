package com.rungether.app.util

import android.content.Context

/**
 * 版本信息工具
 *
 * 通过 PackageManager 读取应用版本号，避免散落构造 BuildConfig 依赖；
 * 设置页与关于页的版本号文案统一来源，保证与构建配置一致。
 */
object VersionInfo {

    // 读取应用 versionName；读取失败时回退为 1.0.0 避免 UI 空字符串
    fun versionName(context: Context): String {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName?.takeUnless { it.isBlank() } ?: FALLBACK
        }.getOrDefault(FALLBACK)
    }

    private const val FALLBACK = "1.0.0"
}
