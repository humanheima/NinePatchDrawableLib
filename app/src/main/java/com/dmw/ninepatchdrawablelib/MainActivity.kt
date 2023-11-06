package com.dmw.ninepatchdrawablelib

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc:
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnStaticBubble: Button
    private lateinit var btnDynamicBubble: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnStaticBubble = findViewById(R.id.btn_static_bubble)
        btnDynamicBubble = findViewById(R.id.btn_dynamic_bubble)

        val fileDir = getExternalFilesDir(null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }
        btnStaticBubble.setOnClickListener {
            RecyclerViewStaticBubbleActivity.launch(this)
        }

        btnDynamicBubble.setOnClickListener {
            RecyclerViewDynamicBubbleActivity.launch(this)
        }


    }
}