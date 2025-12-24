package com.example.flowalbum

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 正方形ImageView
 * 自动保持宽高相等
 */
class SquareImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 使用宽度来设置高度，保持正方形
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}