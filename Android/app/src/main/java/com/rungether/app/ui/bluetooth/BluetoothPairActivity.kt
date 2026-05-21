package com.rungether.app.ui.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.rungether.app.R
import com.rungether.app.bluetooth.BluetoothModule
import com.rungether.app.bluetooth.connection.BluetoothConnectionManager
import com.rungether.app.bluetooth.connection.ConnectionState
import com.rungether.app.bluetooth.connection.DiscoveredDevice
import com.rungether.app.data.prefs.UserPreferences
import com.rungether.app.databinding.ActivityBluetoothPairBinding
import com.rungether.app.databinding.ItemBluetoothBondedBinding
import com.rungether.app.databinding.ItemBluetoothDeviceBinding
import com.rungether.app.ui.common.BaseActivity

/**
 * 陪跑员蓝牙配对页
 *
 * 顶部固定标题栏 + 搜索状态条；中部已配对设备卡片与附近设备列表；
 * 已配对列表读取 BluetoothAdapter.bondedDevices，附近设备由 BluetoothConnectionManager
 * 的 startDiscovery 推送，按 RSSI 信号强中弱三档展示。
 *
 * 完成搜索 → 选择 → 配对 → 建链流程后通知主界面经状态 Flow 自动刷新。
 */
class BluetoothPairActivity : BaseActivity<ActivityBluetoothPairBinding>() {

    private val connectionManager: BluetoothConnectionManager by lazy {
        BluetoothModule.connectionManager(applicationContext)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            startScan()
        } else {
            toast("缺少蓝牙或定位权限，无法搜索附近设备")
            renderScanStatus(scanning = false, granted = false)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityBluetoothPairBinding =
        ActivityBluetoothPairBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnRescan.setOnClickListener { ensurePermissionThenScan() }
    }

    override fun initObserver() {
        super.initObserver()
        connectionManager.state.collectOnStarted { state -> renderBondedList(state) }
        connectionManager.discovering.collectOnStarted { scanning ->
            renderScanStatus(scanning = scanning, granted = true)
        }
        connectionManager.discoveredDevices.collectOnStarted { devices ->
            renderDiscoveredList(devices)
        }
    }

    override fun initData() {
        super.initData()
        renderBondedList(connectionManager.state.value)
        ensurePermissionThenScan()
    }

    override fun onDestroy() {
        connectionManager.stopDiscovery()
        super.onDestroy()
    }

    private fun ensurePermissionThenScan() {
        val needed = requiredPermissions()
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startScan() {
        if (!connectionManager.isBluetoothSupported()) {
            toast("当前设备不支持蓝牙")
            renderScanStatus(scanning = false, granted = true)
            return
        }
        if (!connectionManager.isBluetoothEnabled()) {
            toast("请先开启蓝牙再搜索附近设备")
            renderScanStatus(scanning = false, granted = true)
            return
        }
        connectionManager.startDiscovery()
    }

    private fun renderScanStatus(scanning: Boolean, granted: Boolean) {
        binding.tvScanStatus.text = when {
            !granted -> "权限未授予"
            scanning -> "正在搜索附近设备…"
            else -> "搜索已结束，可下拉重试"
        }
    }

    @SuppressLint("MissingPermission")
    private fun renderBondedList(state: ConnectionState) {
        val container = binding.llBondedContainer
        container.removeAllViews()
        val bonded = runCatching { connectionManager.bondedDevices() }.getOrDefault(emptyList())
        if (bonded.isEmpty()) {
            binding.tvBondedEmpty.visibility = View.VISIBLE
            return
        }
        binding.tvBondedEmpty.visibility = View.GONE
        val connectedAddress = (state as? ConnectionState.Connected)?.deviceAddress
        val connectingAddress = (state as? ConnectionState.Connecting)?.deviceAddress
        bonded.forEachIndexed { index, device ->
            val itemBinding = ItemBluetoothBondedBinding.inflate(layoutInflater, container, false)
            itemBinding.tvBondedName.text = device.name ?: "未命名设备"
            itemBinding.tvBondedMac.text = "MAC: ${maskMac(device.address)}"
            val isConnected = connectedAddress == device.address
            val isConnecting = connectingAddress == device.address
            itemBinding.btnBondedAction.text = when {
                isConnected -> "已连接"
                isConnecting -> "连接中"
                else -> "连接"
            }
            itemBinding.btnBondedAction.setOnClickListener {
                if (!isConnected && !isConnecting) connectionManager.connect(device)
            }
            if (index > 0) {
                itemBinding.root.translationY = 0f
                itemBinding.root.alpha = 1f
                val params = (itemBinding.root.layoutParams as? android.view.ViewGroup.MarginLayoutParams)
                params?.topMargin = resources.getDimensionPixelSize(R.dimen.space_sm)
                itemBinding.root.layoutParams = params
            }
            container.addView(itemBinding.root)
        }
    }

    @SuppressLint("MissingPermission")
    private fun renderDiscoveredList(devices: List<DiscoveredDevice>) {
        val container = binding.llDiscoveredContainer
        container.removeAllViews()
        if (devices.isEmpty()) {
            val placeholder = ItemBluetoothDeviceBinding.inflate(layoutInflater, container, false)
            placeholder.tvDeviceName.text = "暂无设备"
            placeholder.tvSignal.text = "等待搜索结果"
            placeholder.btnConnect.visibility = View.GONE
            container.addView(placeholder.root)
            return
        }
        devices.forEachIndexed { index, item ->
            val itemBinding = ItemBluetoothDeviceBinding.inflate(layoutInflater, container, false)
            itemBinding.tvDeviceName.text = item.name?.takeIf { it.isNotBlank() } ?: "未命名设备"
            val (label, color) = signalDescription(item.signalLevel)
            itemBinding.tvSignal.text = label
            itemBinding.ivSignal.setColorFilter(ContextCompat.getColor(this, color))
            itemBinding.btnConnect.setOnClickListener { connectAndRemember(item) }
            if (index > 0) {
                val divider = View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(this@BluetoothPairActivity, R.color.guide_divider))
                }
                container.addView(
                    divider,
                    android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                    )
                )
            }
            container.addView(itemBinding.root)
        }
    }

    private fun signalDescription(level: DiscoveredDevice.SignalLevel): Pair<String, Int> = when (level) {
        DiscoveredDevice.SignalLevel.STRONG -> "信号强" to R.color.guide_success
        DiscoveredDevice.SignalLevel.MEDIUM -> "信号中" to R.color.guide_warning
        DiscoveredDevice.SignalLevel.WEAK -> "信号弱" to R.color.guide_navy_45
    }

    private fun connectAndRemember(item: DiscoveredDevice) {
        UserPreferences.from(applicationContext).lastPairedMac = item.address
        connectionManager.connect(item.device)
    }

    private fun maskMac(address: String): String {
        if (address.length < 11) return address
        return address.replaceRange(9, address.length - 2, "**:**")
    }

    private fun requiredPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_SCAN
            list += Manifest.permission.BLUETOOTH_CONNECT
        }
        list += Manifest.permission.ACCESS_FINE_LOCATION
        list += Manifest.permission.ACCESS_COARSE_LOCATION
        return list
    }
}
