package com.rungether.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 所有 Activity 的基类
 *
 * 统一约束 ViewBinding 的初始化时机，提供 toast、协程作用域内安全收集 Flow
 * 等通用能力，避免每个子类重复样板代码。
 *
 * 子类需指定 ViewBinding 类型，并实现 inflateBinding 完成绑定。
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    // 子类必须实现：返回当前页面的 ViewBinding
    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        initView(savedInstanceState)
        initObserver()
        initData()
    }

    // 视图初始化钩子，子类按需重写
    protected open fun initView(savedInstanceState: Bundle?) {}

    // 数据观察初始化钩子，子类按需重写
    protected open fun initObserver() {}

    // 数据加载初始化钩子，子类按需重写
    protected open fun initData() {}

    // 统一的短 Toast 入口，避免散落构造
    protected fun toast(message: CharSequence) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 在 STARTED 状态下安全收集 Flow，进入后台自动暂停
    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }
}
