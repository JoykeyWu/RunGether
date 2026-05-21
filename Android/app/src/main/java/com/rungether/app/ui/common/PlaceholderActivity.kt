package com.rungether.app.ui.common

import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.rungether.app.databinding.ActivityPlaceholderBinding

/**
 * 占位符页面基类
 *
 * 双端通用骨架：Toolbar + 居中标题 + 可动态扩展的导航按钮容器。
 * 子类通过 placeholderTitle 指定页面标题，并通过 placeholderNavItems 声明跳转入口。
 * 真实业务实现在「Android 页面开发计划」阶段交付，本类仅完成可启动、可路由可达验证。
 */
abstract class PlaceholderActivity : BaseActivity<ActivityPlaceholderBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityPlaceholderBinding =
        ActivityPlaceholderBinding.inflate(inflater)

    // 子类提供页面标题，可读取 Activity 的 label
    protected open val placeholderTitle: CharSequence
        get() = title ?: ""

    // 子类提供导航按钮：标题 + 目标 Activity
    protected open fun placeholderNavItems(): List<NavItem> = emptyList()

    override fun initView(savedInstanceState: android.os.Bundle?) {
        super.initView(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.title = placeholderTitle
        binding.tvPageTitle.text = placeholderTitle
        renderNavItems()
    }

    private fun renderNavItems() {
        val container = binding.llNavContainer
        container.removeAllViews()
        val items = placeholderNavItems()
        if (items.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        val padding = dpInt(12f)
        items.forEachIndexed { index, item ->
            val button = TextView(this).apply {
                text = item.title
                gravity = Gravity.CENTER
                textSize = 16f
                setPadding(padding, padding, padding, padding)
                setBackgroundResource(item.backgroundRes)
                setTextColor(ContextCompat.getColor(this@PlaceholderActivity, item.textColorRes))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(this@PlaceholderActivity, item.target)
                    item.extrasApplier?.invoke(intent)
                    startActivity(intent)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index != 0) params.topMargin = dpInt(12f)
            container.addView(button, params)
        }
    }
}

/**
 * 导航按钮项
 *
 * 描述一个占位符页面上的跳转入口：显示文案、目标 Activity、按钮背景与文字色。
 */
data class NavItem(
    val title: CharSequence,
    val target: Class<*>,
    val backgroundRes: Int,
    val textColorRes: Int,
    val extrasApplier: ((Intent) -> Unit)? = null
)
