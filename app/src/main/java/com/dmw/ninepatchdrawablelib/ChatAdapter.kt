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
class ChatAdapter(
    private val items: List<ChatItem>,
    val context: Context,
    val isDynamic: Boolean,
    val fromFile: Boolean
) :
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
        val drawable = getDrawable(chatItem.isSelf)
        holder.tvContent.background = drawable
        if (drawable is AnimationDrawable) {
            drawable.start()
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getDrawable(isSelf: Boolean): Drawable? {
        //加载动态气泡
        return if (isDynamic) {
            if (fromFile) {
                //从文件加载
                getDynamicDrawableFromFile(context, "bubbleframe", isSelf)
            } else {
                //从资源文件drawable加载
                getDynamicDrawableFromResource(context, isSelf)

            }
        } else {
            //加载静态气泡
            if (fromFile) {
                //从文件加载
                getStaticDrawableFromFile(context, "bubbleframe/bubble_frame1.png", isSelf)
            } else {
                //从资源文件drawable加载
                getStaticDrawableFromResource(context, isSelf)
            }
        }
    }


    /**
     * 从正常的资源文件加载动态气泡
     */
    private fun getDynamicDrawableFromResource(context: Context, isSelf: Boolean): Drawable? {
        return AnimationDrawableFactory(context)
            .setDrawableResIdList(resIdList)//图片资源id列表
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .setFinishCount(3)//动画播放次数,不是必须的
            .setFrameDuration(100)//每帧动画的播放时间,不是必须的
            .buildFromResource()
    }

    /**
     * 从文件加载动态气泡
     */
    private fun getDynamicDrawableFromFile(
        context: Context,
        pngDirName: String,
        isSelf: Boolean
    ): Drawable? {
        val dir = context.getExternalFilesDir(null)
            ?: return null
        val pngsDir = File(dir, pngDirName)
        if (!pngsDir.exists()) {
            return null
        }
        val files = pngsDir.listFiles()
        if (files == null || files.isEmpty()) {
            return null
        }

        return AnimationDrawableFactory(context)
            .setDrawableDir(pngsDir)//图片文件所在的目录
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .setScaleFromFile(true)//是否从文件中读取图片的缩放比例，不是必须的
            .setFinishCount(3)//动画播放次数
            .setFrameDuration(100)//每帧动画的播放时间
            .buildFromFile()
    }

    private fun getStaticDrawableFromFile(
        context: Context,
        pngFileName: String,
        isSelf: Boolean
    ): Drawable? {
        val dir = context.getExternalFilesDir(null) ?: return null
        val pngFile = File(dir, pngFileName)
        if (!pngFile.exists()) {
            return null
        }

        return NinePatchDrawableFactory(context)
            .setDrawableFile(pngFile)//图片文件
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setScaleFromFile(true)//是否从文件中读取图片的缩放比例，不是必须的
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .buildFromFile()
    }

    private fun getStaticDrawableFromResource(context: Context, isSelf: Boolean): Drawable? {
        return NinePatchDrawableFactory(context)
            .setDrawableResId(R.drawable.bubble_frame1)//图片资源id
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .buildFromResource()
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