package com.example.myapplication.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import com.qidian.fonttest.view.DoubleRealFlipView

// 滑动处理
// 滑动优化处理
class DoubleFlipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : FrameLayout(context, attrs, defStyleInt), GestureDetector.OnGestureListener {

    // 处理滑动
    private val mGestureDetector: GestureDetectorCompat
    val mDoubleRealFlipView: DoubleRealFlipView


    init {
        mGestureDetector = GestureDetectorCompat(context, this)
        // 添加View进去
        mDoubleRealFlipView = DoubleRealFlipView(context)
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(mDoubleRealFlipView, lp)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { e->
            when(e.action) {
                MotionEvent.ACTION_UP -> {
                    // 翻页或者取消翻页
                }
                MotionEvent.ACTION_CANCEL -> {
                    // 翻页
                }
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        // 计算位置
        mDoubleRealFlipView.prepareForScroll(e.x, e.y)
        return true
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        // 根据位置翻页
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        // 翻页
        mDoubleRealFlipView.setTouchPoint(e2.x, e2.y)
        return true
    }

    override fun onLongPress(e: MotionEvent?) {

    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        // 翻页
        return true
    }
}