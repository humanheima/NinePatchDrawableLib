package com.dmw.ninepatchdrawablelib

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmw.ninepatchdrawablelib.databinding.ActivityNinePatchTheoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by p_dmweidu on 2025/7/2
 * Desc: 用来测试NinePatchDrawable的原理
 */
class NinePatchTheoryActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "NinePatchTheoryActivity"

        fun launch(context: Context) {
            val starter = Intent(context, NinePatchTheoryActivity::class.java)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityNinePatchTheoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNinePatchTheoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDeserialize.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                Log.d(TAG, "onCreate: currentThread = ${Thread.currentThread().name}")
                testDeserialize()
            }
        }
    }

    private fun testDeserialize() {

        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.bubble_frame1)

        Log.d(
            TAG,
            "testDeserialize: originalBitmap.width = ${originalBitmap.width}, originalBitmap.height = ${originalBitmap.height}"
        )

        // 加载 .9.png 文件
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_bubble_frame1)

        Log.d(
            TAG,
            "testDeserialize: bitmap.width = ${bitmap.width}, bitmap.height = ${bitmap.height}"
        )

        // 获取 NinePatch 数据块
        val chunk: ByteArray? = bitmap.ninePatchChunk
        Log.d(TAG, "testDeserialize: ${chunk.contentToString()}")
        val ninePatchChunk = NinePatchChunk.deserialize(chunk)


        // 获取拉伸区域
        val divX = ninePatchChunk.mDivX
        val divY = ninePatchChunk.mDivY

        // 获取内容区域的 padding
        val padding = Rect(ninePatchChunk.mPaddings)


        // 打印信息
        Log.d(TAG, "Horizontal stretch regions: " + divX.contentToString())
        Log.d(TAG, "Vertical stretch regions: " + divY.contentToString())
        Log.d(TAG, "Padding: $padding");

    }

}