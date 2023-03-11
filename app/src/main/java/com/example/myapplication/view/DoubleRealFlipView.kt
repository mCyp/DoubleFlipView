package com.qidian.fonttest.view

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.myapplication.view.DeviceUtil
import kotlin.math.*

private const val TOP_SIDE = 1
private const val BOTTOM_SIDE = 2
private const val OUT_LEN = 40f

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

    // 1. 修正架构
    // 2. 修正阴影
    // 3. 优化手势和动画

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

    // 触摸事件需要判断：
    // 1. 触摸的是左右哪一侧？
    // 2. 反动的上半部分还是下半部分？
    // 0-代表左边一页 1-右边页
    private var flipPage = 0
    private var flipSide = 0
    private var mDegree: Double = 0.0
    private var mTouchDis: Float = 0.0f

    var isStopScroll: Boolean = false
    var bgColor: Int = 0

    private var mColorMatrixFilter: ColorMatrixColorFilter
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
        reUsePath.reset()
        reUsePath.moveTo(mBezierVertex1.x, mBezierVertex1.y)
        reUsePath.lineTo(mBezierVertex2.x, mBezierVertex2.y)
        reUsePath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        reUsePath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
        reUsePath.lineTo(mBezierEnd1.x, mBezierEnd1.y)
        if(flipPage == 0) {
            reUsePath.offset(-mRightPageLTPoint.x, 0f)
        }
        reUsePath.close()
        // 这个offset根据页面调整
        canvas.save()
        if(flipPage == 0) {
            canvas.translate(mRightPageLTPoint.x, mRightPageLTPoint.y)
            flipPath.offset(-mRightPageLTPoint.x, 0f)
        }
        canvas.clipPath(flipPath)
        canvas.clipPath(reUsePath)
        //mPaint.colorFilter = mColorMatrixFilter
        canvas.drawColor(bgColor)
        // 2. 绘制在白色区域
        // 以中间的两个点为圆心，旋转
        var pivotX: Float
        val pivotY: Float
        var de = 180f - 2 * mDegree
        // 计算旋转
        if(flipSide == TOP_SIDE) {
            pivotX = mRightPageLTPoint.x
            pivotY = mRightPageLTPoint.y
            if(flipPage == 1) {
                de = -(de)
            } else {
                pivotX -= mRightPageLTPoint.x
            }
        } else {
            pivotX = mRightPageLTPoint.x
            pivotY = mRightPageRBPoint.y
            if(flipPage == 0) {
                //mMatrix
                de = -de
                pivotX -= mRightPageLTPoint.x
            }
        }
        mMatrix.setRotate(de.toFloat(), pivotX, pivotY)

        val originArr = floatArrayOf(0f, 0f)
        val mapArr = floatArrayOf(0f, 0f)
        if(flipPage == 0) {
            if(flipSide == TOP_SIDE) {
                originArr[0] = mRightPageRBPoint.x - mRightPageLTPoint.x
                originArr[1] = mRightPageLTPoint.y
            } else {
                originArr[0] = mRightPageRBPoint.x - mRightPageLTPoint.x
                originArr[1] = mRightPageRBPoint.y
            }
        } else {
            if(flipSide == TOP_SIDE) {
                originArr[0] = mLeftPageLTPoint.x
                originArr[1] = mLeftPageLTPoint.y
            } else {
                originArr[0] = mLeftPageLTPoint.x
                originArr[1] = mLeftPageRBPoint.y
            }
        }
        mMatrix.mapPoints(mapArr, originArr)
        if(flipPage == 0) {
            mMatrix.postTranslate(mCurCornerPoint.x - mRightPageLTPoint.x - mapArr[0], mCurCornerPoint.y - mapArr[1])
        } else {
            mMatrix.postTranslate(mCurCornerPoint.x - mapArr[0], mCurCornerPoint.y - mapArr[1])
        }

        if(flipPage == 0) {
            mLeftMiddleBitmap?.let {
                canvas.drawBitmap(it, mMatrix, null)
            }
        } else {
            mRightMiddleBitmap?.let {
                canvas.drawBitmap(it, mMatrix, null)
            }
        }
        mPaint.colorFilter = null

        // 3. 设置阴影
        if(flipPage == 0) {
            canvas.translate(-mRightPageLTPoint.x, -mRightPageLTPoint.y)
        }
        val minDis = mTouchDis / 8
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        val left: Float
        val right: Float
        val bottom: Float
        val top: Float
        val rotateDegree: Float
        if(flipPage == 0) {
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
        if(flipPage == 0) {
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
        // 设置阴影的顶点
        val rad = Math.toRadians(mDegree)
        if (flipPage == 0) {
            outPoint.x = mCurCornerPoint.x + OUT_LEN * sin(rad).toFloat()
        } else {
            outPoint.x = mCurCornerPoint.x - OUT_LEN * sin(rad).toFloat()
        }
        if (flipSide == TOP_SIDE) {
            outPoint.y = mCurCornerPoint.y + OUT_LEN * cos(rad).toFloat()
        } else {
            outPoint.y = mCurCornerPoint.y - OUT_LEN * cos(rad).toFloat()
        }
        // 计算旋转的角度
        var deOne = Math.toDegrees(
            atan2(
                (mCurCornerPoint.x - mBezierControl1.x).toDouble(),
                (mBezierControl1.y - mCurCornerPoint.y).toDouble()
            )
        )
        if(deOne > 90){
            deOne -= 180f
        }

        // 绘制一半的阴影
        canvas.save()
        // 不同页面翻转的角度不一致
        reUsePath.reset()
        reUsePath.moveTo(outPoint.x, outPoint.y)
        reUsePath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
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
        canvas.rotate(deOne.toFloat(), outPoint.x, outPoint.y)
        val colors = shadowReverseColors
        val rightFirst = if(flipPage == 0) {
            (outPoint.x - cos(rad) * OUT_LEN - 1).toFloat()
        } else {
            (outPoint.x + cos(rad) * OUT_LEN + 1).toFloat()
        }
        val bottomFirst = if(flipSide == TOP_SIDE) {
            outPoint.y - abs(mOriginalCorner.x - mBezierControl1.x)
        } else {
            outPoint.y + abs(mOriginalCorner.x - mBezierControl1.x)
        }
        mPaint.shader = getGradient(outPoint.x, mBezierControl1.y, rightFirst, mBezierControl1.y, colors)
        canvas.drawRect(outPoint.x, outPoint.y, rightFirst, bottomFirst ,mPaint)
        canvas.restore()

        canvas.save()
        reUsePath.reset()
        reUsePath.moveTo(outPoint.x, outPoint.y)
        reUsePath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
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
        canvas.rotate(deOne.toFloat(), outPoint.x, outPoint.y)

        val secondColors = shadowReverseColors
        val secondBottom: Float = if(flipSide == TOP_SIDE) {
            (outPoint.y - abs(sin(rad)) * OUT_LEN).toFloat() - 1
        } else {
            (outPoint.y + abs(sin(rad)) * OUT_LEN).toFloat() + 1
        }
        val secondRight = if(flipPage == 0) {
            outPoint.x - abs(mOriginalCorner.y - mBezierControl2.y)
        } else {
            outPoint.x + abs(mOriginalCorner.y - mBezierControl2.y)
        }
        mPaint.shader = getGradient(outPoint.x, outPoint.y, outPoint.x, secondBottom, secondColors)
        canvas.drawRect(outPoint.x, outPoint.y, secondRight, secondBottom ,mPaint)
        canvas.restore()
    }

    private fun drawFlipPageBottomPageContent(canvas: Canvas) {
        canvas.save()
        // 绘制内容部分
        if(flipPage == 0) {
            reUsePath.reset()
            reUsePath.addRect(mLeftPageLTPoint.x, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageRBPoint.y, Path.Direction.CW)
            canvas.clipPath(reUsePath)
            canvas.clipPath(flipPath)
            mLeftBottomBitmap?.let {
                canvas.drawBitmap(it, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
            }
        } else if(flipPage == 1) {
            reUsePath.reset()
            reUsePath.addRect(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageRBPoint.x, mRightPageRBPoint.y, Path.Direction.CW)
            canvas.clipPath(reUsePath)
            canvas.clipPath(flipPath)
            mRightBottomBitmap?.let {
                canvas.drawBitmap(it, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
            }
        }
        // 绘制阴影
        val rotateDegree = if(flipPage == 0) {
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
        if(flipPage == 0) {
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
        } else if(flipPage == 1) {
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
        if (flipPage == 0) {
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
        if(flipPage == 0) {
            reUsePath.reset()
            reUsePath.moveTo(mRightPageLTPoint.x + r, mRightPageLTPoint.y)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageLTPoint.x + 2 * r, mRightPageLTPoint.y + 2 * r, -90f, -90f, false)
            reUsePath.lineTo(mRightPageLTPoint.x, mRightPageRBPoint.y - r)
            reUsePath.arcTo(mRightPageLTPoint.x, mRightPageRBPoint.y - 2 * r,mRightPageLTPoint.x + 2 * r, mRightPageRBPoint.y, -180f, -90f, false)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageRBPoint.y)
            reUsePath.lineTo(mRightPageRBPoint.x, mRightPageLTPoint.y)
            reUsePath.close()
            canvas.clipPath(reUsePath)
            mLeftMiddleBitmap?.let { b->
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
            mRightMiddleBitmap?.let { b->
                if(!b.isRecycled) {
                    canvas.drawBitmap(b, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
                }
            }
        }
        canvas.restore()
    }

    private fun calculatePoint() {
        mMiddlePoint.x = (mCurCornerPoint.x + mOriginalCorner.x) / 2
        mMiddlePoint.y = (mCurCornerPoint.y + mOriginalCorner.y) / 2
        // BezierControl1 需要做限制，因为不能超过两边页的中间点
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
            mBezierStart1.x = min(mBezierStart1.x, mLeftPageLTPoint.x)
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
        mBezierEnd1 = getCross(mCurCornerPoint, mBezierControl1, mBezierStart1, mBezierStart2)
        mBezierEnd2 = getCross(mCurCornerPoint, mBezierControl2, mBezierStart1, mBezierStart2)
        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4

        flipPath.reset()
        flipPath.moveTo(mBezierStart1.x, mBezierStart1.y)
        flipPath.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y)
        flipPath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
        flipPath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        flipPath.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y)
        flipPath.lineTo(mOriginalCorner.x, mOriginalCorner.y)
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
        Log.d("wangjie", "prePareDirection")
        // 确定方向
        when(direct) {
            DIRECT_TL -> {
                flipPage = 1
                flipSide = BOTTOM_SIDE
            }
            DIRECT_TR -> {
                flipPage = 0
                flipSide = BOTTOM_SIDE
            }
            DIRECT_BL -> {
                flipPage = 1
                flipSide = TOP_SIDE
            }
            DIRECT_BR -> {
                flipPage = 0
                flipSide = TOP_SIDE
            }
        }
        initCornerPoint()
    }

    private fun covertTouchPointToCurCornerPoint(x: Float, y: Float) {
        var targetTouchY = y
        // 1. 方向是否发生变更
        var offsetY = y - mStartPoint.y
        if(offsetY > 0) {
            if(flipSide == BOTTOM_SIDE) {
                flipSide = TOP_SIDE
                initCornerPoint()
            }
        } else if(offsetY < 0) {
            if(flipSide == TOP_SIDE) {
                flipSide = BOTTOM_SIDE
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