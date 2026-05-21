package com.rajk2007.aurawall

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout

class GlassCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val radius = 48f

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        super.onDraw(canvas)
    }
}
