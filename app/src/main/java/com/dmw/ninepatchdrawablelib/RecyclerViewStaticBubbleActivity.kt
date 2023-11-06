package com.dmw.ninepatchdrawablelib

import android.content.Context
import android.content.Intent

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 静态聊天气泡
 */
class RecyclerViewStaticBubbleActivity : BaseActivity() {

    companion object {

        fun launch(context: Context) {
            val starter = Intent(context, RecyclerViewStaticBubbleActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun createAdapter(): ChatAdapter {
        adapter = ChatAdapter(items, this,false)
        return adapter as ChatAdapter
    }
}