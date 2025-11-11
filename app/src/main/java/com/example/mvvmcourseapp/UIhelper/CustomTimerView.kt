package com.example.mvvmcourseapp.UIhelper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.data.models.QuizQuestion

class CustomTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var timer: CountDownTimer? = null
    private var remainingSeconds: Int = 0
    private var totalSeconds: Int = 0
    var onTimerFinished: (() -> Unit)? = null

    // Цвета
    private val purpleColor: Int by lazy {
        ResourcesCompat.getColor(resources, R.color.purple, null)
    }
    private val backgroundColor: Int = Color.LTGRAY

    // Кисти
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    // Размеры
    private var strokeWidth: Float = 10f
    private var textSize: Float = 60f

    // Шрифт
    private var customFont: Typeface? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.CustomTimerView) {
            strokeWidth = getDimension(R.styleable.CustomTimerView_strokeWidth, 20f)
            textSize = getDimension(R.styleable.CustomTimerView_textSize, 60f)

            try {
                val fontId = getResourceId(R.styleable.CustomTimerView_customFont, -1)
                if (fontId != -1) {
                    customFont = ResourcesCompat.getFont(context, fontId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        backgroundPaint.color = backgroundColor
        backgroundPaint.strokeWidth = strokeWidth

        progressPaint.color = purpleColor
        progressPaint.strokeWidth = strokeWidth

        textPaint.color = purpleColor
        textPaint.textSize = textSize
        customFont?.let { textPaint.typeface = it }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) / 2f) - strokeWidth

        // Рисуем фоновый круг
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Рисуем прогресс
        if (totalSeconds > 0) {
            val sweepAngle = 360f * remainingSeconds / totalSeconds
            val rect = RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)
        }

        // Рисуем текст
        val text = remainingSeconds.toString()
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textY = centerY - (textBounds.top + textBounds.bottom) / 2f
        canvas.drawText(text, centerX, textY, textPaint)
    }

    fun startTimer(totalSeconds: Int, currentQuestion : QuizQuestion?, flag: Boolean) {
        if (currentQuestion != null && !flag) {
            stopTimer()
            this.totalSeconds = totalSeconds
            this.remainingSeconds = totalSeconds

            timer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000).toInt()
                    invalidate()
                }

                override fun onFinish() {
                    remainingSeconds = 0
                    invalidate()
                    onTimerFinished?.invoke()
                }
            }.start()

            invalidate()
        }
    }

    fun getRemainingTime(): Int
    {
        return remainingSeconds
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    fun setCustomFont(fontId: Int) {
        try {
            customFont = ResourcesCompat.getFont(context, fontId)
            textPaint.typeface = customFont
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTimer()
    }
}