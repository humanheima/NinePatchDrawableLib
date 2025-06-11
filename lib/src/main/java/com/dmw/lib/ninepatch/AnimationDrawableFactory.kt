package com.dmw.lib.ninepatch

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.util.DisplayMetrics
import android.util.Log
import com.dmw.lib.ninepatch.BitmapLruCache
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by dmw on 2023/10/23
 * Desc: 用来创建一个AnimationDrawable，这个AnimationDrawable可以设置循环次数。AnimationDrawable中的每一帧都是一个NinePatchDrawable
 * 参考链接：
 * 1. https://juejin.cn/post/7188708254346641465
 * 2. https://mp.weixin.qq.com/s?__biz=MzI1NjEwMTM4OA==&mid=2651232105&idx=1&sn=fcc4fa956f329f839f2a04793e7dd3b9&mpshare=1&scene=21&srcid=0719Nyt7J8hsr4iYwOjVPXQE#wechat_redirect
 * 3. https://android.googlesource.com/platform/frameworks/base/+/56a2301/include/androidfw/ResourceTypes.h
 * 4. 第一次加载图片的时候，从文件解析出Bitmap，然后存储在缓存里，下次再加载的时候，直接从缓存拿
 * @see com.yuewen.dreamer.bubble.BitmapLruCache
 *
 * 注意：一组图片的宽高必须一致，不然可能会导致未知bug
 * 注意：一组图片的宽高必须一致，不然可能会导致未知bug
 * 注意：一组图片的宽高必须一致，不然可能会导致未知bug
 */
class AnimationDrawableFactory(private val context: Context) {

    companion object {

        private const val TAG = "AnimationDrawableFactor"

        //Note: 把加载过的图片缓存在内存里
        private val bitmapLruCache = BitmapLruCache()

        val NO_COLOR = 0x00000001
        val COLOR_SIZE = 9

        /**
         * 同一个路径的图片，如果需要水平镜像，保存到在LruCache的时候，key就是这个前缀+图片路径
         */
        val HORIZONTAL_MIRROR_PREFIX = "horizontal_mirror_prefix"


    }

    /**
     * 文件中，原始图片的密度，这里默认是1，如果是3倍图，就是3
     * 从文件中解析出来的bitmap，需要设置inDensity，inTargetDensity，才能正确的缩放
     */
    private var bitmapMapInDensity: Int = 1

    /**
     * 是否要水平镜像，如果左右气泡都用一张图的话，需要水平镜像一下
     */
    private var horizontalMirror = false

    /**
     * 帧动画AnimationDrawable循环次数
     */
    private var finishCount: Int = 1

    /**
     * 帧动画AnimationDrawable每一帧的时间，默认100毫秒
     */
    private var frameDuration: Int = 100

    /**
     * 图片资源id列表
     */
    private var drawableResIdList: MutableList<Int>? = null

    /**
     * 图片文件所在的目录，目前只测试了png格式
     */
    private var drawableDir: File? = null


    /**
     * 原始图片宽高
     */
    private var originWidth: Int = 0
    private var originHeight: Int = 0

    /**
     * 最终使用的图片宽高
     */
    private var finalWidth: Int = 0
    private var finalHeight: Int = 0

    private var horizontalStretchBean: PatchStretchBean? = null
    private var verticalStretchBean: PatchStretchBean? = null

    private var paddingRect: Rect? = null

    private var paddingLeft: Int = 0
    private var paddingRight: Int = 0
    private var paddingTop: Int = 0
    private var paddingBottom: Int = 0

    private var finalPaddingRect = Rect()


    private var chunk: ByteArray? = null


    /**
     * 设置横向的拉伸线段，必须的
     * @param horizontalStretchBean 横向的拉伸线段
     */
    fun setHorizontalStretchBean(horizontalStretchBean: PatchStretchBean): AnimationDrawableFactory {
        this.horizontalStretchBean = horizontalStretchBean
        return this
    }

    /**
     * 设置竖向的拉伸线段，必须的
     * @param verticalStretchBean 竖向的拉伸线段
     */
    fun setVerticalStretchBean(verticalStretchBean: PatchStretchBean): AnimationDrawableFactory {
        this.verticalStretchBean = verticalStretchBean
        return this
    }

    /**
     * 设置原始图片的宽高，必须的，是不是可以从bitmap里获取？不能，因为第二次复用的时候，无法获取原始图片的宽度和高度
     * @param originWidth 原始图片的宽度
     * @param originHeight 原始图片的高度
     */
    fun setOriginSize(originWidth: Int, originHeight: Int): AnimationDrawableFactory {
        this.originWidth = originWidth
        this.originHeight = originHeight
        return this
    }

    fun setDrawableResIdList(drawableResIdList: MutableList<Int>): AnimationDrawableFactory {
        this.drawableResIdList = drawableResIdList
        return this
    }

    fun setDrawableDir(drawableDir: File): AnimationDrawableFactory {
        this.drawableDir = drawableDir
        return this
    }

    /**
     * 设置padding，不是必须的。
     * @param rect padding的区域
     */
    fun setPadding(rect: Rect): AnimationDrawableFactory {
        paddingRect = rect
        return this
    }

    /**
     * 设置是否水平镜像，如果左右聊天气泡都用一张图的话，需要水平镜像一下，不是必须的
     * @param horizontalMirror 是否水平镜像
     */
    fun setHorizontalMirror(horizontalMirror: Boolean): AnimationDrawableFactory {
        this.horizontalMirror = horizontalMirror
        return this
    }

    /**
     * 不是必须的，如果不设置，就用默认的，循环一次
     */
    fun setFinishCount(finishCount: Int): AnimationDrawableFactory {
        this.finishCount = finishCount
        return this
    }

    /**
     * 不是必须的，如果不设置，就用默认的，100毫秒
     */
    fun setFrameDuration(frameDuration: Int): AnimationDrawableFactory {
        this.frameDuration = frameDuration
        return this
    }

    fun setBitmapMapInDensity(bitmapMapInDensity: Int): AnimationDrawableFactory {
        this.bitmapMapInDensity = bitmapMapInDensity
        return this
    }

    /**
     *
     * @param resources
     * 注意，资源都是一倍图，在drawable文件夹的，从资源加载会自动缩放到当前的density。如果是从文件加载，则需要自己处理缩放。
     */
    fun buildFromResource(): AnimationDrawable? {

        if (drawableResIdList.isNullOrEmpty()) {
            return null
        }

        val currentTimeMillis = System.currentTimeMillis()
        val animationDrawable = CanStopAnimationDrawable()
        animationDrawable.setFinishCount(finishCount)

        drawableResIdList?.forEach { resId ->
            val ninePatchDrawable = get9PatchFromResource(context.resources, resId)
            if (ninePatchDrawable != null) {
                animationDrawable.addFrame(ninePatchDrawable, 100)
            }
        }
        animationDrawable.isOneShot = false

        Log.i(
            TAG,
            "getAnimationDrawableFromResource: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return animationDrawable

    }

    /**
     * @param resources
     * @param resId 图片资源id
     */
    private fun get9PatchFromResource(resources: Resources, resId: Int): Drawable? {
        val resIdString = resId.toString()
        var bitmap = if (horizontalMirror) {
            bitmapLruCache.getBitmap(HORIZONTAL_MIRROR_PREFIX + resIdString)
        } else {
            bitmapLruCache.getBitmap(resIdString)
        }

        if (bitmap == null) {
            bitmap = try {
                BitmapFactory.decodeResource(resources, resId)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }

            if (bitmap != null) {
                if (horizontalMirror) {
                    val matrix = Matrix()
                    matrix.postScale(-1f, 1f)
                    val mirrorBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix,
                        false
                    )
                    bitmapLruCache.putBitmap(
                        HORIZONTAL_MIRROR_PREFIX + resIdString,
                        mirrorBitmap
                    )
                    bitmap = mirrorBitmap
                } else {
                    bitmapLruCache.putBitmap(resIdString, bitmap)
                }
                Log.i(TAG, "setResourceData: width = ${bitmap.width}, height = ${bitmap.height}")
            }
        } else {
            Log.d(TAG, "setResourceData: 从缓存中获取bitmap != null")
        }

        setFinalUsedBitmapSize(bitmap)

        return get9PatchDrawable(bitmap, resources)
    }


    /**
     * 设置本地文件夹中的图片
     * @param dir 本地文件夹
     * @param scale 是否需要缩放，如果文件夹中是1倍图，需要缩放，如果就是正常的3倍图，不需要缩放，这块缩放逻辑可以自行调整
     * @param patchHorizontal 横向拉伸的线段
     * @param patchVertical 竖向拉伸的线段
     * @param paddingRect padding的区域
     * @param originWidth 原始图片的宽度
     * @param originHeight 原始图片的高度
     * @param finishCount 动画循环次数
     */
    fun buildFromFile(): AnimationDrawable? {
        val currentTimeMillis = System.currentTimeMillis()
        if (drawableDir == null || !drawableDir!!.exists()) {
            return null
        }
        val files = drawableDir?.listFiles()
        if (files.isNullOrEmpty()) {
            return null
        }
        val animationDrawable = CanStopAnimationDrawable()
        //设置循环5次，就结束
        animationDrawable.setFinishCount(finishCount)
        files.forEach { pngFile ->
            Log.i(TAG, "buildFromFile: pngFile = $pngFile")
            val ninePatchDrawable = get9PatchFromFile(context.resources, pngFile)
            if (ninePatchDrawable != null) {
                animationDrawable.addFrame(ninePatchDrawable, frameDuration)
            }
        }
        animationDrawable.isOneShot = false

        Log.i(
            TAG,
            "getAnimationDrawableFromFile: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return animationDrawable

    }

    /**
     * 设置本地文件夹中的图片
     * 从文件解析的话，需要处理缩放，density
     * @param file 本地png文件路径
     */
    private fun get9PatchFromFile(
        resources: Resources,
        file: File
    ): Drawable? {

        val absolutePath = file.absolutePath

        var bitmap = if (horizontalMirror) {
            bitmapLruCache.getBitmap(HORIZONTAL_MIRROR_PREFIX + absolutePath)
        } else {
            bitmapLruCache.getBitmap(absolutePath)
        }

        if (bitmap == null) {
            bitmap = try {
                BitmapFactory.decodeFile(absolutePath, BitmapFactory.Options().apply {
                    //bitmap 原本的密度
                    inDensity = bitmapMapInDensity * DisplayMetrics.DENSITY_DEFAULT
                    //当前设备的密度
                    inTargetDensity = resources.displayMetrics.densityDpi
                })
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }

            if (bitmap != null) {
                Log.i(
                    TAG,
                    "setFileData: not scale width = ${bitmap.width}, height = ${bitmap.height}"
                )
                if (horizontalMirror) {
                    val matrix = Matrix()
                    matrix.preScale(-1f, 1f)
                    val mirrorBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix,
                        false
                    )
                    bitmapLruCache.putBitmap(
                        HORIZONTAL_MIRROR_PREFIX + absolutePath,
                        mirrorBitmap
                    )
                    bitmap = mirrorBitmap
                } else {
                    bitmapLruCache.putBitmap(absolutePath, bitmap)
                }
            }
        }

        setFinalUsedBitmapSize(bitmap)

        return get9PatchDrawable(bitmap, resources)

    }

    private fun get9PatchDrawable(bitmap: Bitmap?, resources: Resources): Drawable? {
        return try {
            paddingRect?.let {
                paddingLeft = it.left
                paddingRight = it.right
                paddingTop = it.top
                paddingBottom = it.bottom
                buildPadding()
            }

            if (chunk == null) {
                chunk = buildChunk()
            }
            val ninePatchDrawable =
                NinePatchDrawable(resources, bitmap, chunk, finalPaddingRect, null)
            ninePatchDrawable
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 直接处理bitmap数据
     */
    private fun setFinalUsedBitmapSize(bitmap: Bitmap?) {
        this.finalWidth = bitmap?.width ?: 0
        this.finalHeight = bitmap?.height ?: 0
    }

    private fun buildChunk(): ByteArray {
        // 横向和竖向端点的数量 = 线段数量 * 2，这里只有一个线段，所以都是2
        val horizontalEndpointsSize = 2
        val verticalEndpointsSize = 2

        //这里计算的 arraySize 是 int 值，最终占用的字节数是 arraySize * 4
        val arraySize = 1 + 2 + 4 + 1 + horizontalEndpointsSize + verticalEndpointsSize + COLOR_SIZE
        //这里乘以4，是因为一个int占用4个字节
        val byteBuffer = ByteBuffer.allocate(arraySize * 4).order(ByteOrder.nativeOrder())

        byteBuffer.put(1.toByte()) //第一个字节无意义，不等于0就行
        byteBuffer.put(horizontalEndpointsSize.toByte()) //mDivX x数组的长度
        byteBuffer.put(verticalEndpointsSize.toByte()) //mDivY y数组的长度
        byteBuffer.put(COLOR_SIZE.toByte()) //mColor数组的长度

        // skip 8 bytes
        byteBuffer.putInt(0)
        byteBuffer.putInt(0)

        //Note: 目前还没搞清楚，发现都 byteBuffer.putInt(0)，也没问题。
        //左右padding
        byteBuffer.putInt(finalPaddingRect.left)
        byteBuffer.putInt(finalPaddingRect.right)
//        //上下padding
        byteBuffer.putInt(finalPaddingRect.top)
        byteBuffer.putInt(finalPaddingRect.bottom)

//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)
//        //上下padding
//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)


//        // skip 4 bytes
        byteBuffer.putInt(0)

        //mDivX数组，控制横向拉伸的线段数据
        horizontalStretchBean?.let {
            if (horizontalMirror) {
                byteBuffer.putInt((originWidth - it.end) * finalWidth / originWidth)
                byteBuffer.putInt((originWidth - it.start) * finalWidth / originWidth)
            } else {
                byteBuffer.putInt(it.start * finalWidth / originWidth)
                byteBuffer.putInt(it.end * finalWidth / originWidth)
            }
        }

        //mDivY数组，控制竖向拉伸的线段数据
        verticalStretchBean?.let {
            byteBuffer.putInt(it.start * finalHeight / originHeight)
            byteBuffer.putInt(it.end * finalHeight / originHeight)
        }

        //mColor数组
        for (i in 0 until COLOR_SIZE) {
            byteBuffer.putInt(NO_COLOR)
        }

        return byteBuffer.array()
    }

    /**
     * 控制内容填充的区域
     * （注意：这里的left，top，right，bottom同xml文件中的padding意思一致，只不过这里是百分比形式）
     *
     * TODO 举个通俗的例子，并不对应代码逻辑。
     *
     * ### **举例说明**
     * 假设一个 9-patch 图像尺寸为 100x100 像素：
     * - 底部边框的黑色标记从第 20 像素到第 80 像素，表示内容区域的水平范围。
     *     - 则 `mPaddings.left = 20`，`mPaddings.right = 100 - 80 = 20`。
     * - 右侧边框的黑色标记从第 10 像素到第 90 像素，表示内容区域的垂直范围。
     *     - 则 `mPaddings.top = 10`，`mPaddings.bottom = 100 - 90 = 10`。
     * - 结果：内容区域是一个矩形，左上角坐标为 (20, 10)，右下角坐标为 (80, 90)，适合放置文本或图标。
     */
    private fun buildPadding() {
        if (horizontalMirror) {
            finalPaddingRect.left = ((originWidth - paddingRight) * finalWidth / originWidth)
            finalPaddingRect.right = ((paddingLeft * finalWidth) / originWidth)
        } else {
            finalPaddingRect.left = (paddingLeft * finalWidth / originWidth)
            finalPaddingRect.right = ((originWidth - paddingRight) * finalWidth / originWidth)
        }

        finalPaddingRect.top = (paddingTop * finalHeight / originHeight)
        finalPaddingRect.bottom = ((originHeight - paddingBottom) * finalHeight / originHeight)

        Log.i(TAG, "buildPadding: rect = $finalPaddingRect")
    }


}