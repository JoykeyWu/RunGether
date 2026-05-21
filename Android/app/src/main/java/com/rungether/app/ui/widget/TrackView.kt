package com.rungether.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * 自定义轨迹图控件
 *
 * Canvas 网格底板 + 起点终点标记 + 路径绘制 + 自动跟随当前位置；
 * 强制不绘制真实街道底图，与需求文档「无街道底图」一致。
 *
 * 控件不持有定位 API，仅消费外部传入的相对坐标序列；
 * 调用方可以传入经纬度并由本控件按当前可视范围自动归一化展示。
 */
class TrackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFFFFF")
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = Color.parseColor("#1A4157FF")
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        color = Color.parseColor("#FF4157FF")
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val startPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF10B981")
    }
    private val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFEF4444")
    }
    private val markerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.WHITE
    }

    private val rawPoints = mutableListOf<DoubleArray>()
    private val mappedPath = Path()
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private val padding = dp(16f)

    // 仅用于盲人端主题切换，调用方可指定深色调色板
    fun applyDarkPalette() {
        backgroundPaint.color = Color.parseColor("#FF000000")
        gridPaint.color = Color.parseColor("#33FFFFFF")
        pathPaint.color = Color.parseColor("#FFFFFFFF")
        startPaint.color = Color.parseColor("#FF34D399")
        endPaint.color = Color.parseColor("#FFFF3B30")
        markerStroke.color = Color.parseColor("#FF000000")
        invalidate()
    }

    // 追加一个经纬度点；调用方传入原始 GPS 坐标即可
    fun addPoint(latitude: Double, longitude: Double) {
        rawPoints.add(doubleArrayOf(latitude, longitude))
        recomputePath()
        invalidate()
    }

    // 设置完整的轨迹点序列，用于历史详情页一次性加载
    fun setPoints(points: List<DoubleArray>) {
        rawPoints.clear()
        rawPoints.addAll(points)
        recomputePath()
        invalidate()
    }

    // 清空轨迹
    fun clear() {
        rawPoints.clear()
        mappedPath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawGrid(canvas)
        if (rawPoints.isEmpty()) return
        canvas.drawPath(mappedPath, pathPaint)
        canvas.drawCircle(startX, startY, dp(7f), startPaint)
        canvas.drawCircle(startX, startY, dp(7f), markerStroke)
        canvas.drawCircle(endX, endY, dp(7f), endPaint)
        canvas.drawCircle(endX, endY, dp(7f), markerStroke)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputePath()
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val step = dp(24f)
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun recomputePath() {
        mappedPath.reset()
        if (rawPoints.isEmpty() || width == 0 || height == 0) return

        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY
        for (point in rawPoints) {
            if (point[0] < minLat) minLat = point[0]
            if (point[0] > maxLat) maxLat = point[0]
            if (point[1] < minLon) minLon = point[1]
            if (point[1] > maxLon) maxLon = point[1]
        }
        val spanLat = (maxLat - minLat).takeIf { it > 0 } ?: 0.0001
        val spanLon = (maxLon - minLon).takeIf { it > 0 } ?: 0.0001

        val drawableWidth = width - padding * 2
        val drawableHeight = height - padding * 2

        for ((index, point) in rawPoints.withIndex()) {
            val lat = point[0]
            val lon = point[1]
            val ratioX = ((lon - minLon) / spanLon).toFloat()
            val ratioY = 1f - ((lat - minLat) / spanLat).toFloat()
            val mappedX = padding + ratioX * drawableWidth
            val mappedY = padding + ratioY * drawableHeight
            if (index == 0) {
                mappedPath.moveTo(mappedX, mappedY)
                startX = mappedX
                startY = mappedY
            } else {
                mappedPath.lineTo(mappedX, mappedY)
            }
            endX = mappedX
            endY = mappedY
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
