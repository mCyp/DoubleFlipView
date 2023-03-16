package com.example.myapplication.view

import android.graphics.*

interface DirectDrawAction {
    fun drawNoFlipSide(canvas: Canvas, reUsePath: Path, radius: Int)
    fun drawFlipPageContent(canvas: Canvas, reUsePath: Path, flipPath: Path, r: Int)
    fun drawBookMiddleArea(canvas: Canvas, reUsePath: Path, r: Int, mPaint: Paint)
    fun drawFlipPageBottomPageContent(canvas: Canvas, reUsePath: Path, flipPath: Path, mDegree: Double, mBezierStart1: PointF, mBezierStart2: PointF, mPaint: Paint, mTouchDis: Float, per: Float, minDis: Float)
    fun drawTwoSideShadow(canvas: Canvas, reUsePath: Path, flipPath: Path, mDegree: Double, mCurCornerPoint: PointF, mBezierControl1: PointF, mBezierStart1: PointF, mBezierControl2: PointF, mBezierStart2: PointF, mOriginalCorner: PointF, mPaint: Paint)
    fun drawBackContentAndShadow(canvas: Canvas, reUsePath: Path, flipPath: Path, mBezierVertex1: PointF, mBezierVertex2: PointF, mBezierEnd2: PointF, mBezierEnd1: PointF, mCurCornerPoint: PointF, mDegree: Double, mMatrix: Matrix, mPaint: Paint, mBezierStart1: PointF, mBezierStart2: PointF, mTouchDis: Float)
}

