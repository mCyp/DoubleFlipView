package com.example.myapplication.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.view.View;


/**
 * Created by huangzhaoyi on 2016/2/25.
 */
public class QDRealFlipImpl extends View {
    private boolean mExistPage;
    private int mWidth;
    private int mHeight;

    private PointF mTouch = new PointF(); // 拖拽点
    private int mCornerX = 0; // 拖拽点对应的页脚
    private int mCornerY = 0;
    private boolean mIsRTAndLB; // 是否属于右上左下
    private Bitmap mCurrentBitmap;
    private Bitmap mNextBitmap;
    private Paint mPaint;
    private int mBgColor;

    private Path mPath0;
    private Path mPath1;
    private PointF mBezierStart1 = new PointF(); // 贝塞尔曲线起始点
    private PointF mBezierControl1 = new PointF(); // 贝塞尔曲线控制点
    private PointF mBezierVertex1 = new PointF(); // 贝塞尔曲线顶点
    private PointF mBezierEnd1 = new PointF(); // 贝塞尔曲线结束点
    private PointF mBezierStart2 = new PointF(); // 另一条贝塞尔曲线
    private PointF mBezierControl2 = new PointF();
    private PointF mBezierVertex2 = new PointF();
    private PointF mBezierEnd2 = new PointF();

    private float mMiddleX;
    private float mMiddleY;
    private float mDegrees;
    private float mTouchToCornerDis;
    private ColorMatrixColorFilter mColorMatrixFilter;
    private Matrix mMatrix;
    private float[] mMatrixArray = {0, 0, 0, 0, 0, 0, 0, 0, 1.0f};
    private float mMaxLength;

    private int[] mFrontBackShadowColors;
    private int[] mFrontBackShadowReverseColors;
    private int[] mBackShadowColors;
    private int[] mBackShadowReverseColors;
    private int[] mFrontShadowColors;
    private int[] mFrontShadowReverseColors;

    // 编辑模式
    private static final float FACTOR = 1.0f; // 放大倍数
    private Paint mMagnifierPaint;
    private ShapeDrawable mMagnifierDrawable;
    private Matrix mMagnifierDrawableMatrix = new Matrix();
    // private QDBookMarkItem mSelectedItem;
    private float mEditModeTouchX;
    private float mEditModeTouchY;
    private int mRadius = 90;
    private boolean mIsEditMode;
    private boolean mIsEditModeDrawMagnifier;
    private int mTranslationY = 0;//平移中

    public QDRealFlipImpl(Context context, int width, int height) {
        super(context);
        this.mWidth = width;
        this.mHeight = height;
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.drawColor(mBgColor);
        if (mIsEditMode) {
            drawEditModeArea(canvas, mCurrentBitmap);
            if (mIsEditModeDrawMagnifier) {
                drawEditModeMagnifier(canvas);
            }
        } else {
            if (mTranslationY != 0) {
                drawScrollBody(canvas);
            } else {
                drawBody(canvas);
            }
        }
    }

    private void drawBody(Canvas canvas) {
        if (!mExistPage) {
            resetXY();
        } else {
            calcPoints();
        }
        drawCurrentPageArea(canvas, mCurrentBitmap);
        drawNextPageAreaAndShadow(canvas, mNextBitmap);
        drawCurrentPageShadow(canvas);
        drawCurrentBackArea(canvas, mCurrentBitmap);
    }

    private void init() {
        this.mMaxLength = (float) Math.hypot(mWidth, mHeight);
        this.mPath0 = new Path();
        this.mPath1 = new Path();
        this.mPaint = new Paint();
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setDither(true);
        this.mPaint.setAntiAlias(true);

        ColorMatrix cm = new ColorMatrix();
        float array[] = {0.55f, 0, 0, 0, 80.0f, 0, 0.55f, 0, 0, 80.0f, 0, 0, 0.55f, 0, 80.0f, 0, 0, 0, 0.2f, 0};
        cm.set(array);
        this.mColorMatrixFilter = new ColorMatrixColorFilter(cm);
        this.mMatrix = new Matrix();
        // 不让x,y为0,否则在点计算时会有问题
        this.mTouch.x = 0.01f;
        this.mTouch.y = 0.01f;
        createDrawable();

        this.mMagnifierPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mMagnifierPaint.setStyle(Paint.Style.STROKE);
        this.mMagnifierPaint.setColor(Color.WHITE);
        this.mMagnifierPaint.setStrokeWidth(dip2px(3));
        this.mRadius = dip2px(45);
    }

    /**
     * 创建阴影的GradientDrawable
     */
    private void createDrawable() {
        mFrontBackShadowColors = new int[]{0x666666, 0xb0666666};
        mFrontBackShadowReverseColors = new int[]{0xb0666666, 0x666666};
        mBackShadowColors = new int[]{0xff333333, 0x333333};
        mBackShadowReverseColors = new int[]{0x333333, 0xff333333};
        mFrontShadowColors = new int[]{0x33666666, 0x00666666};
        mFrontShadowReverseColors = new int[]{0x00666666, 0x33666666};
    }

    /**
     * 画内容
     *
     * @param canvas
     * @param bitmap
     */
    private void drawCurrentPageArea(final Canvas canvas, Bitmap bitmap) {
        mPath0.reset();
        mPath0.moveTo(mBezierStart1.x, mBezierStart1.y);
        mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y);
        mPath0.lineTo(mTouch.x, mTouch.y);
        mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y);
        mPath0.lineTo(mCornerX, mCornerY);
        mPath0.close();

        canvas.save();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath0);
            } else {
                canvas.clipPath(mPath0, Region.Op.XOR);
            }
        } catch (Exception e) {
            // Logger.exception(e);
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        canvas.restore();
    }

    /**
     * 绘制翻起页背面
     */
    private void drawCurrentBackArea(Canvas canvas, Bitmap bitmap) {
        int i = (int) (mBezierStart1.x + mBezierControl1.x) / 2;
        float f1 = Math.abs(i - mBezierControl1.x);
        int i1 = (int) (mBezierStart2.y + mBezierControl2.y) / 2;
        float f2 = Math.abs(i1 - mBezierControl2.y);
        float f3 = Math.min(f1, f2);
        mPath1.reset();
        mPath1.moveTo(mBezierVertex2.x, mBezierVertex2.y);
        mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y);
        mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y);
        mPath1.close();
        int left;
        int right;
        if (mIsRTAndLB) {
            left = (int) (mBezierStart1.x - 1);
            right = (int) (mBezierStart1.x + f3 + 1);
        } else {
            left = (int) (mBezierStart1.x - f3 - 1);
            right = (int) (mBezierStart1.x + 1);
        }
        canvas.save();
        try {
            canvas.clipPath(mPath0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipPath(mPath1);
            } else {
                canvas.clipPath(mPath1, Region.Op.INTERSECT);
            }
        } catch (Exception e) {
            // Logger.exception(e);
        }
        mPaint.setColorFilter(mColorMatrixFilter);

        canvas.drawColor(mBgColor);

        float dis = (float) Math.hypot(mCornerX - mBezierControl1.x, mBezierControl2.y - mCornerY);
        float f8 = (mCornerX - mBezierControl1.x) / dis;
        float f9 = (mBezierControl2.y - mCornerY) / dis;

        mMatrixArray[0] = 1 - 2 * f9 * f9;
        mMatrixArray[1] = 2 * f8 * f9;
        mMatrixArray[3] = mMatrixArray[1];
        mMatrixArray[4] = 1 - 2 * f8 * f8;
        mMatrix.reset();
        mMatrix.setValues(mMatrixArray);
        mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y);
        mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y);
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, mMatrix, mPaint);
        }
        mPaint.setColorFilter(null);
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        if(mIsRTAndLB) {
            mPaint.setShader(getGradient(left, (int) mBezierStart1.y, right, (int) mBezierStart1.y, mFrontBackShadowColors));
        } else {
            mPaint.setShader(getGradient(left, (int) mBezierStart1.y, right, (int) mBezierStart1.y, mFrontBackShadowReverseColors));
        }
        canvas.drawRect(left, (int) mBezierStart1.y, right, (int) (mBezierStart1.y + mMaxLength), mPaint);
        canvas.restore();
    }

    /**
     * 绘制翻起页的阴影
     */
    private void drawCurrentPageShadow(Canvas canvas) {
        double degree;
        if (mIsRTAndLB) {
            degree = Math.PI / 4 - Math.atan2(mBezierControl1.y - mTouch.y, mTouch.x - mBezierControl1.x);
        } else {
            degree = Math.PI / 4 - Math.atan2(mTouch.y - mBezierControl1.y, mTouch.x - mBezierControl1.x);
        }
        // 翻起页阴影顶点与touch点的距离
        double d1 = (float) 25 * 1.414 * Math.cos(degree);
        double d2 = (float) 25 * 1.414 * Math.sin(degree);
        float x = (float) (mTouch.x + d1);
        float y;
        if (mIsRTAndLB) {
            y = (float) (mTouch.y + d2);
        } else {
            y = (float) (mTouch.y - d2);
        }

        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl1.x, mBezierControl1.y);
        mPath1.lineTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.close();
        float rotateDegrees;
        canvas.save();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath0);
            } else {
                canvas.clipPath(mPath0, Region.Op.XOR);
            }
            canvas.clipPath(mPath1, Region.Op.INTERSECT);
        } catch (Exception e) {
            // Logger.exception(e);
        }
        int leftX;
        int rightX;
        if (mIsRTAndLB) {
            leftX = (int) (mBezierControl1.x);
            rightX = (int) mBezierControl1.x + 25;
        } else {
            leftX = (int) (mBezierControl1.x - 25);
            rightX = (int) mBezierControl1.x + 1;
        }
        rotateDegrees = (float) Math.toDegrees(Math.atan2(mTouch.x - mBezierControl1.x, mBezierControl1.y - mTouch.y));
        canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y);
        if(mIsRTAndLB) {
            mPaint.setShader(getGradient(leftX, (int) (mBezierControl1.y), rightX, (int) (mBezierControl1.y), mFrontShadowColors));
        } else {
            mPaint.setShader(getGradient(leftX, (int) (mBezierControl1.y), rightX, (int) (mBezierControl1.y), mFrontShadowReverseColors));
        }
        canvas.drawPaint(mPaint);
        canvas.restore();

        mPath1.reset();
        mPath1.moveTo(x, y);
        mPath1.lineTo(mTouch.x, mTouch.y);
        mPath1.lineTo(mBezierControl2.x, mBezierControl2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.close();
        canvas.save();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath0);
            } else {
                canvas.clipPath(mPath0, Region.Op.XOR);
            }
            canvas.clipPath(mPath1);
        } catch (Exception e) {
            // Logger.exception(e);
        }
        int topX, bottomX;
        if (mIsRTAndLB) {
            topX = (int) (mBezierControl2.y);
            bottomX = (int) (mBezierControl2.y + 25);
        } else {
            topX = (int) (mBezierControl2.y - 25);
            bottomX = (int) (mBezierControl2.y + 1);
        }
        rotateDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl2.y - mTouch.y, mBezierControl2.x - mTouch.x));
        canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y);
        if(mIsRTAndLB) {
            mPaint.setShader(getGradient(0, topX, 0, bottomX, mFrontShadowColors));
        } else {
            mPaint.setShader(getGradient(0, topX, 0, bottomX, mFrontShadowReverseColors));
        }
        canvas.drawPaint(mPaint);
        canvas.restore();
    }

    public int mLeftX = 1;//用来判断动画是否到边缘，解决一个Scroller的bug
    public int mRightX = 1;

    /**
     * 绘制下页界面区域和阴影
     */
    private void drawNextPageAreaAndShadow(Canvas canvas, Bitmap bitmap) {
        mPath1.reset();
        mPath1.moveTo(mBezierStart1.x, mBezierStart1.y);
        mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y);
        mPath1.lineTo(mBezierVertex2.x, mBezierVertex2.y);
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y);
        mPath1.lineTo(mCornerX, mCornerY);
        mPath1.close();
        mDegrees = (float) Math.toDegrees(Math.atan2(mBezierControl1.x - mCornerX, mBezierControl2.y - mCornerY));
        int leftX;
        int rightX;
        if (mIsRTAndLB) {
            leftX = (int) (mBezierStart1.x);
            rightX = (int) (mBezierStart1.x + mTouchToCornerDis / 4);
        } else {
            leftX = (int) (mBezierStart1.x - mTouchToCornerDis / 4);
            rightX = (int) mBezierStart1.x;
        }
        //保存这两个值
        mLeftX = leftX;
        mRightX = rightX;

        canvas.save();
        try {
            canvas.clipPath(mPath0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipPath(mPath1);
            } else {
                canvas.clipPath(mPath1, Region.Op.INTERSECT);
            }
        } catch (Exception e) {
            // Logger.exception(e);
        }
        // mPaint.setSha
        //只截一张图，则底部的图不需要绘制了
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y);
        if(mIsRTAndLB) {
            mPaint.setShader(getGradient(leftX, (int) mBezierStart1.y, rightX, (int) mBezierStart1.y, mBackShadowColors));
        } else {
            mPaint.setShader(getGradient(leftX, (int) mBezierStart1.y, rightX, (int) mBezierStart1.y, mBackShadowReverseColors));
        }
        canvas.drawRect(leftX, (int) mBezierStart1.y, rightX, (int) (mMaxLength + mBezierStart1.y), mPaint);
        canvas.restore();
    }

    private LinearGradient getGradient(int l, int t, int r, int b, int[] colors) {
        return new LinearGradient(l, t, r, b, colors, new float[]{0f, 1.0f}, Shader.TileMode.CLAMP);
    }

    /**
     * 重置所有计算变量
     */
    private void calcPointsReset() {
        mMiddleX = 0;
        mMiddleY = 0;
        mBezierControl1.x = 0;
        mBezierControl1.y = 0;
        mBezierControl2.x = 0;
        mBezierControl2.y = 0;

        mBezierStart1.x = 0;
        mBezierStart1.y = 0;

        mBezierStart2.x = 0;
        mBezierStart2.y = 0;

        mTouchToCornerDis = 0;
        mBezierEnd1 = new PointF();
        mBezierEnd2 = new PointF();
        mBezierVertex1.x = 0;
        mBezierVertex1.y = 0;
        mBezierVertex2.x = 0;
        mBezierVertex2.y = 0;

        mCornerX = 0;
        mCornerY = 0;
    }

    /**
     * 计算坐标
     */
    public void calcPoints() {
        mMiddleX = (mTouch.x + mCornerX) / 2;
        mMiddleY = (mTouch.y + mCornerY) / 2;
        mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
        mBezierControl1.y = mCornerY;
        mBezierControl2.x = mCornerX;
        mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
        mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
        mBezierStart1.y = mCornerY;

        // 当mBezierStart1.x < 0或者mBezierStart1.x > 480时
        // 如果继续翻页，会出现BUG故在此限制
        if (mTouch.x > 0 && mTouch.x < mWidth) {
            if (mBezierStart1.x < 0 || mBezierStart1.x > mWidth) {
                if (mBezierStart1.x < 0) {
                    mBezierStart1.x = mWidth - mBezierStart1.x;
                }
                float f1 = Math.abs(mCornerX - mTouch.x);
                float f2 = mWidth * f1 / mBezierStart1.x;
                mTouch.x = Math.abs(mCornerX - f2);

                float f3 = Math.abs(mCornerX - mTouch.x) * Math.abs(mCornerY - mTouch.y) / f1;
                mTouch.y = Math.abs(mCornerY - f3);
                mMiddleX = (mTouch.x + mCornerX) / 2;
                mMiddleY = (mTouch.y + mCornerY) / 2;

                mBezierControl1.x = mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX);
                mBezierControl1.y = mCornerY;

                mBezierControl2.x = mCornerX;
                mBezierControl2.y = mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY);
                mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2;
            }
        }
        mBezierStart2.x = mCornerX;
        mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y) / 2;

        mTouchToCornerDis = (float) Math.hypot((mTouch.x - mCornerX), (mTouch.y - mCornerY));
        mBezierEnd1 = getCross(mTouch, mBezierControl1, mBezierStart1, mBezierStart2);
        mBezierEnd2 = getCross(mTouch, mBezierControl2, mBezierStart1, mBezierStart2);

        /*
         * mBezierVertex1.x 推导
         * ((mBezierStart1.x+mBezierEnd1.x)/2+mBezierControl1.x)/2 化简等价于
         * (mBezierStart1.x+ 2*mBezierControl1.x+mBezierEnd1.x) / 4
         */
        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4;
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4;
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4;
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4;
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐标
     */
    private PointF getCross(PointF P1, PointF P2, PointF P3, PointF P4) {
        PointF CrossP = new PointF();
        // 二元函数通式： y=ax+b
        float a1 = (P2.y - P1.y) / (P2.x - P1.x);
        float b1 = ((P1.x * P2.y) - (P2.x * P1.y)) / (P1.x - P2.x);

        float a2 = (P4.y - P3.y) / (P4.x - P3.x);
        float b2 = ((P3.x * P4.y) - (P4.x * P3.y)) / (P3.x - P4.x);
        CrossP.x = (b2 - b1) / (a1 - a2);
        CrossP.y = a1 * CrossP.x + b1;
        return CrossP;
    }

    /**
     * 计算起始向限
     *
     * @param x
     * @param y
     */
    public void calcCornerXY(float x, float y) {
        // 计算Y轴向限
        if (y <= mHeight / 2) {
            mCornerY = 0;
        } else {
            mCornerY = mHeight;
        }

        // 这里统一为宽度，以满足我们的需求
        mCornerX = mWidth;

        if ((mCornerX == 0 && mCornerY == mHeight) || (mCornerX == mWidth && mCornerY == 0)) {
            mIsRTAndLB = true;
        } else {
            mIsRTAndLB = false;
        }
    }

    /**
     * 滑动时重新计算拖拽点
     */
    public void reCalcPoints(float x, float y) {
        calcCornerXY(x, y);
        setTouchXY(x, y);
        calcPoints();
    }

    /**
     * 计算动画执行距离
     *
     * @param isNextFlip
     * @return
     */
    public int[] calcAnimDistance(boolean isNextFlip) {
        int dx = 0, dy = 0;
        if (isNextFlip) {
            if (mCornerX > 0) {
                dx = -(int) (mWidth + mTouch.x) + 1;
            } else {
                dx = (int) (mWidth - mTouch.x + mWidth) - 1;
            }
            if (mCornerY > 0) {
                dy = mHeight - (int) mTouch.y - 1;
            } else {
                dy = (int) (1 - mTouch.y) + 1; // 防止mTouch.y最终变为0
            }
        } else {
            if (mCornerX > 0) {
                dx = (int) (mWidth - mTouch.x) + 1;
            } else {
                dx = (int) (1 - mTouch.x);
            }
            if (mCornerY > 0) {
                dy = mHeight - (int) mTouch.y;
            } else {
                dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
            }
        }
        return new int[]{dx, dy};
    }

    /**
     * 计算ReturnBack动画执行距离
     *
     * @param isBack
     * @return
     */
    public int[] calcAnimReturnBackDistance(boolean isBack) {
        int dx, dy;
        if (!isBack) {
            if (mCornerX > 0) {
                dx = -(int) (mWidth + mTouch.x) + 1;
            } else {
                dx = (int) (mWidth - mTouch.x + mWidth);
            }
            if (mCornerY > 0) {
                dy = mHeight - (int) mTouch.y;
            } else {
                dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
            }
        } else {
            if (mCornerX > 0) {
                dx = (int) (mWidth - mTouch.x) + 1;
            } else {
                // dx = (int) mTouch.x - 1;
                dx = (int) (1 - mTouch.x);
            }
            if (mCornerY > 0) {
                dy = mHeight - (int) mTouch.y;
            } else {
                dy = (int) (1 - mTouch.y); // 防止mTouch.y最终变为0
            }
        }
        return new int[]{dx, dy};
    }

    public void resetXY() {
        this.mTouch.x = 0;
        this.mTouch.y = 0;
        calcPointsReset();
    }

    public void setTouchXY(float x, float y) {
        this.mTouch.set(x, y);
    }

    /**
     * 计算动画初始值
     *
     * @param x
     * @param y
     */
    public void calcParams(float x, float y) {
        calcCornerXY(x, y);
        setTouchXY(x, y);
    }

    /**
     * 计算ReturnBack动画初始值
     *
     * @param x
     * @param y
     * @param startX
     */
    public void calcReturnBackParams(float x, float y, float startX) {
        calcCornerXY(startX, y);
        setTouchXY(x, y);
    }

    //region # 编辑模式

    /**
     * 初始化编辑模式
     */
    public void initEditMode(float editX, float editY) {
        this.mIsEditMode = true;
        this.mIsEditModeDrawMagnifier = true;
        this.mEditModeTouchX = editX;
        this.mEditModeTouchY = editY;
        // this.mSelectedItem = selectedItem;
    }

    /**
     * 退出选择模式
     */
    public void cancelMagnifier() {
        this.mIsEditMode = true;
        this.mIsEditModeDrawMagnifier = false;
    }

    /**
     * 退出编辑模式
     */
    public void cancelEditMode() {
        this.mIsEditMode = false;
        this.mIsEditModeDrawMagnifier = false;
        resetXY();
    }

    /**
     * 创建编辑模式下放大的图片
     */
    private void createMagnifierDrawable(Bitmap bitmap) {
        try {
            if (bitmap == null || bitmap.isRecycled())
                return;
            Bitmap shaderBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * (int) FACTOR, bitmap.getHeight() * (int) FACTOR, true);
            if (shaderBitmap == null || shaderBitmap.isRecycled()) {
                return;
            }
            BitmapShader shader = new BitmapShader(shaderBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            if (shader != null) {
                this.mMagnifierDrawable = new ShapeDrawable(new OvalShape());
                this.mMagnifierDrawable.getPaint().setShader(shader);
                this.mMagnifierDrawable.setBounds(0, 0, mRadius * 2, mRadius * 2);
            }
        } catch (OutOfMemoryError error) {
            // Logger.exception(error);
        }
    }

    /**
     * 绘制编辑模式区域
     */
    private void drawEditModeArea(Canvas canvas, Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }

    /**
     * 绘制编辑模式下放大境
     */
    private void drawEditModeMagnifier(Canvas canvas) {
        createMagnifierDrawable(mCurrentBitmap);

        if (mMagnifierDrawable == null)
            return;
        this.mMagnifierDrawableMatrix.setTranslate(mRadius - mEditModeTouchX * FACTOR, mRadius - mEditModeTouchY * FACTOR - dip2px(5));
        this.mMagnifierDrawable.getPaint().getShader().setLocalMatrix(mMagnifierDrawableMatrix);
        if (mEditModeTouchY < mHeight / 4) {
            this.mEditModeTouchY += (mRadius + dip2px(50));
        } else {
            this.mEditModeTouchY -= (mRadius + dip2px(50));
        }
        // bounds，就是那个圆的外切矩形
        Rect boundsRect = new Rect();
        boundsRect.left = (int) mEditModeTouchX - mRadius;
        boundsRect.right = (int) mEditModeTouchX + mRadius;
        boundsRect.top = (int) mEditModeTouchY - mRadius;
        boundsRect.bottom = (int) mEditModeTouchY + mRadius;

        this.mMagnifierDrawable.setBounds(boundsRect);
        this.mMagnifierDrawable.draw(canvas);
        // 画边框
        canvas.drawCircle(mEditModeTouchX, mEditModeTouchY, mRadius, mMagnifierPaint);
    }

    public void setEditModeXY(float x, float y) {
        this.mEditModeTouchX = x;
        this.mEditModeTouchY = y;
    }

    //endregion

    public void setExistPage(boolean existPage) {
        this.mExistPage = existPage;
    }

    public void setBitmaps(Bitmap currentBitmap, Bitmap nextBitmap) {
        this.mCurrentBitmap = currentBitmap;
        this.mNextBitmap = nextBitmap;
    }

    public void setBgColor(int color) {
        this.mBgColor = color;
    }

    public boolean canDragOver() {
        if (mTouchToCornerDis > mWidth / 10)
            return true;
        return false;
    }

    public void onDestroy() {
        recycleBitmap(mCurrentBitmap);
        recycleBitmap(mNextBitmap);
    }

    /**
     * 释放图片
     */
    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private int dip2px(float dip) {
        return DeviceUtil.INSTANCE.dip2px(getContext(), dip);
    }


    public void setTranslationY(int dy) {
        //将bitmap做位移操作
        mTranslationY = dy;
        postInvalidate();
    }

    private void drawScrollBody(Canvas canvas) {
        canvas.save();
        try {
            canvas.translate(0, mTranslationY);
        } catch (Exception e) {
            // Logger.exception(e);
        }
        if (mCurrentBitmap != null && !mCurrentBitmap.isRecycled()) {
            canvas.drawBitmap(mCurrentBitmap, 0, 0, null);
        }
        canvas.restore();
    }
}
