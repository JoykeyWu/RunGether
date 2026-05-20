package com.rungether.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.rungether.app.bluetooth.protocol.DirectionType
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 方向摇杆控件
 *
 * 圆形摇杆 + 按压滑动 + 角度计算 + 松手自动回中；
 * 对外回调用户当前的方向角度与离散方向类型，由调用方决定是否发送指令。
 *
 * 不耦合任何蓝牙调用与震动反馈，确保控件可在 IDE 预览与单元测试中独立验证。
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFE7EBFF")
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = Color.parseColor("#FFA2ADFF")
    }
    private val stickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF4157FF")
    }
    private val stickStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.WHITE
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var stickRadius = 0f
    private var stickX = 0f
    private var stickY = 0f
    private var pressed = false

    var onDirectionChanged: ((DirectionType, Int) -> Unit)? = null
    var onReleased: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f - dp(4f)
        stickRadius = baseRadius * 0.4f
        stickX = centerX
        stickY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, borderPaint)
        canvas.drawCircle(stickX, stickY, stickRadius, stickPaint)
        canvas.drawCircle(stickX, stickY, stickRadius, stickStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressed = true
                moveStick(event.x, event.y)
                dispatchDirection()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressed = false
                resetToCenter()
                invalidate()
                onReleased?.invoke()
                onDirectionChanged?.invoke(DirectionType.STRAIGHT, 0)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // 设置摇杆中心位置，限制在底盘内
    private fun moveStick(targetX: Float, targetY: Float) {
        val dx = targetX - centerX
        val dy = targetY - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val maxOffset = baseRadius - stickRadius
        if (distance <= maxOffset) {
            stickX = targetX
            stickY = targetY
        } else {
            val ratio = maxOffset / distance
            stickX = centerX + dx * ratio
            stickY = centerY + dy * ratio
        }
    }

    private fun resetToCenter() {
        stickX = centerX
        stickY = centerY
    }

    private fun dispatchDirection() {
        if (!pressed) return
        val dx = stickX - centerX
        val dy = stickY - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val maxOffset = baseRadius - stickRadius
        val intensity = if (maxOffset <= 0f) 0f else distance / maxOffset

        val angleDeg = (Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toInt() + 360) % 360
        val signedAngle = if (angleDeg > 180) angleDeg - 360 else angleDeg
        val absAngle = if (signedAngle < 0) -signedAngle else signedAngle

        val direction = when {
            intensity < 0.15f -> DirectionType.STRAIGHT
            absAngle <= 15 -> DirectionType.STRAIGHT
            absAngle <= 30 -> DirectionType.MICRO
            absAngle <= 75 -> DirectionType.TURN
            else -> DirectionType.HARD_TURN
        }
        onDirectionChanged?.invoke(direction, signedAngle)
    }

    // 仅在控件内部使用：将 dp 换算为 px
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
