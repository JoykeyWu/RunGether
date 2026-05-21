package com.rungether.app.ui.common

import android.view.LayoutInflater
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import com.rungether.app.R

/**
 * 带 Toolbar 标准返回行为的 Activity 基类
 *
 * 占位符页面与设置类页面共用：自动启用 Home 返回箭头，并将点击映射到 onBackPressed
 * 子类需要在自身布局中放置一个 id 为 toolbar 的 androidx.appcompat.widget.Toolbar
 */
abstract class ToolbarActivity<VB : ViewBinding> : BaseActivity<VB>() {

    abstract override fun inflateBinding(inflater: LayoutInflater): VB

    override fun initView(savedInstanceState: android.os.Bundle?) {
        super.initView(savedInstanceState)
        val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }
}
