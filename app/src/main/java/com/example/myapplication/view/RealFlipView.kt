package com.qidian.fonttest.view

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.myapplication.view.DeviceUtil
import kotlin.math.*

private const val TOP_SIDE = 1
private const val BOTTOM_SIDE = 2
private const val OUT_LEN = 40f
@Suppress("DEPRECATION")
class RealFlipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleInt: Int = 0
) : View(context, attrs, defStyleInt) {

    // 背景色需要调整
    // 一页的宽高比 9:18 --- 1:1
    // 横屏的时候采用
    // 每一页的宽高应该提前计算好

    // 需要准备6页数据
    var mLeftBottomBitmap: Bitmap? = null
    var mLeftMiddleBitmap: Bitmap? = null
    var mLeftTopBitmap: Bitmap? = null
    var mRightTopBitmap: Bitmap? = null
    var mRightMiddleBitmap: Bitmap? = null
    var mRightBottomBitmap: Bitmap? = null

    private val flipPath = Path()
    private val reUsePath = Path()

    private val mPaint = Paint()
    private val shadowColors = intArrayOf(-0x4f99999a, 0x666666)
    private val shadowReverseColors = intArrayOf(0x666666, -0x4f99999a)

    private val mTouchPoint: PointF = PointF(-1.0f, -1.0f)

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

    // 滑动对应的页脚
    private val mCorner = PointF()

    private var curWidth = 0
    private var curHeight = 0
    // 每页的宽高
    private var pageWidth = 0
    private var pageHeight = 0

    // 触摸事件需要判断：
    // 1. 触摸的是左右哪一侧？
    // 2. 反动的上半部分还是下半部分？
    // 0-代表左边一页 1-右边页
    private var curFlipPage = 0
    private var flipSide = 0
    private var mDegree: Double = 0.0
    private var mTouchDis: Float = 0.0f

    var isStopScroll: Boolean = false
    var bgColor: Int = 0

    private var mColorMatrixFilter: ColorMatrixColorFilter
    private val mMatrixArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1.0f)
    private val mMatrix: Matrix
    private val r = DeviceUtil.dip2px(context, 13f)

    init {
        val array = floatArrayOf(0.55f, 0f, 0f, 0f, 80.0f, 0f, 0.55f, 0f, 0f, 80.0f, 0f, 0f, 0.55f, 0f, 80.0f, 0f, 0f, 0f, 0.2f, 0f)
        val cm = ColorMatrix()
        cm.set(array)
        mColorMatrixFilter = ColorMatrixColorFilter(cm)
        mMatrix = Matrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w == 0 || h == 0) {
            return
        }

        // 不考虑适配不同机型
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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        flipSide = BOTTOM_SIDE
        curFlipPage = 1
        mTouchPoint.x = 1710f
        mTouchPoint.y = 696f
        isStopScroll = false
        mCorner.x = mRightPageRBPoint.x
        mCorner.y = mRightPageRBPoint.y

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
    }

    private fun drawBackContentAndShadow(canvas: Canvas){
        // 1. 限制绘制翻开白区域
        reUsePath.reset()
        reUsePath.moveTo(mBezierVertex1.x, mBezierVertex1.y)
        reUsePath.lineTo(mBezierVertex2.x, mBezierVertex2.y)
        reUsePath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        reUsePath.lineTo(mTouchPoint.x, mTouchPoint.y)
        reUsePath.lineTo(mBezierEnd1.x, mBezierEnd1.y)
        if(curFlipPage == 1) {
            reUsePath.offset(-mRightPageLTPoint.x, 0f)
        }
        reUsePath.close()


        // 这个offset根据页面调整

        canvas.save()
        if(curFlipPage == 1) {
            canvas.translate(mRightPageLTPoint.x, mRightPageLTPoint.y)
            flipPath.offset(-mRightPageLTPoint.x, 0f)
        }
        canvas.clipPath(flipPath)
        canvas.clipPath(reUsePath)
        //mPaint.colorFilter = mColorMatrixFilter
        canvas.drawColor(bgColor)

        // 2. 绘制在白色区域
        // 这里的公式其实是一个沿着 y = kx 的对称，理解起来有点难度，
        // 公式的计算地址：https://juejin.cn/post/6844903504671145992
        val dis = hypot(mCorner.x - mBezierControl1.x, mCorner.y - mBezierControl2.y)
        val curSin: Float = (mCorner.x - mBezierControl1.x) / dis
        val curCos: Float = (mBezierControl2.y - mCorner.y) / dis
        mMatrixArray[0] = 1 - 2 * curCos * curCos
        mMatrixArray[1] = 2 * curSin * curCos
        mMatrixArray[3] = mMatrixArray[1]
        mMatrixArray[4] = 1 - 2 * curSin * curSin
        mMatrix.reset()
        mMatrix.setValues(mMatrixArray)
        if(curFlipPage == 0) {
            mMatrix.preTranslate(-(mBezierControl1.x), -mBezierControl1.y)
            mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y)
        } else {
            mMatrix.preTranslate(-(mBezierControl1.x - mRightPageLTPoint.x), -mBezierControl1.y)
            mMatrix.postTranslate(mBezierControl1.x - mRightPageLTPoint.x, mBezierControl1.y)
        }
        if(curFlipPage == 0) {
            mRightMiddleBitmap?.let {
                canvas.drawBitmap(it, mMatrix, mPaint)
            }
        } else {
            mRightMiddleBitmap?.let {
                canvas.drawBitmap(it, mMatrix, mPaint)
            }
        }

        mPaint.colorFilter = null

        // 3. 设置阴影
        if(curFlipPage == 1) {
            canvas.translate(-mRightPageLTPoint.x, -mRightPageLTPoint.y)
        }
        val minDis = mTouchDis / 8
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        val left: Float
        val right: Float
        val bottom: Float
        val top: Float
        val rotateDegree: Float
        if(curFlipPage == 0) {
            left = mBezierStart1.x - minDis - 1
            right = mBezierStart1.x + 1
            if(flipSide == TOP_SIDE) {
                top = mBezierStart1.y
                bottom = top + rectHeight.toFloat()
                rotateDegree = (90 - mDegree.toFloat())
            } else {
                bottom = mBezierStart1.y
                top = bottom - rectHeight.toFloat()
                rotateDegree = -(90 - mDegree.toFloat())
            }
        } else {
            left = mBezierStart1.x - 1
            right = mBezierStart1.x + minDis + 1
            if(flipSide == TOP_SIDE) {
                top = mBezierStart1.y
                bottom = top + rectHeight.toFloat()
                rotateDegree = -(90 - mDegree.toFloat())
            } else {
                bottom = mBezierStart1.y
                top = bottom - rectHeight.toFloat()
                rotateDegree = (90 - mDegree.toFloat())
            }
        }
        if(curFlipPage == 0) {
            mPaint.shader = getGradient(left, mBezierStart1.y, right, mBezierStart1.y, shadowColors)
        } else {
            mPaint.shader = getGradient(left, mBezierStart1.y, right, mBezierStart1.y, shadowReverseColors)
        }

        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        canvas.drawRect(left, top, right, bottom, mPaint)
        canvas.restore()
    }

    private fun drawTwoSideShadow(canvas: Canvas) {
        val outPoint = PointF()
        if (curFlipPage == 0) {
            outPoint.x = mTouchPoint.x + OUT_LEN * cos(mDegree).toFloat()
        } else {
            outPoint.x = mTouchPoint.x - OUT_LEN * cos(mDegree).toFloat()
        }
        if (flipSide == TOP_SIDE) {
            outPoint.y = mTouchPoint.y + OUT_LEN * sin(mDegree).toFloat()
        } else {
            outPoint.y = mTouchPoint.y - OUT_LEN * sin(mDegree).toFloat()
        }

        // 绘制一半的阴影
        canvas.save()
        // 不同页面翻转的角度不一致
        reUsePath.reset()
        reUsePath.moveTo(outPoint.x, outPoint.y)
        reUsePath.lineTo(mTouchPoint.x, mTouchPoint.y)
        reUsePath.lineTo(mBezierControl1.x, mBezierControl1.y)
        reUsePath.lineTo(mBezierStart1.x, mBezierStart1.y)
        reUsePath.close()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(flipPath)
            } else {
                canvas.clipPath(flipPath, Region.Op.DIFFERENCE)
            }
            canvas.clipPath(reUsePath)
        } catch (e: Exception) {
            // Logger.exception(e);
        }
        val deOne = Math.toDegrees(
            atan2(
                abs(mTouchPoint.x - mBezierControl1.x).toDouble(),
                abs(mTouchPoint.y - mBezierControl1.y).toDouble()
            )
        )
        val targetDeOne = if (curFlipPage == 0) {
            if (flipSide == TOP_SIDE) {
                -deOne
            } else {
                deOne
            }
        } else {
            if (flipSide == TOP_SIDE) {
                deOne
            } else {
                -deOne
            }
        }
        canvas.rotate(targetDeOne.toFloat(), outPoint.x, outPoint.y)
        val colors = if (curFlipPage == 0) shadowColors else shadowReverseColors
        var bottomFirst = 0f
        var rightFirst = 0f
        if(curFlipPage == 0) {
            rightFirst = (outPoint.x - sin(mDegree) * OUT_LEN - 1).toFloat()
        } else {
            rightFirst = (outPoint.x + sin(mDegree) * OUT_LEN + 1).toFloat()
        }
        if(flipSide == TOP_SIDE) {
            bottomFirst = outPoint.y - abs(mCorner.x - mBezierControl1.x)
        } else {
            bottomFirst = outPoint.y + abs(mCorner.x - mBezierControl1.x)
        }
        mPaint.shader = getGradient(outPoint.x, mBezierControl1.y, rightFirst, mBezierControl1.y, colors)
        canvas.drawRect(outPoint.x, outPoint.y, rightFirst, bottomFirst ,mPaint)
        canvas.restore()

        canvas.save()
        reUsePath.reset()
        reUsePath.moveTo(outPoint.x, outPoint.y)
        reUsePath.lineTo(mTouchPoint.x, mTouchPoint.y)
        reUsePath.lineTo(mBezierControl2.x, mBezierControl2.y)
        reUsePath.lineTo(mBezierStart2.x, mBezierStart2.y)
        reUsePath.close()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(flipPath)
            } else {
                canvas.clipPath(flipPath, Region.Op.DIFFERENCE)
            }
            canvas.clipPath(reUsePath)
        } catch (e: Exception) {
            // Logger.exception(e);
        }
        val deTwo = Math.toDegrees(
            atan2(
                abs(mTouchPoint.x - mBezierControl2.x).toDouble(),
                abs(mTouchPoint.y - mBezierControl2.y).toDouble()
            )
        )
        val targetDeTwo = if (curFlipPage == 0) {
            if (flipSide == TOP_SIDE) {
                deTwo
            } else {
                -deTwo
            }
        } else {
            if (flipSide == TOP_SIDE) {
                -deTwo
            } else {
                deTwo
            }
        }
        canvas.rotate(targetDeTwo.toFloat(), outPoint.x, outPoint.y)

        var topSecond = 0f
        var rightSecond = 0f
        if(curFlipPage == 0) {
            rightSecond = (outPoint.x - cos(mDegree) * OUT_LEN).toFloat() - 1
        } else {
            rightSecond = (outPoint.x + cos(mDegree) * OUT_LEN).toFloat() + 1
        }
        if(flipSide == TOP_SIDE) {
            topSecond = outPoint.y + abs(mCorner.y - mBezierControl2.y)
        } else {
            topSecond = outPoint.y - abs(mCorner.y - mBezierControl2.y)
        }
        mPaint.shader =
            getGradient(outPoint.x, 0f, rightSecond, 0f, colors)
        canvas.drawRect(outPoint.x, outPoint.y, rightSecond, topSecond ,mPaint)
        canvas.restore()
    }

    private fun drawFlipPageBottomPageContent(canvas: Canvas) {
        canvas.save()
        // 绘制内容部分
        if(curFlipPage == 0) {
            reUsePath.reset()
            reUsePath.addRect(mLeftPageLTPoint.x, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageRBPoint.y, Path.Direction.CW)
            canvas.clipPath(reUsePath)
            canvas.clipPath(flipPath)
            mLeftBottomBitmap?.let {
                canvas.drawBitmap(it, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
            }
        } else if(curFlipPage == 1) {
            reUsePath.reset()
            reUsePath.addRect(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageRBPoint.x, mRightPageRBPoint.y, Path.Direction.CW)
            canvas.clipPath(reUsePath)
            canvas.clipPath(flipPath)
            mRightBottomBitmap?.let {
                canvas.drawBitmap(it, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
            }
        }
        // 绘制阴影
        val rotateDegree = if(curFlipPage == 0) {
            if(flipSide == TOP_SIDE) {
                (90 - mDegree.toFloat())
            } else {
                -(90 - mDegree.toFloat())
            }
        } else {
            if(flipSide == TOP_SIDE) {
                -(90 - mDegree.toFloat())
            } else {
                (90 - mDegree.toFloat())
            }
        }
        canvas.clipPath(flipPath)
        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        var left = 0f
        var right = 0f
        var top = 0f
        var bottom = 0f
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        if(curFlipPage == 0) {
            right = mBezierStart1.x
            left = right - mTouchDis / 4
            if(flipSide == TOP_SIDE) {
                top = mBezierStart1.y
                bottom = mBezierStart1.y + rectHeight.toFloat()
            } else {
                bottom = mBezierStart1.y
                top = mBezierStart1.y - rectHeight.toFloat()
            }
            mPaint.shader = getGradient(left, top, right, top, shadowReverseColors)
        } else if(curFlipPage == 1) {
            left = mBezierStart1.x
            right = left + mTouchDis / 4
            if(flipSide == TOP_SIDE) {
                top = mBezierStart1.y
                bottom = mBezierStart1.y + rectHeight.toFloat()
            } else {
                bottom = mBezierStart1.y
                top = mBezierStart1.y - rectHeight.toFloat()
            }
            mPaint.shader = getGradient(left, top, right, top, shadowColors)
        }
        canvas.drawRect(left, top, right, bottom, mPaint)
        canvas.restore()
    }

    private fun drawFlipPageContent(canvas: Canvas) {
        canvas.save()
        if (curFlipPage == 0) {
            reUsePath.reset()
            reUsePath.moveTo(mLeftPageRBPoint.x - r, mLeftPageLTPoint.y)
            reUsePath.arcTo( mLeftPageRBPoint.x - 2 * r, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageLTPoint.y + 2 * r, -90f, 90f, false)
            reUsePath.lineTo(mLeftPageRBPoint.x, mLeftPageRBPoint.y - r)
            reUsePath.arcTo(mLeftPageRBPoint.x - 2 * r, mLeftPageRBPoint.y - 2 * r,mLeftPageRBPoint.x, mLeftPageRBPoint.y, 0f, 90f, false)
            reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageRBPoint.y)
            reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageLTPoint.y)
            reUsePath.close()
            canvas.clipPath(reUsePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(flipPath)
            } else {
                canvas.clipPath(flipPath, Region.Op.DIFFERENCE)
            }
            mLeftTopBitmap?.let {
                canvas.drawBitmap(it, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
            }
        } else {
            reUsePath.reset()
            reUsePath.moveTo(mRightPageLTPoint.x + r, mRightPageLTPoint.y)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageLTPoint.x + 2 * r, mRightPageLTPoint.y + 2 * r, -90f, -90f, false)
            reUsePath.lineTo(mRightPageLTPoint.x, mRightPageRBPoint.y - r)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageRBPoint.y - 2 * r,mRightPageLTPoint.x + 2 * r, mRightPageRBPoint.y, -180f, -90f, false)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageRBPoint.y)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageLTPoint.y)
            reUsePath.close()
            canvas.clipPath(reUsePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(flipPath)
            } else {
                canvas.clipPath(flipPath, Region.Op.DIFFERENCE)
            }
            mRightTopBitmap?.let {
                canvas.drawBitmap(it, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
            }
        }
        canvas.restore()
    }

    private fun drawBookMiddleArea(canvas: Canvas){
        val r = DeviceUtil.dip2px(context, 13f)

        reUsePath.reset()
        reUsePath.moveTo(mLeftPageRBPoint.x - r, mLeftPageLTPoint.y)
        reUsePath.arcTo( mLeftPageRBPoint.x - 2 * r, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageLTPoint.y + 2 * r, -90f, 90f, false)
        reUsePath.lineTo(mLeftPageRBPoint.x, mLeftPageRBPoint.y - r)
        reUsePath.arcTo(mLeftPageRBPoint.x - 2 * r, mLeftPageRBPoint.y - 2 * r,mLeftPageRBPoint.x, mLeftPageRBPoint.y, 0f, 90f, false)
        reUsePath.close()
        mPaint.shader = getGradient(mLeftPageRBPoint.x - r, 0f, mLeftPageRBPoint.x, 0f, shadowReverseColors)
        canvas.drawPath(reUsePath, mPaint)

        reUsePath.reset()
        reUsePath.moveTo(mRightPageLTPoint.x + r, mRightPageLTPoint.y)
        reUsePath.arcTo(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageLTPoint.x + 2 * r, mRightPageLTPoint.y + 2 * r, -90f, -90f, false)
        reUsePath.lineTo(mRightPageLTPoint.x, mRightPageRBPoint.y - r)
        reUsePath.arcTo(mRightPageLTPoint.x, mRightPageRBPoint.y - 2 * r,mRightPageLTPoint.x + 2 * r, mRightPageRBPoint.y, -180f, -90f, false)
        reUsePath.close()
        mPaint.shader = getGradient(mRightPageLTPoint.x, 0f, mRightPageLTPoint.x + r, 0f, shadowColors)
        canvas.drawPath(reUsePath, mPaint)
    }

    @Suppress("SameParameterValue")
    private fun getGradient(l: Float, t: Float, r: Float, b: Float, colors: IntArray): LinearGradient {
        return LinearGradient(l, t, r, b, colors, floatArrayOf(0f, 1.0f), Shader.TileMode.CLAMP)
    }

    private fun drawNoFlipSide(canvas: Canvas) {
        canvas.save()
        if(curFlipPage == 0) {
            reUsePath.reset()
            reUsePath.moveTo(mRightPageLTPoint.x + r, mRightPageLTPoint.y)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageLTPoint.x + 2 * r, mRightPageLTPoint.y + 2 * r, -90f, -90f, false)
            reUsePath.lineTo(mRightPageLTPoint.x, mRightPageRBPoint.y - r)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageRBPoint.y - 2 * r,mRightPageLTPoint.x + 2 * r, mRightPageRBPoint.y, -180f, -90f, false)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageRBPoint.y)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageLTPoint.y)
            reUsePath.close()
            canvas.clipPath(reUsePath)
            mRightTopBitmap?.let { b->
                if(!b.isRecycled) {
                   canvas.drawBitmap(b, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
                }
            }
        } else {
            reUsePath.reset()
            reUsePath.moveTo(mLeftPageRBPoint.x - r, mLeftPageLTPoint.y)
            reUsePath.arcTo( mLeftPageRBPoint.x - 2 * r, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageLTPoint.y + 2 * r, -90f, 90f, false)
            reUsePath.lineTo(mLeftPageRBPoint.x, mLeftPageRBPoint.y - r)
            reUsePath.arcTo(mLeftPageRBPoint.x - 2 * r, mLeftPageRBPoint.y - 2 * r,mLeftPageRBPoint.x, mLeftPageRBPoint.y, 0f, 90f, false)
            reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageRBPoint.y)
            reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageLTPoint.y)
            reUsePath.close()
            canvas.clipPath(reUsePath)
            mLeftTopBitmap?.let { b->
                if(!b.isRecycled) {
                    canvas.drawBitmap(b, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
                }
            }
        }
        canvas.restore()
    }

    private fun calculatePoint() {
        mMiddlePoint.x = (mTouchPoint.x + mCorner.x) / 2
        mMiddlePoint.y = (mTouchPoint.y + mCorner.y) / 2
        mBezierControl1.x =
            mMiddlePoint.x - (mCorner.y - mMiddlePoint.y) * (mCorner.y - mMiddlePoint.y) / (mCorner.x - mMiddlePoint.x)
        mBezierControl1.y = mCorner.y
        mBezierControl2.x = mCorner.x
        mBezierControl2.y =
            mMiddlePoint.y - (mCorner.x - mMiddlePoint.x) * (mCorner.x - mMiddlePoint.x) / (mCorner.y - mMiddlePoint.y)
        mBezierStart1.x = mBezierControl1.x - (mCorner.x - mBezierControl1.x) / 2
        mBezierStart1.y = mCorner.y
        mBezierStart2.x = mCorner.x
        mBezierStart2.y = mBezierControl2.y - (mCorner.y - mBezierControl2.y) / 2
        mBezierEnd1 = getCross(mTouchPoint, mBezierControl1, mBezierStart1, mBezierStart2)
        mBezierEnd2 = getCross(mTouchPoint, mBezierControl2, mBezierStart1, mBezierStart2)
        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4
        // 注意场景，不能为90度

        mDegree = Math.toDegrees(atan2((abs(mCorner.y - mBezierControl2.y)).toDouble(), (abs(mCorner.x - mBezierControl1.x)).toDouble()))
        mTouchDis = hypot((mTouchPoint.x - mCorner.x).toDouble(), (mTouchPoint.y - mCorner.y).toDouble()).toFloat()

        flipPath.reset()
        flipPath.moveTo(mBezierStart1.x, mBezierStart1.y)
        flipPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y)
        flipPath.lineTo(mTouchPoint.x, mTouchPoint.y)
        flipPath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        flipPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y)
        flipPath.lineTo(mCorner.x, mCorner.y)
        flipPath.close()
    }

    private fun resetPoint() {
        mTouchPoint.x = 0f
        mTouchPoint.y = 0f
        curFlipPage = 0
        mMiddlePoint.x = 0f
        mMiddlePoint.y = 0f
        mBezierControl1.x = 0f
        mBezierControl1.y = 0f
        mBezierControl2.x = 0f
        mBezierControl2.y = 0f
        mBezierStart1.x = 0f
        mBezierStart1.y = mCorner.y
        mBezierStart2.x = mCorner.x
        mBezierStart2.y = mBezierControl2.y - (mCorner.y - mBezierControl2.y) / 2
        mBezierEnd1 = getCross(mTouchPoint, mBezierControl1, mBezierStart1, mBezierStart2)
        mBezierEnd2 = getCross(mTouchPoint, mBezierControl2, mBezierStart1, mBezierStart2)
        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4

        flipPath.reset()
        flipPath.moveTo(mBezierStart1.x, mBezierStart1.y)
        flipPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y)
        flipPath.lineTo(mTouchPoint.x, mTouchPoint.y)
        flipPath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        flipPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y)
        flipPath.lineTo(mCorner.x, mCorner.y)
        flipPath.close()
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


    // 先写一个简单的事件
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        // fixme 确定位置后，需要确定，mCorner
        Log.d("wangjie", "x: ${x}, y: ${y}")
        /*when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchPoint.x = x
                mTouchPoint.y = y
                if(x < curWidth / 2) {
                    curFlipPage = 0
                } else {
                    curFlipPage = 1
                }
                if(y < curHeight / 2) {
                    flipSide = TOP_SIDE
                } else {
                    flipSide = BOTTOM_SIDE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(mTouchPoint.x != -1f && mTouchPoint.y != -1f) {
                    mTouchPoint.x = x
                    mTouchPoint.y = y
                }
            }
            MotionEvent.ACTION_UP -> {

            }
        }*/
        return super.onTouchEvent(event)
    }


}