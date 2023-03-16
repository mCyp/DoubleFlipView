package com.example.myapplication.view.direction

import android.graphics.*
import android.os.Build
import com.example.myapplication.view.DeviceUtil
import com.example.myapplication.view.DirectDrawAction
import com.qidian.fonttest.view.OUT_LEN
import com.qidian.fonttest.view.TOP_SIDE
import kotlin.math.*

abstract class RightBaseDirectDrawAction: BaseDirectDrawAction() {

    override fun flipPage(): Int {
        return 1
    }

    override fun drawNoFlipSide(
        canvas: Canvas,
        reUsePath: Path,
        radius: Int
    ) {
        canvas.save()
        reUsePath.reset()
        reUsePath.moveTo(mLeftPageRBPoint.x - radius, mLeftPageLTPoint.y)
        reUsePath.arcTo( mLeftPageRBPoint.x - 2 * radius, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageLTPoint.y + 2 * radius, -90f, 90f, false)
        reUsePath.lineTo(mLeftPageRBPoint.x, mLeftPageRBPoint.y - radius)
        reUsePath.arcTo(mLeftPageRBPoint.x - 2 * radius, mLeftPageRBPoint.y - 2 * radius,mLeftPageRBPoint.x, mLeftPageRBPoint.y, 0f, 90f, false)
        reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageRBPoint.y)
        reUsePath.lineTo(mLeftPageLTPoint.x, mLeftPageLTPoint.y)
        reUsePath.close()
        canvas.clipPath(reUsePath)
        mLeftTopBitmap?.let { b->
            if(!b.isRecycled) {
                canvas.drawBitmap(b, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
            }
        }
        canvas.restore()
    }

    override fun drawFlipPageContent(
        canvas: Canvas,
        reUsePath: Path,
        flipPath: Path,
        r: Int
    ) {
        canvas.save()
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

        canvas.restore()
    }

    override fun drawBookMiddleArea(
        canvas: Canvas,
        reUsePath: Path,
        r: Int,
        mPaint: Paint
    ) {
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

    override fun drawFlipPageBottomPageContent(
        canvas: Canvas,
        reUsePath: Path,
        flipPath: Path,
        mDegree: Double,
        mBezierStart1: PointF,
        mBezierStart2: PointF,
        mPaint: Paint,
        mTouchDis: Float,
        per: Float,
        minDistance: Float
    ) {
        canvas.save()
        // 绘制内容部分
        reUsePath.reset()
        reUsePath.addRect(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageRBPoint.x, mRightPageRBPoint.y, Path.Direction.CW)
        canvas.clipPath(reUsePath)
        canvas.clipPath(flipPath)
        mRightBottomBitmap?.let {
            canvas.drawBitmap(it, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
        }
        // 绘制阴影
        val rotateDegree = if(flipSide() == TOP_SIDE) {
            -(90 - mDegree.toFloat())
        } else {
            (90 - mDegree.toFloat())
        }
        canvas.clipPath(flipPath)
        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        var left = 0f
        var right = 0f
        var top = 0f
        var bottom = 0f
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        left = mBezierStart1.x

        val minDis = if(context != null) DeviceUtil.dip2px(context!!, 20f) else 30
        right = left + (minDistance + max((mTouchDis / 4 - minDistance) * per * 0.2f, minDis.toFloat()))
        if(flipSide() == TOP_SIDE) {
            top = mBezierStart1.y
            bottom = mBezierStart1.y + rectHeight.toFloat()
        } else {
            bottom = mBezierStart1.y
            top = mBezierStart1.y - rectHeight.toFloat()
        }
        mPaint.shader = getGradient(left, top, right, top, shadowColors)
        canvas.drawRect(left, top, right, bottom, mPaint)
        canvas.restore()
    }

    override fun drawBackContentAndShadow(
        canvas: Canvas,
        reUsePath: Path,
        flipPath: Path,
        mBezierVertex1: PointF,
        mBezierVertex2: PointF,
        mBezierEnd2: PointF,
        mBezierEnd1: PointF,
        mCurCornerPoint: PointF,
        mDegree: Double,
        mMatrix: Matrix,
        mPaint: Paint,
        mBezierStart1: PointF,
        mBezierStart2: PointF,
        mTouchDis: Float
    ) {
        // 旋转 + 平移
        // 1. 限制绘制翻开白区域
        reUsePath.reset()
        reUsePath.moveTo(mBezierVertex1.x, mBezierVertex1.y)
        reUsePath.lineTo(mBezierVertex2.x, mBezierVertex2.y)
        reUsePath.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        reUsePath.lineTo(mCurCornerPoint.x, mCurCornerPoint.y)
        reUsePath.lineTo(mBezierEnd1.x, mBezierEnd1.y)
        reUsePath.close()
        // 这个offset根据页面调整
        canvas.save()

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
        if(flipSide() == TOP_SIDE) {
            pivotX = mRightPageLTPoint.x
            pivotY = mRightPageLTPoint.y
            de = -(de)
        } else {
            pivotX = mRightPageLTPoint.x
            pivotY = mRightPageRBPoint.y
        }
        mMatrix.setRotate(de.toFloat(), pivotX, pivotY)

        val originArr = floatArrayOf(0f, 0f)
        val mapArr = floatArrayOf(0f, 0f)
        if(flipSide() == TOP_SIDE) {
            originArr[0] = mLeftPageLTPoint.x
            originArr[1] = mLeftPageLTPoint.y
        } else {
            originArr[0] = mLeftPageLTPoint.x
            originArr[1] = mLeftPageRBPoint.y
        }
        mMatrix.mapPoints(mapArr, originArr)
        mMatrix.postTranslate(mCurCornerPoint.x - mapArr[0], mCurCornerPoint.y - mapArr[1])

        mRightMiddleBitmap?.let {
            canvas.drawBitmap(it, mMatrix, null)
        }
        mPaint.colorFilter = null

        // 3. 设置阴影

        val minDis = mTouchDis / 8
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        val left: Float
        val right: Float
        val bottom: Float
        val top: Float
        val rotateDegree: Float
        left = mBezierStart1.x - 1
        right = mBezierVertex1.x + 1
        if(flipSide() == TOP_SIDE) {
            top = mBezierStart1.y
            bottom = top + rectHeight.toFloat()
            rotateDegree = -(90 - mDegree.toFloat())
        } else {
            bottom = mBezierStart1.y
            top = bottom - rectHeight.toFloat()
            rotateDegree = (90 - mDegree.toFloat())
        }
        mPaint.shader = getGradient(left, mBezierStart1.y, right, mBezierStart1.y, shadowReverseColors)
        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        //canvas.drawRect(left, top, right, bottom, mPaint)
        canvas.restore()
    }
}