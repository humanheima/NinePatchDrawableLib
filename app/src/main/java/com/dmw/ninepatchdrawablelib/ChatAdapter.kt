package com.dmw.ninepatchdrawablelib

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dmw.lib.ninepatch.AnimationDrawableFactory
import com.dmw.lib.ninepatch.NinePatchDrawableFactory
import com.dmw.lib.ninepatch.PatchStretchBean
import com.dmw.ninepatchdrawablelib.ChatAdapter.MyViewHolder
import java.io.File

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 聊天列表适配器
 * @param isDynamic 是否使用动态气泡
 */
class ChatAdapter(private val items: List<ChatItem>, val context: Context, val isDynamic: Boolean) :
    RecyclerView.Adapter<MyViewHolder>() {


    private val resIdList = mutableListOf<Int>().apply {
        add(R.drawable.bubble_frame1)
        add(R.drawable.bubble_frame2)
        add(R.drawable.bubble_frame3)
        add(R.drawable.bubble_frame4)
        add(R.drawable.bubble_frame5)
        add(R.drawable.bubble_frame6)
        add(R.drawable.bubble_frame7)
        add(R.drawable.bubble_frame8)
        add(R.drawable.bubble_frame9)
        add(R.drawable.bubble_frame10)
        add(R.drawable.bubble_frame11)
        add(R.drawable.bubble_frame12)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_layout, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val chatItem = items[position]

        val lp = holder.tvContent.layoutParams as? RelativeLayout.LayoutParams
        if (chatItem.isSelf) {
            holder.ivAvatarLeft.visibility = View.GONE
            holder.ivAvatarRight.visibility = View.VISIBLE
            lp?.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            lp?.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
        } else {
            holder.ivAvatarLeft.visibility = View.VISIBLE
            holder.ivAvatarRight.visibility = View.GONE
            lp?.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            lp?.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        }
        holder.tvContent.text = chatItem.content
        val drawable = getDrawable()
        holder.tvContent.background = drawable
        if (drawable is AnimationDrawable) {
            drawable.start()
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getDrawable(): Drawable? {
        if (isDynamic) {
            //return getDynamicDrawableFromFile(context, "bubbleframe")
            return getDynamicDrawableFromResource(context)
        }
        return getStaticDrawable()
    }


    /**
     * 从正常的资源文件加载动态气泡
     */
    private fun getDynamicDrawableFromResource(context: Context): Drawable? {
        return AnimationDrawableFactory().getAnimationDrawableFromResource(
            context.resources,
            resIdList,
            PatchStretchBean(60, 61),
            PatchStretchBean(52, 53),
            Rect(31, 37, 90, 75), 128, 112, 5
        )
    }


    /**
     * 从文件加载动态气泡
     */
    private fun getDynamicDrawableFromFile(context: Context, pngDirName: String): Drawable? {
        val dir = context.getExternalFilesDir(null)
            ?: return null
        val pngsDir: File = File(dir, pngDirName)
        if (!pngsDir.exists()) {
            return null
        }
        val files = pngsDir.listFiles()
        if (files == null || files.isEmpty()) {
            return null
        }

        return AnimationDrawableFactory().getAnimationDrawableFromFile(
            context.resources,
            true,
            pngsDir,
            PatchStretchBean(60, 61),
            PatchStretchBean(52, 53),
            Rect(31, 37, 90, 75), 128, 112, 5
        )
    }


    private fun getStaticDrawable(): Drawable? {
        //return getStaticDrawableFromResource(context)
        return getStaticDrawableFromFile(context, "bubbleframe/bubble_frame1.png")
    }

    private fun getStaticDrawableFromFile(context: Context, pngFileName: String): Drawable? {
        val dir = context.getExternalFilesDir(null) ?: return null
        val pngFile: File = File(dir, pngFileName)
        if (!pngFile.exists()) {
            return null
        }

        return NinePatchDrawableFactory().get9PatchDrawableFromFile(
            context.resources,
            true,
            pngFile,
            PatchStretchBean(60, 61),
            PatchStretchBean(52, 53),
            Rect(31, 37, 90, 75), 128, 112
        )
    }

    private fun getStaticDrawableFromResource(context: Context): Drawable? {
        return NinePatchDrawableFactory().get9PatchDrawableFromResource(
            context.resources,
            R.drawable.bubble_frame1,
            PatchStretchBean(60, 61),
            PatchStretchBean(52, 53),
            Rect(31, 37, 90, 75), 128, 112
        )
    }


    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        var tvContent: TextView
        var ivAvatarLeft: ImageView
        var ivAvatarRight: ImageView


        init {
            tvContent = v.findViewById(R.id.tv_content)
            ivAvatarLeft = v.findViewById(R.id.iv_avatar_left)
            ivAvatarRight = v.findViewById(R.id.iv_avatar_right)
        }
    }


}