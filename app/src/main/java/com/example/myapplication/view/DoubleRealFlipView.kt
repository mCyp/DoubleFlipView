package com.qidian.fonttest.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.myapplication.view.DeviceUtil
import com.example.myapplication.view.direction.*
import kotlin.math.*

const val TOP_SIDE = 1
const val BOTTOM_SIDE = 2
const val OUT_LEN = 40f

const val DIRECT_TL = 1
const val DIRECT_TR = 2
const val DIRECT_BL = 3
const val DIRECT_BR = 4

@Suppress("DEPRECATION")
class DoubleRealFlipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : View(context, attrs, defStyleInt) {

    // 1. 修正内容白边的问题
    // 2. 优化手势和动画

    // 背景色需要调整
    // 一页的宽高比 9:18 --- 1:1
    // 横屏的时候采用
    // 每一页的宽高应该提前计算好

    // 需要同时准备6页数据 可不可以优化一点？
    var mLeftBottomBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mLeftBottomBitmap = value
            field = value
        }
    var mLeftMiddleBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mLeftMiddleBitmap = value
            field = value
        }
    var mLeftTopBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mLeftTopBitmap = value
            field = value
        }
    var mRightTopBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mRightTopBitmap = value
            field = value
        }
    var mRightMiddleBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mRightMiddleBitmap = value
            field = value
        }
    var mRightBottomBitmap: Bitmap? = null
        set(value) {
            directDrawAction.mRightBottomBitmap = value
            field = value
        }

    private val flipPath = Path()
    private val reUsePath = Path()
    private val mPaint = Paint()

    private val mStartPoint: PointF = PointF(-1.0f, -1.0f)
    // 初页脚
    private val mOriginalCorner = PointF()
    // 滑动过程中的页脚
    private val mCurCornerPoint: PointF = PointF(-1.0f, -1.0f)

    // 触摸点和页脚之间的中点
    private val mMiddlePoint: PointF = PointF()
    private val mBezierStart1 = PointF()
    private val mBezierControl1 = PointF()
    private val mBezierVertex1 = PointF()
    private var mBezierEnd1 = PointF()
    private val mBezierStart2 = PointF()
    private val mBezierControl2 = PointF()
    private val mBezierVertex2 = PointF()
    private var mBezierEnd2 = PointF()
    private val mLeftPageLTPoint = PointF()
    private val mLeftPageRBPoint = PointF()
    private val mRightPageLTPoint = PointF()
    private val mRightPageRBPoint = PointF()
    private var curWidth = 0
    private var curHeight = 0
    // 每页的宽高
    private var pageWidth = 0
    private var pageHeight = 0

    // 0-代表左边一页 1-右边页
    private var flipPage = 0
    private var flipSide = 0
    private var mDegree: Double = 0.0
    private var mTouchDis: Float = 0.0f
    private var per: Float = 1.0f

    var isStopScroll: Boolean = true
    var bgColor: Int = 0

    private var mColorMatrixFilter: ColorMatrixColorFilter
    private val mMatrix: Matrix
    private val r = DeviceUtil.dip2px(context, 13f)

    private lateinit var directDrawAction: BaseDirectDrawAction

    init {
        val array = floatArrayOf(0.55f, 0f, 0f, 0f, 80.0f, 0f, 0.55f, 0f, 0f, 80.0f, 0f, 0f, 0.55f, 0f, 80.0f, 0f, 0f, 0f, 0.2f, 0f)
        val cm = ColorMatrix()
        cm.set(array)
        mColorMatrixFilter = ColorMatrixColorFilter(cm)
        mMatrix = Matrix()
        initDirectAction(DIRECT_TL)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w == 0 || h == 0) {
            return
        }

        curWidth = w
        curHeight = h
        pageWidth = w / 2
        pageHeight = h
        mLeftPageLTPoint.x = 0f
        mLeftPageLTPoint.y = 0f
        calculatePagePoint()
    }


    private fun calculatePagePoint() {
        mLeftPageRBPoint.x = mLeftPageLTPoint.x + pageWidth
        mLeftPageRBPoint.y = mLeftPageRBPoint.y + pageHeight
        mRightPageLTPoint.x = mLeftPageLTPoint.x + pageWidth
        mRightPageLTPoint.y = mLeftPageLTPoint.y
        mRightPageRBPoint.x = mRightPageLTPoint.x + pageWidth
        mRightPageRBPoint.y = mLeftPageRBPoint.y
        directDrawAction.mLeftPageLTPoint = mLeftPageLTPoint
        directDrawAction.mLeftPageRBPoint = mLeftPageRBPoint
        directDrawAction.mRightPageLTPoint = mRightPageLTPoint
        directDrawAction.mRightPageRBPoint = mRightPageRBPoint
        directDrawAction.context = context
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isStopScroll) {
            resetPoint()
        } else {
            calculatePoint()
        }

        // 1. 绘制非翻页的一边
        drawNoFlipSide(canvas)
        // 2. 绘制翻页的一边
        drawFlipPageContent(canvas)
        // 3. 绘制中间区域和阴影
        drawBookMiddleArea(canvas)
        // 4. 绘制翻页下面一页露出的区域和阴影
        drawFlipPageBottomPageContent(canvas)
        // 5. 绘制翻页的两侧阴影
        drawTwoSideShadow(canvas)
        // 6. 绘制翻页的背部内容和阴影
        drawBackContentAndShadow(canvas)

        /*val paint = Paint()
        paint.setColor(Color.BLACK)
        canvas.drawLine(mBezierControl1.x, mBezierControl1.y, mBezierControl2.x, mBezierControl2.y, paint)
        paint.strokeWidth = 10f
        canvas.drawPoint(mBezierStart1.x, mBezierStart1.y, paint)
        canvas.drawPoint(mBezierStart2.x, mBezierStart2.y, paint)*/
    }

    private fun drawBackContentAndShadow(canvas: Canvas){
        // 旋转 + 平移
        // 1. 限制绘制翻开白区域
        directDrawAction.drawBackContentAndShadow(canvas, reUsePath, flipPath, mBezierVertex1, mBezierVertex2, mBezierEnd2, mBezierEnd1, mCurCornerPoint, mDegree, mMatrix, mPaint, mBezierStart1, mBezierStart2, mTouchDis)
    }

    private fun drawTwoSideShadow(canvas: Canvas) {
       directDrawAction.drawTwoSideShadow(canvas, reUsePath, flipPath, mDegree, mCurCornerPoint, mBezierControl1, mBezierStart1, mBezierControl2, mBezierStart2, mOriginalCorner, mPaint)
    }

    private fun drawFlipPageBottomPageContent(canvas: Canvas) {
        directDrawAction.drawFlipPageBottomPageContent(canvas, reUsePath, flipPath, mDegree, mBezierStart1, mBezierStart2, mPaint, mTouchDis, per, abs(mBezierStart1.x - mBezierControl1.x))
    }

    private fun drawFlipPageContent(canvas: Canvas) {
        directDrawAction.drawFlipPageContent(canvas, reUsePath, flipPath, r)
    }

    private fun drawBookMiddleArea(canvas: Canvas){
        directDrawAction.drawBookMiddleArea(canvas, reUsePath, r, mPaint)
    }

    private fun drawNoFlipSide(canvas: Canvas) {
        directDrawAction.drawNoFlipSide(canvas, reUsePath, r)
    }

    private fun calculatePoint() {
        mMiddlePoint.x = (mCurCornerPoint.x + mOriginalCorner.x) / 2
        mMiddlePoint.y = (mCurCornerPoint.y + mOriginalCorner.y) / 2
        mBezierControl1.x =
            mMiddlePoint.x - (mOriginalCorner.y - mMiddlePoint.y) * (mOriginalCorner.y - mMiddlePoint.y) / (mOriginalCorner.x - mMiddlePoint.x)
        if(flipPage == 0) {
            mBezierControl1.x = min(mBezierControl1.x, mLeftPageRBPoint.x)
        } else {
            mBezierControl1.x = max(mBezierControl1.x, mRightPageLTPoint.x)
        }
        mBezierControl1.y = mOriginalCorner.y
        mBezierControl2.x = mOriginalCorner.x
        mBezierControl2.y =
            mMiddlePoint.y - (mOriginalCorner.x - mMiddlePoint.x) * (mOriginalCorner.x - mMiddlePoint.x) / (mOriginalCorner.y - mMiddlePoint.y)
        mBezierStart1.x = mBezierControl1.x - (mOriginalCorner.x - mBezierControl1.x) / 2
        if(flipPage == 0) {
            mBezierStart1.x = min(mBezierStart1.x, mLeftPageRBPoint.x)
        } else {
            mBezierStart1.x = max(mBezierStart1.x, mRightPageLTPoint.x)
        }

        mBezierStart1.y = mOriginalCorner.y
        // 和 Start1 等比例
        mBezierStart2.x = mOriginalCorner.x
        mBezierStart2.y =
            mBezierControl2.y - abs((mBezierControl1.x - mBezierStart1.x) / (mOriginalCorner.x - mBezierControl1.x)) * (mOriginalCorner.y - mBezierControl2.y)
        mBezierEnd1 = getCross(mCurCornerPoint, mBezierControl1, mBezierStart1, mBezierStart2)
        mBezierEnd2 = getCross(mCurCornerPoint, mBezierControl2, mBezierStart1, mBezierStart2)
        //Log.d("jj", "mBezierStart1:$mBezierStart1, mBezierControl1: $mBezierControl1, mBezierEnd1: $mBezierEnd1, mCurCornerPoint: $mCurCornerPoint")
        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4
        // fixme 注意场景，不能为90度

        mDegree = Math.toDegrees(atan2((abs(mOriginalCorner.y - mBezierControl2.y)).toDouble(), (abs(mOriginalCorner.x - mBezierControl1.x)).toDouble()))
        mTouchDis = hypot((mCurCornerPoint.x - mOriginalCorner.x).toDouble(), (mCurCornerPoint.y - mOriginalCorner.y).toDouble()).toFloat()

        flipPath.reset()
        flipPath.moveTo(mBezierStart1.x, mBezierStart1.y)
        flipPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y)
        flipPath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
        flipPath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        flipPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y)
        flipPath.lineTo(mOriginalCorner.x, mOriginalCorner.y)
        flipPath.close()
    }

    private fun resetPoint() {
        mCurCornerPoint.x = 0f
        mCurCornerPoint.y = 0f
        flipPage = 0
        mMiddlePoint.x = 0f
        mMiddlePoint.y = 0f
        mBezierControl1.x = 0f
        mBezierControl1.y = 0f
        mBezierControl2.x = 0f
        mBezierControl2.y = 0f
        mBezierStart1.x = 0f
        mBezierStart1.y = mOriginalCorner.y
        mBezierStart2.x = mOriginalCorner.x
        mBezierStart2.y = mBezierControl2.y - (mOriginalCorner.y - mBezierControl2.y) / 2
        mBezierEnd1.x = mOriginalCorner.x
        mBezierEnd1.y = mOriginalCorner.y
        mBezierEnd2.x = mOriginalCorner.x
        mBezierEnd2.y = mOriginalCorner.y
        mBezierVertex1.x = mOriginalCorner.x
        mBezierVertex1.y = mOriginalCorner.y
        mBezierVertex2.x = mOriginalCorner.x
        mBezierVertex2.y = mOriginalCorner.y
        //Log.d("jj", "reset mBezierStart1:$mBezierStart1, mBezierControl1: $mBezierControl1, mBezierEnd1: $mBezierEnd1, mCurCornerPoint: $mCurCornerPoint")
        flipPath.reset()
    }

    /**
     * 求线P1P2和线P3P4的焦点
     */
    private fun getCross(P1: PointF, P2: PointF, P3: PointF, P4: PointF): PointF {
        val crossPoint = PointF()
        // 二元函数通式： y=ax+b
        val a1 = (P2.y - P1.y) / (P2.x - P1.x)
        val b1 = (P1.x * P2.y - P2.x * P1.y) / (P1.x - P2.x)
        val a2 = (P4.y - P3.y) / (P4.x - P3.x)
        val b2 = (P3.x * P4.y - P4.x * P3.y) / (P3.x - P4.x)
        crossPoint.x = (b2 - b1) / (a1 - a2)
        crossPoint.y = a1 * crossPoint.x + b1
        return crossPoint
    }

    /**
     * 手指落下的位置
     */
    fun prepareOnDown(x: Float, y: Float) {
        if(x < mLeftPageLTPoint.x || x > mRightPageRBPoint.x || y < mLeftPageLTPoint.y || y > mLeftPageRBPoint.y) {
            return
        }
        
        mStartPoint.x = x
        mStartPoint.y = y
    }

    fun prePareDirection(direct: Int) {
        // 确定方向
        when(direct) {
            DIRECT_TL -> {
                flipPage = 1
                flipSide = BOTTOM_SIDE
                initDirectAction(DIRECT_TL)
            }
            DIRECT_TR -> {
                flipPage = 0
                flipSide = BOTTOM_SIDE
                initDirectAction(DIRECT_TR)
            }
            DIRECT_BL -> {
                flipPage = 1
                flipSide = TOP_SIDE
                initDirectAction(DIRECT_BL)
            }
            DIRECT_BR -> {
                flipPage = 0
                flipSide = TOP_SIDE
                initDirectAction(DIRECT_BR)
            }
        }
        initCornerPoint()
    }

    private fun initDirectAction(direct: Int) {
        when(direct) {
            DIRECT_TL -> {
                directDrawAction = RBDrawAction()
            }
            DIRECT_TR -> {
                directDrawAction = LBDrawAction()
            }
            DIRECT_BL -> {
                directDrawAction = RTDrawAction()
            }
            DIRECT_BR -> {
                directDrawAction = LTDrawAction()
            }
        }
        directDrawAction.mLeftTopBitmap = mLeftTopBitmap
        directDrawAction.mLeftMiddleBitmap = mLeftMiddleBitmap
        directDrawAction.mLeftBottomBitmap = mLeftBottomBitmap
        directDrawAction.mRightTopBitmap = mRightTopBitmap
        directDrawAction.mRightMiddleBitmap = mRightMiddleBitmap
        directDrawAction.mRightBottomBitmap = mRightBottomBitmap
        directDrawAction.mLeftPageLTPoint = mLeftPageLTPoint
        directDrawAction.mLeftPageRBPoint = mLeftPageRBPoint
        directDrawAction.mRightPageLTPoint = mRightPageLTPoint
        directDrawAction.mRightPageRBPoint = mRightPageRBPoint
        directDrawAction.context = context
    }

    private fun covertTouchPointToCurCornerPoint(x: Float, y: Float) {
        var targetTouchY = y
        // 1. 方向是否发生变更
        var offsetY = y - mStartPoint.y
        if(offsetY > 0) {
            if(flipSide == BOTTOM_SIDE) {
                flipSide = TOP_SIDE
                if(flipPage == 0) {
                    initDirectAction(DIRECT_BR)
                } else {
                    initDirectAction(DIRECT_BL)
                }
                initCornerPoint()
            }
        } else if(offsetY < 0) {
            if(flipSide == TOP_SIDE) {
                flipSide = BOTTOM_SIDE
                if(flipPage == 0) {
                    initDirectAction(DIRECT_TR)
                } else {
                    initDirectAction(DIRECT_TL)
                }
                initCornerPoint()
            }
        } else {
            // 如果 offsetY == 0
            offsetY = if(flipSide == TOP_SIDE){
                1f
            } else {
                -1f
            }
        }
        // 最大极限情况下求坐标
        val len = hypot(abs(mStartPoint.y - mOriginalCorner.y), pageWidth.toFloat())
        val maxYDis = sqrt(len * len - (x - mRightPageLTPoint.x) * (x - mRightPageLTPoint.x))
        var maxY = maxYDis
        if(flipSide == BOTTOM_SIDE) {
            maxY = mRightPageRBPoint.y - maxY
        }
        // 中间点坐标
        val midX: Float = if(flipPage == 0){
            (mLeftPageLTPoint.x + x) / 2
        } else {
            (mRightPageRBPoint.x + x) / 2
        }
        val midY = (maxY + mStartPoint.y) / 2
        // 开始点
        val startX: Float
        val startY: Float
        if(flipSide == TOP_SIDE) {
            startX = mLeftPageRBPoint.x
            startY = mLeftPageLTPoint.y
        } else {
            startX = mLeftPageRBPoint.x
            startY = mLeftPageRBPoint.y
        }
        // 计算页脚能够翻转的最大角度
        val maxDegree = Math.toDegrees(atan2(abs(midX - startX).toDouble(), abs(midY - startY).toDouble())) * 2
        offsetY = abs(offsetY)
        if(offsetY >= abs(maxY - mStartPoint.y)) {
            offsetY = abs(maxY - mStartPoint.y)
        }
        per = 1 - offsetY / abs(maxY - mStartPoint.y)
        val perDe = (offsetY / abs(maxY - mStartPoint.y)) * maxDegree
        if(flipSide == BOTTOM_SIDE) {
           if(y < maxY)  {
               targetTouchY = maxY
           }
        } else {
            if(y > maxY) {
                targetTouchY = maxY
            }
        }
        // 转动的半径
        val r = abs(mStartPoint.y - mOriginalCorner.y)
        val rad = Math.toRadians(perDe)
        // 计算边角的坐标
        if(flipPage == 0) {
            mCurCornerPoint.x = (x + r * sin(rad)).toFloat()
        } else {
            mCurCornerPoint.x = (x - r * sin(rad)).toFloat()
        }
        if(flipSide == TOP_SIDE) {
            mCurCornerPoint.y = (targetTouchY - r * cos(rad)).toFloat()
        } else {
            mCurCornerPoint.y = (targetTouchY + r * cos(rad)).toFloat()
        }
    }

    // 设置的页脚的顶点
    fun setTouchPoint(x: Float, y: Float) {
        val targetX = min(max(mLeftPageLTPoint.x, x), mRightPageRBPoint.x)
        val targetY = min(max(mLeftPageLTPoint.y, y), mRightPageRBPoint.y)
        covertTouchPointToCurCornerPoint(targetX, targetY)
    }


    fun stopScroll() {
        isStopScroll = true
    }

    private fun initCornerPoint() {
        if(flipPage == 0) {
            if(flipSide == TOP_SIDE) {
                mOriginalCorner.x = mLeftPageLTPoint.x
                mOriginalCorner.y = mLeftPageLTPoint.y
            } else {
                mOriginalCorner.x = mLeftPageLTPoint.x
                mOriginalCorner.y = mLeftPageRBPoint.y
            }
        } else {
            if(flipSide == TOP_SIDE) {
                mOriginalCorner.x = mRightPageRBPoint.x
                mOriginalCorner.y = mRightPageLTPoint.y
            } else {
                mOriginalCorner.x = mRightPageRBPoint.x
                mOriginalCorner.y = mRightPageRBPoint.y
            }
        }
        isStopScroll = false
    }
}