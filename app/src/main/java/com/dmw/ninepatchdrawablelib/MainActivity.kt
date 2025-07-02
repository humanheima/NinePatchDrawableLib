package com.dmw.ninepatchdrawablelib

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.dmw.ninepatchdrawablelib.databinding.ActivityMainBinding

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc:
 */
class MainActivity : AppCompatActivity() {


    private val TAG = "MainActivity"

    private lateinit var btnStaticBubbleFromFile: Button
    private lateinit var btnStaticBubbleFromDrawable: Button


    private lateinit var btnDynamicBubbleFromFile: Button
    private lateinit var btnDynamicBubbleFromDrawable: Button

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        btnStaticBubbleFromFile = findViewById(R.id.btn_static_bubble_from_file)
        btnStaticBubbleFromDrawable = findViewById(R.id.btn_static_bubble_from_drawable)

        btnDynamicBubbleFromFile = findViewById(R.id.btn_dynamic_bubble_from_file)
        btnDynamicBubbleFromDrawable = findViewById(R.id.btn_dynamic_bubble_from_drawable)

        val fileDir = getExternalFilesDir(null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }


        binding.btnCopyFile.setOnClickListener {
            FileUtils.copyBubbleFrameFromAssets(this)

            val fileList = FileUtils.listFiles(this)

            fileList.forEach {
                Log.d(TAG, "onCreate: $it")
            }
        }

        binding.btnNinePatchTheory.setOnClickListener {
            NinePatchTheoryActivity.launch(this)
        }

        btnStaticBubbleFromFile.setOnClickListener {
            RecyclerViewStaticBubbleActivity.launch(this, true)
        }

        btnStaticBubbleFromDrawable.setOnClickListener {
            RecyclerViewStaticBubbleActivity.launch(this, false)
        }



        btnDynamicBubbleFromFile.setOnClickListener {
            RecyclerViewDynamicBubbleActivity.launch(this, true)
        }

        btnDynamicBubbleFromDrawable.setOnClickListener {
            RecyclerViewDynamicBubbleActivity.launch(this, false)
        }


    }
}