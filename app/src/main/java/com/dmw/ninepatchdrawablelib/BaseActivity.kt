package com.dmw.ninepatchdrawablelib

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 静态聊天气泡
 */
abstract class BaseActivity : AppCompatActivity() {

    private var rvChat: RecyclerView? = null
    private var btnAddOtherMsg: Button? = null
    private var btnAddSelfMsg: Button? = null

    protected var adapter: ChatAdapter? = null
    protected var items: MutableList<ChatItem> = mutableListOf()

    private val msgList = mutableListOf<String>()
    private var counter = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view_bubble)
        rvChat = findViewById(R.id.rv_chat)
        btnAddOtherMsg = findViewById(R.id.btn_add_other_msg)
        btnAddSelfMsg = findViewById(R.id.btn_add_self_msg)

        rvChat?.layoutManager = LinearLayoutManager(this)

        initMsgList()

        for (i in 0..2) {
            items.add(
                if (i % 2 == 0)
                    ChatItem(
                        "X${counter++}，${msgList.random()}",
                        true
                    )
                else
                    ChatItem(
                        "Y${counter++}，${msgList.random()}",
                        false
                    )
            )
        }

        adapter = createAdapter()
        rvChat?.adapter = adapter


        btnAddOtherMsg?.setOnClickListener {
            if (adapter == null) {
                return@setOnClickListener
            }
            items.add(
                ChatItem(
                    "Y${counter++}，${msgList.random()}",
                    false
                )
            )
            adapter?.notifyItemInserted(items.size - 1)

            rvChat?.scrollToPosition(items.size - 1)

        }

        btnAddSelfMsg?.setOnClickListener {
            if (adapter == null) {
                return@setOnClickListener
            }
            items.add(
                ChatItem(
                    "X${counter++}，${msgList.random()}",
                    true
                )
            )
            adapter?.notifyItemInserted(items.size - 1)

            rvChat?.scrollToPosition(items.size - 1)

        }

    }

    private fun initMsgList() {
        msgList.add("1")
        msgList.add("哈")
        msgList.add("鱼")
        msgList.add("熊掌")
        msgList.add("哈哈")
        msgList.add("指鹿为马")
        msgList.add("指鼠为鸭")
        msgList.add("天高任鸟飞，自由真可贵")
        msgList.add("鱼，我所欲也；熊掌，亦我所欲也。二者不可得兼，舍鱼而取熊掌者也。")
        msgList.add("生，亦我所欲也；义，亦我所欲也。二者不可得兼，舍生而取义者也。生亦我所欲，所欲有甚于生者，故不为苟得也；死亦我所恶，所恶有甚于死者，故患有所不辟也。")
    }

    abstract fun createAdapter(): ChatAdapter


}