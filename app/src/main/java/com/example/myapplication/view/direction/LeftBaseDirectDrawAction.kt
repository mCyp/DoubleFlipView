package com.example.myapplication.view.direction

import android.graphics.*
import android.os.Build
import com.example.myapplication.view.DeviceUtil
import com.qidian.fonttest.view.TOP_SIDE
import kotlin.math.*

abstract class LeftBaseDirectDrawAction: BaseDirectDrawAction() {

    override fun flipPage(): Int {
        return 0
    }

    override fun drawNoFlipSide(
        canvas: Canvas,
        reUsePath: Path,
        radius: Int
    ) {
        canvas.save()
        reUsePath.reset()
        reUsePath.moveTo(mRightPageLTPoint.x + radius, mRightPageLTPoint.y)
        reUsePath.arcTo(mRightPageLTPoint.x, mRightPageLTPoint.y, mRightPageLTPoint.x + 2 * radius, mRightPageLTPoint.y + 2 * radius, -90f, -90f, false)
        reUsePath.lineTo(mRightPageLTPoint.x, mRightPageRBPoint.y - radius)
        reUsePath.arcTo(mRightPageLTPoint.x, mRightPageRBPoint.y - 2 * radius,mRightPageLTPoint.x + 2 * radius, mRightPageRBPoint.y, -180f, -90f, false)
        reUsePath.lineTo(mRightPageRBPoint.x, mRightPageRBPoint.y)
        reUsePath.lineTo(mRightPageRBPoint.x, mRightPageLTPoint.y)
        reUsePath.close()
        canvas.clipPath(reUsePath)
        mRightTopBitmap?.let { b->
            if(!b.isRecycled) {
                canvas.drawBitmap(b, mRightPageLTPoint.x, mRightPageLTPoint.y, null)
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
        minDis: Float
    ) {
        canvas.save()
        reUsePath.reset()
        reUsePath.addRect(mLeftPageLTPoint.x, mLeftPageLTPoint.y, mLeftPageRBPoint.x, mLeftPageRBPoint.y, Path.Direction.CW)
        canvas.clipPath(reUsePath)
        canvas.clipPath(flipPath)
        mLeftBottomBitmap?.let {
            canvas.drawBitmap(it, mLeftPageLTPoint.x, mLeftPageLTPoint.y, null)
        }
        // 绘制阴影
        val rotateDegree = if(flipSide() == TOP_SIDE) {
            (90 - mDegree.toFloat())
        } else {
            -(90 - mDegree.toFloat())
        }
        canvas.clipPath(flipPath)
        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        val left: Float
        var top = 0f
        var bottom = 0f
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        val right: Float = mBezierStart1.x

        val minDistance = if(context != null) DeviceUtil.dip2px(context!!, 20f) else 30
        left = right - (minDis + max((mTouchDis / 4 - minDis) * per * 0.2f, minDistance.toFloat()))
        if(flipSide() == TOP_SIDE) {
            top = mBezierStart1.y
            bottom = mBezierStart1.y + rectHeight.toFloat()
        } else {
            bottom = mBezierStart1.y
            top = mBezierStart1.y - rectHeight.toFloat()
        }
        mPaint.shader = getGradient(left, top, right, top, shadowReverseColors)
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
        // flip 0 特有
        reUsePath.offset(-mRightPageLTPoint.x, 0f)
        reUsePath.close()
        // 这个offset根据页面调整
        canvas.save()
        // flip 0 特有
        canvas.translate(mRightPageLTPoint.x, mRightPageLTPoint.y)
        flipPath.offset(-mRightPageLTPoint.x, 0f)

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
            pivotX -= mRightPageLTPoint.x
        } else {
            pivotX = mRightPageLTPoint.x
            pivotY = mRightPageRBPoint.y
            //mMatrix
            de = -de
            pivotX -= mRightPageLTPoint.x
        }
        mMatrix.setRotate(de.toFloat(), pivotX, pivotY)

        val originArr = floatArrayOf(0f, 0f)
        val mapArr = floatArrayOf(0f, 0f)
        if(flipSide() == TOP_SIDE) {
            originArr[0] = mRightPageRBPoint.x - mRightPageLTPoint.x
            originArr[1] = mRightPageLTPoint.y
        } else {
            originArr[0] = mRightPageRBPoint.x - mRightPageLTPoint.x
            originArr[1] = mRightPageRBPoint.y
        }
        mMatrix.mapPoints(mapArr, originArr)
        mMatrix.postTranslate(mCurCornerPoint.x - mRightPageLTPoint.x - mapArr[0], mCurCornerPoint.y - mapArr[1])
        mLeftMiddleBitmap?.let {
            canvas.drawBitmap(it, mMatrix, null)
        }
        mPaint.colorFilter = null

        // 3. 绘制背部的阴影
        canvas.translate(-mRightPageLTPoint.x, -mRightPageLTPoint.y)
        val rectHeight = hypot((mBezierStart1.x - mBezierStart2.x).toDouble(), (mBezierStart1.y - mBezierStart2.y).toDouble())
        val left: Float
        val right: Float
        val bottom: Float
        val top: Float
        val rotateDegree: Float
        // 绘制在BezierControl
        left = mBezierVertex1.x - 1
        right = mBezierStart1.x + 1
        if(flipSide() == TOP_SIDE) {
            top = mBezierStart1.y
            bottom = top + rectHeight.toFloat()
            rotateDegree = (90 - mDegree.toFloat())
        } else {
            bottom = mBezierStart1.y
            top = bottom - rectHeight.toFloat()
            rotateDegree = -(90 - mDegree.toFloat())
        }
        mPaint.shader = getGradient(left, mBezierStart1.y, right, mBezierStart1.y, shadowColors)
        canvas.rotate(rotateDegree, mBezierStart1.x, mBezierStart1.y)
        canvas.drawRect(left, top, right, bottom, mPaint)
        canvas.restore()
    }
}