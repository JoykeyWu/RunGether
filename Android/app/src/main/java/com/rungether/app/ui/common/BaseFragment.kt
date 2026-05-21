package com.rungether.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 所有 Fragment 的基类
 *
 * 约束 ViewBinding 的生命周期严格绑定到视图，避免在 onDestroyView 之后
 * 仍持有失效引用导致内存泄漏；同时提供与 BaseActivity 一致的 Flow 收集能力。
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "BaseFragment binding accessed outside view lifecycle" }

    // 子类必须实现：返回当前页面的 ViewBinding
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initObserver()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 视图初始化钩子，子类按需重写
    protected open fun initView(savedInstanceState: Bundle?) {}

    // 数据观察初始化钩子，子类按需重写
    protected open fun initObserver() {}

    // 数据加载初始化钩子，子类按需重写
    protected open fun initData() {}

    // 在视图生命周期 STARTED 状态下安全收集 Flow
    protected fun <T> Flow<T>.collectOnViewStarted(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }
}
