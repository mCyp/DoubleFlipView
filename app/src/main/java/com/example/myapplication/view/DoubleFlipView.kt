package com.example.myapplication.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import com.qidian.fonttest.view.*

private const val STATUS_NONE = 0
private const val STATUS_DOWN = 1
private const val STATUS_MOVE = 2
// 滑动处理
// 滑动优化处理
class DoubleFlipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : FrameLayout(context, attrs, defStyleInt), GestureDetector.OnGestureListener {

    // 确定翻页的手势
    // 1. 根据手指触碰的位置进行翻页，实际是围绕手指的点进行旋转
    // 2. 到达翻页顶点触发的行为，bezierc2 和 beziers2 往一处汇集
    // 3. 折成什么角度由位置决定的
    // 4. 滑动的时候其实有一个最小的触发动作

    // 处理滑动
    private var status: Int = STATUS_NONE
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
                    status = STATUS_NONE
                    mDoubleRealFlipView.stopScroll()
                }
                MotionEvent.ACTION_CANCEL -> {
                    // 翻页
                    status = STATUS_NONE
                    mDoubleRealFlipView.stopScroll()
                }
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        // 计算位置
        status = STATUS_DOWN
        mDoubleRealFlipView.prepareOnDown(e.x, e.y)
        return true
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        // 根据位置翻页
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        //Log.d("wangjie", "distanceX:$distanceX, distanceY: $distanceY")
        // 往左上是正
        if(status == STATUS_DOWN || status == STATUS_MOVE) {
            if(distanceX != 0f && status == STATUS_DOWN) {
                if(distanceX > 0 && distanceY > 0) {
                    mDoubleRealFlipView.prePareDirection(DIRECT_TL)
                } else if (distanceX > 0 && distanceY <= 0) {
                    mDoubleRealFlipView.prePareDirection(DIRECT_BL)
                } else if (distanceX < 0 && distanceY > 0) {
                    mDoubleRealFlipView.prePareDirection(DIRECT_TR)
                } else {
                    mDoubleRealFlipView.prePareDirection(DIRECT_BR)
                }
                status = STATUS_MOVE
            }
            if(status == STATUS_MOVE) {
                //Log.d("wangjie", "mScroller.startScroll")
                Log.d("wangjie", "touchPoint: ${e2.x}, ${e2.y}")
                mDoubleRealFlipView.setTouchPoint(e2.x, e2.y)
                mDoubleRealFlipView.invalidate()
            }
        }
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