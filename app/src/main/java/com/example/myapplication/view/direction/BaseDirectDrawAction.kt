package com.example.myapplication.view.direction

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.Log
import com.example.myapplication.view.DirectDrawAction
import com.qidian.fonttest.view.OUT_LEN
import com.qidian.fonttest.view.TOP_SIDE
import kotlin.math.*


abstract class BaseDirectDrawAction: DirectDrawAction {

    val shadowColors = intArrayOf(-0x4f99999a, 0x666666)
    val shadowReverseColors = intArrayOf(0x666666, -0x4f99999a)

    var mLeftBottomBitmap: Bitmap? = null
    var mLeftMiddleBitmap: Bitmap? = null
    var mLeftTopBitmap: Bitmap? = null
    var mRightTopBitmap: Bitmap? = null
    var mRightMiddleBitmap: Bitmap? = null
    var mRightBottomBitmap: Bitmap? = null
    lateinit var mLeftPageLTPoint: PointF
    lateinit var mLeftPageRBPoint: PointF
    lateinit var mRightPageLTPoint: PointF
    lateinit var mRightPageRBPoint: PointF

    var context: Context? = null
    var bgColor: Int = 0


    abstract fun flipSide(): Int

    abstract fun flipPage(): Int

    override fun drawTwoSideShadow(
        canvas: Canvas,
        reUsePath: Path,
        flipPath: Path,
        mDegree: Double,
        mCurCornerPoint: PointF,
        mBezierControl1: PointF,
        mBezierStart1: PointF,
        mBezierControl2: PointF,
        mBezierStart2: PointF,
        mOriginalCorner: PointF,
        mPaint: Paint
    ) {
        // 这里写的很那看，后续优化一下
        val outPoint = PointF()
        // 计算旋转的角度
        var deOne = Math.toDegrees(
            atan2(
                (mBezierControl1.y - mCurCornerPoint.y).toDouble(),
                (mCurCornerPoint.x - mBezierControl1.x).toDouble()
            )
        )
        if(flipPage() == 0) {
            if(flipSide() == TOP_SIDE) {
                deOne = abs(deOne)
            }
        } else {
            if(flipSide() == TOP_SIDE) {
                deOne = 180 - abs(deOne)
            } else {
                deOne = 180 - deOne
            }
        }
        // 设置阴影的顶点
        val rad = Math.toRadians(deOne - 45f)
        // 求顶点
        if (flipPage() == 0) {
            outPoint.x = mCurCornerPoint.x + OUT_LEN * 1.414f * cos(rad).toFloat()
        } else {
            outPoint.x = mCurCornerPoint.x - OUT_LEN * 1.414f * cos(rad).toFloat()
        }
        if (flipSide() == TOP_SIDE) {
            outPoint.y = mCurCornerPoint.y + OUT_LEN * sin(rad).toFloat()
        } else {
            outPoint.y = mCurCornerPoint.y - OUT_LEN * sin(rad).toFloat()
        }

        if(flipPage() == 0) {
            if(flipSide() == TOP_SIDE) {
                deOne -= 90
            } else {
                deOne = 90 - deOne
            }
        } else {
            if(flipSide() == TOP_SIDE) {
                deOne = 90 - deOne
            } else {
                deOne -= 90
            }
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
        val rightFirst = if(flipPage() == 0) {
            (outPoint.x -  OUT_LEN)
        } else {
            (outPoint.x +  OUT_LEN)
        }
        val bottomFirst = if(flipSide() == TOP_SIDE) {
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
        val secondBottom: Float = if(flipSide() == TOP_SIDE) {
            outPoint.y - OUT_LEN
        } else {
            outPoint.y +  OUT_LEN
        }
        val secondRight = if(flipPage() == 0) {
            outPoint.x - abs(mOriginalCorner.y - mBezierControl2.y)
        } else {
            outPoint.x + abs(mOriginalCorner.y - mBezierControl2.y)
        }
        mPaint.shader = getGradient(outPoint.x, outPoint.y, outPoint.x, secondBottom, secondColors)
        canvas.drawRect(outPoint.x, outPoint.y, secondRight, secondBottom ,mPaint)
        canvas.restore()
    }

    @Suppress("SameParameterValue")
    fun getGradient(l: Float, t: Float, r: Float, b: Float, colors: IntArray): LinearGradient {
        return LinearGradient(l, t, r, b, colors, floatArrayOf(0f, 1.0f), Shader.TileMode.CLAMP)
    }
}