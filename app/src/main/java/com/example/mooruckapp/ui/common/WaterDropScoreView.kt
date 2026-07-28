package com.example.mooruckapp.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.mooruckapp.R

/**
 * 물방울 모양의 물주기 점수 표시 View
 *
 * 물방울 테두리는 항상 그려지고, [score](0~100)에 비례해서 아래쪽부터 내부가 채워짐
 *
 * 마이페이지에서 사용자 전체 물주기 점수를 시각적으로 보여주는 용도
 */
class WaterDropScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** 0~100 사이 점수 (값이 바뀌면 자동으로 다시 그려짐) */
    var score: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 3f
        color = ContextCompat.getColor(context, R.color.sub_green)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary_green)
    }

    private val dropPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildDropPath(w.toFloat(), h.toFloat())
    }

    /** 물방울(teardrop) 모양 Path 생성 */
    private fun rebuildDropPath(width: Float, height: Float) {
        dropPath.reset()
        if (width <= 0f || height <= 0f) return

        val strokeInset = outlinePaint.strokeWidth / 2f
        val w = width - strokeInset * 2f
        val h = height - strokeInset * 2f

        val radius = w / 2f
        val centerX = strokeInset + w / 2f
        val centerY = strokeInset + h - radius
        val topY = strokeInset

        dropPath.moveTo(centerX, topY)
        // 오른쪽 위 -> 오른쪽 원 시작점
        dropPath.cubicTo(
            centerX + radius * 1.3f, strokeInset + h * 0.55f,
            centerX + radius, centerY - radius * 0.4f,
            centerX + radius, centerY,
        )
        // 아래쪽 반원 (오른쪽 -> 왼쪽)
        dropPath.arcTo(
            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            0f,
            180f,
            false,
        )
        // 왼쪽 원 -> 왼쪽 위 -> 꼭짓점
        dropPath.cubicTo(
            centerX - radius, centerY - radius * 0.4f,
            centerX - radius * 1.3f, strokeInset + h * 0.55f,
            centerX, topY,
        )
        dropPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dropPath.isEmpty) return

        // 점수만큼 아래에서부터 채우기: score% 만큼의 높이만 클리핑
        val fillFraction = score / 100f
        val fillTop = height * (1f - fillFraction)

        canvas.save()
        canvas.clipRect(0f, fillTop, width.toFloat(), height.toFloat())
        canvas.drawPath(dropPath, fillPaint)
        canvas.restore()

        // 테두리는 항상 전체를 그림
        canvas.drawPath(dropPath, outlinePaint)
    }
}
