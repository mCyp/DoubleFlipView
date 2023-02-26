package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.view.DoubleFlipView
import com.qidian.fonttest.view.DoubleRealFlipView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    private var flipView: DoubleFlipView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)



        flipView = findViewById(R.id.flipView)
        lifecycleScope.launch(Dispatchers.IO) {
            var firstBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_one, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            firstBitmap = adjustBitmap(firstBitmap)
            var twoBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_two, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            twoBitmap = adjustBitmap(twoBitmap)
            var threeBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_three, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            threeBitmap = adjustBitmap(threeBitmap)
            var fourBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_four, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            fourBitmap = adjustBitmap(fourBitmap)
            var fiveBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_five, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            fiveBitmap = adjustBitmap(fiveBitmap)
            var sixBitmap = BitmapFactory.decodeResource(resources, R.drawable.dog_six, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
            sixBitmap = adjustBitmap(sixBitmap)

            withContext(Dispatchers.Main) {

            }
        }
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

}