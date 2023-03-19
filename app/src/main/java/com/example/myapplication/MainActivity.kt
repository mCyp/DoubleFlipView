package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.view.DoubleFlipView
import com.example.myapplication.view.PageFlipListener
import com.qidian.fonttest.view.DoubleRealFlipView
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
            var firstBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_one, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            firstBitmap = adjustBitmap(firstBitmap)
            bimapArray[0] = firstBitmap
            var twoBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_two, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            twoBitmap = adjustBitmap(twoBitmap)
            bimapArray[1] = twoBitmap
            var threeBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_three, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            threeBitmap = adjustBitmap(threeBitmap)
            bimapArray[2] = threeBitmap
            var fourBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_four, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            fourBitmap = adjustBitmap(fourBitmap)
            bimapArray[3] = fourBitmap
            var fiveBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_five, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            fiveBitmap = adjustBitmap(fiveBitmap)
            bimapArray[4] = fiveBitmap
            var sixBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_six, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
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

}