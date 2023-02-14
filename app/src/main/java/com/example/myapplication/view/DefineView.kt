package com.example.myapplication.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class DefineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : View(context, attrs, defStyleInt) {

    var mPaint: Paint = Paint()

    init {
        mPaint.isAntiAlias = true
        mPaint.isDither = true

    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        mPaint.color = Color.RED
        canvas?.drawRect(100f, 100f, 400f, 500f, mPaint)

        canvas?.save()
        mPaint.color = Color.BLUE
        canvas?.rotate(60f, 100f, 500f)
        canvas?.drawRect(100f, 100f, 400f, 500f, mPaint)
        canvas?.restore()

    }
}