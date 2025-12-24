package com.example.flowalbum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 自定义指示器视图
 * 用于显示当前图片位置和总数
 */
class IndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 圆点半径
    private var dotRadius = 8f
    
    // 圆点间距
    private var dotSpacing = 24f
    
    // 活跃圆点画笔
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.indicator_active)
        style = Paint.Style.FILL
    }
    
    // 非活跃圆点画笔
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.indicator_inactive)
        style = Paint.Style.FILL
    }
    
    // 总数量
    private var totalCount = 0
    
    // 当前位置（从0开始）
    private var currentPosition = 0
    
    // 最大显示圆点数（避免数量过多时显示不下）
    private val maxVisibleDots = 10

    init {
        // 设置透明背景
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setPadding(16, 8, 16, 8)
    }

    /**
     * 设置总数和当前位置
     */
    fun setIndicator(total: Int, position: Int) {
        totalCount = total
        currentPosition = position.coerceIn(0, total - 1)
        invalidate() // 重绘视图
    }

    /**
     * 设置当前位置
     */
    fun setCurrentPosition(position: Int) {
        currentPosition = position.coerceIn(0, totalCount - 1)
        invalidate()
    }

    /**
     * 获取当前位置
     */
    fun getCurrentPosition(): Int = currentPosition

    /**
     * 获取总数
     */
    fun getTotalCount(): Int = totalCount

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 计算所需宽度
        val dotsToShow = minOf(totalCount, maxVisibleDots)
        val totalWidth = (dotsToShow * dotRadius * 2 + (dotsToShow - 1) * dotSpacing).toInt() + paddingLeft + paddingRight
        val totalHeight = (dotRadius * 2).toInt() + paddingTop + paddingBottom
        
        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (totalCount <= 0) return
        
        // 计算要显示的圆点范围
        val dotsToShow = minOf(totalCount, maxVisibleDots)
        val startIndex: Int
        val endIndex: Int
        
        if (totalCount <= maxVisibleDots) {
            // 如果总数不超过最大显示数，显示全部
            startIndex = 0
            endIndex = totalCount - 1
        } else {
            // 否则，以当前位置为中心显示
            val halfVisible = maxVisibleDots / 2
            startIndex = (currentPosition - halfVisible).coerceIn(0, totalCount - maxVisibleDots)
            endIndex = startIndex + maxVisibleDots - 1
        }
        
        // 计算起始X坐标（居中对齐）
        val viewWidth = width.toFloat()
        val totalDotsWidth = dotsToShow * dotRadius * 2 + (dotsToShow - 1) * dotSpacing
        var currentX = (viewWidth - totalDotsWidth) / 2 + dotRadius
        val centerY = height / 2f
        
        // 绘制圆点
        for (i in startIndex..endIndex) {
            val paint = if (i == currentPosition) activePaint else inactivePaint
            val radius = if (i == currentPosition) dotRadius * 1.2f else dotRadius
            canvas.drawCircle(currentX, centerY, radius, paint)
            currentX += dotRadius * 2 + dotSpacing
        }
        
        // 如果有更多圆点未显示，在两端绘制省略号
        if (startIndex > 0) {
            // 左侧省略号
            val ellipsisX = paddingLeft + dotRadius
            for (i in 0..2) {
                canvas.drawCircle(
                    ellipsisX + i * (dotRadius + 4),
                    centerY,
                    dotRadius * 0.5f,
                    inactivePaint
                )
            }
        }
        
        if (endIndex < totalCount - 1) {
            // 右侧省略号
            val ellipsisX = width - paddingRight - dotRadius * 7
            for (i in 0..2) {
                canvas.drawCircle(
                    ellipsisX + i * (dotRadius + 4),
                    centerY,
                    dotRadius * 0.5f,
                    inactivePaint
                )
            }
        }
    }

    /**
     * 设置圆点样式
     */
    fun setDotStyle(radius: Float, spacing: Float) {
        dotRadius = radius
        dotSpacing = spacing
        requestLayout()
    }

    /**
     * 设置圆点颜色
     */
    fun setDotColors(activeColor: Int, inactiveColor: Int) {
        activePaint.color = activeColor
        inactivePaint.color = inactiveColor
        invalidate()
    }
}