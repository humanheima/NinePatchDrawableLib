package com.dmw.ninepatchdrawablelib

import android.content.Context
import android.content.Intent

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 静态聊天气泡
 */
class RecyclerViewStaticBubbleActivity : BaseActivity() {

    companion object {

        fun launch(context: Context, fromFile: Boolean) {
            val starter = Intent(context, RecyclerViewStaticBubbleActivity::class.java)
            starter.putExtra("fromFile", fromFile)
            context.startActivity(starter)
        }
    }

    override fun createAdapter(): ChatAdapter {
        val fromFile = intent.getBooleanExtra("fromFile", false)
        adapter = ChatAdapter(items, this, false, fromFile)
        return adapter as ChatAdapter
    }
}