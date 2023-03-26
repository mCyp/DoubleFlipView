package com.example.myapplication

import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.view.DeviceUtil
import com.example.myapplication.view.DoubleFlipView
import com.example.myapplication.view.PageFlipListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity(), PageFlipListener {

    private var bimapArray = Array<Bitmap?>(6, {null})

    private var flipView: DoubleFlipView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        flipView = findViewById(R.id.flipView)
        flipView?.setPageFlipListener(this)
        lifecycleScope.launch(Dispatchers.IO) {
            var firstBitmap = createBitmapFirst(0)
            firstBitmap = adjustBitmap(firstBitmap)
            bimapArray[0] = firstBitmap
            var twoBitmap = createBitmapTwo(1)
            twoBitmap = adjustBitmap(twoBitmap)
            bimapArray[1] = twoBitmap
            var threeBitmap = createBitmapFirst(2)
            threeBitmap = adjustBitmap(threeBitmap)
            bimapArray[2] = threeBitmap
            var fourBitmap = createBitmapTwo(3)
            fourBitmap = adjustBitmap(fourBitmap)
            bimapArray[3] = fourBitmap
            var fiveBitmap = createBitmapFirst(4)
            fiveBitmap = adjustBitmap(fiveBitmap)
            bimapArray[4] = fiveBitmap
            var sixBitmap = createBitmapTwo(5)
            sixBitmap = adjustBitmap(sixBitmap)
            bimapArray[5] = sixBitmap

            withContext(Dispatchers.Main) {
                updateFlip()
                flipView?.mDoubleRealFlipView?.invalidate()
            }
        }
    }

    private fun updateFlip() {
        flipView?.mDoubleRealFlipView?.mLeftBottomBitmap = bimapArray[0]
        flipView?.mDoubleRealFlipView?.mLeftMiddleBitmap = bimapArray[1]
        flipView?.mDoubleRealFlipView?.mLeftTopBitmap = bimapArray[2]
        flipView?.mDoubleRealFlipView?.mRightTopBitmap = bimapArray[3]
        flipView?.mDoubleRealFlipView?.mRightMiddleBitmap = bimapArray[4]
        flipView?.mDoubleRealFlipView?.mRightBottomBitmap = bimapArray[5]
    }

    private fun adjustBitmap(bitmap: Bitmap): Bitmap {
        val curH = bitmap.height
        val curW = bitmap.width
        if(curH == 0 || curW == 0) {
            return bitmap
        }
        val dm: DisplayMetrics = resources.getDisplayMetrics()
        val height = dm.heightPixels
        val scaleHeight = height.toFloat() / curH
        val matrix = Matrix()
        matrix.postScale(scaleHeight, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, curW, curH, matrix, true)
    }

    override fun onNextPage() {
        val cacheOne = bimapArray[0]
        val cacheTwo = bimapArray[1]
        for(i in 0..3){
            bimapArray[i] = bimapArray[i + 2]
        }
        bimapArray[4] = cacheOne
        bimapArray[5] = cacheTwo
        updateFlip()
        flipView?.reset()
        flipView?.invalidate()
    }

    override fun onPrePage() {
        val cacheOne = bimapArray[4]
        val cacheTwo = bimapArray[5]
        for(i in 5 downTo 2){
            bimapArray[i] = bimapArray[i - 2]
        }
        bimapArray[0] = cacheOne
        bimapArray[1] = cacheTwo
        updateFlip()
        flipView?.reset()
        flipView?.invalidate()
    }

    private fun createBitmapFirst(index: Int): Bitmap {
        val root: View = LayoutInflater.from(this).inflate(R.layout.view_album_style_one, null, false)
        val tvTitle = root.findViewById<TextView>(R.id.tvTitle)
        val ivTop = root.findViewById<ImageView>(R.id.ivTop)
        val ivBottom = root.findViewById<ImageView>(R.id.ivBottom)
        when(index) {
            0 -> {
                tvTitle.text = "来鼋头渚吧！"
                ivTop.setImageResource(R.drawable.ytz_b)
                ivBottom.setImageResource(R.drawable.ytz_p)
            }
            2 -> {
                tvTitle.text = "元宵城隍庙～"
                ivTop.setImageResource(R.drawable.chm_j)
                ivBottom.setImageResource(R.drawable.chm_p)
            }
            4 -> {
                tvTitle.text = "外滩建筑"
                ivTop.setImageResource(R.drawable.wt_p)
                ivBottom.setImageResource(R.drawable.wt_h)
            }
        }

        val margin = DeviceUtil.dip2px(this, 20f)
        val targetWidth = DeviceUtil.getScreenWidth(this) / 2 - margin
        val targetHeight = DeviceUtil.getScreenHeight(this) - margin
        val measureWidth = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
        val measureHeight = View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.EXACTLY)
        root.measure(measureWidth, measureHeight)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        root.draw(canvas)
        return bitmap
    }

private fun createBitmapTwo(index: Int): Bitmap {
    val root: View = LayoutInflater.from(this).inflate(R.layout.view_album_style_two, null, false)
    val tvTitle = root.findViewById<TextView>(R.id.tvTitle)
    val ivTop = root.findViewById<ImageView>(R.id.ivTop)
    when(index) {
        1 -> {
            tvTitle.text = "记录张园"
            ivTop.setImageResource(R.drawable.zy_p)
        }
        3 -> {
            tvTitle.text = "唐镇随手拍"
            ivTop.setImageResource(R.drawable.tz_j)
        }
        5 -> {
            tvTitle.text = "张江微电子四号楼"
            ivTop.setImageResource(R.drawable.zj_j)
        }
    }
    val margin = DeviceUtil.dip2px(this, 20f)
    val targetWidth = DeviceUtil.getScreenWidth(this) / 2 -  margin
    val targetHeight = DeviceUtil.getScreenHeight(this) - margin
    val measureWidth = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
    val measureHeight = View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.EXACTLY)
    root.measure(measureWidth, measureHeight)
    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    root.draw(canvas)
    return bitmap
}

}