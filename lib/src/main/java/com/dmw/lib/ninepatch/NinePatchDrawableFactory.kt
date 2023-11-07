package com.dmw.lib.ninepatch

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.util.DisplayMetrics
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 从文件或者资源中加载一张png，然后转化成NinePatchDrawable返回。
 */
class NinePatchDrawableFactory {

    companion object {

        private const val TAG = "NinePatchDrawableFactor"

        //Note: 把加载过的图片缓存在内存里
        private val bitmapLruCache = BitmapLruCache()

        val NO_COLOR = 0x00000001
        val COLOR_SIZE = 9

        /**
         * 同一个路径的图片，如果需要水平镜像，保存到在LruCache的时候，key就是这个前缀+图片路径
         */
        val HORIZONTAL_MIRROR_PREFIX = "horizontal_mirror_prefix"

    }

    private var scale = false

    /**
     * 是否要水平镜像，如果左右气泡都用一张图的话，需要水平镜像一下
     */
    private var horizontalMirror = false

    private var width: Int = 0
    private var height: Int = 0

    private var originWidth: Int = 0
    private var originHeight: Int = 0

    private var patchRegionHorizontal = mutableListOf<PatchStretchBean>()
    private var patchRegionVertical = mutableListOf<PatchStretchBean>()

    private var paddingLeft: Int = 0
    private var paddingRight: Int = 0
    private var paddingTop: Int = 0
    private var paddingBottom: Int = 0

    private var chunk: ByteArray? = null

    /**
     *
     * @param resources
     * @param resId 资源id
     * @param patchHorizontal 横向拉伸的线段
     * @param patchVertical 竖向拉伸的线段
     * @param paddingRect padding的区域
     * @param originWidth 原始图片的宽度
     * @param originHeight 原始图片的高度
     * @param horizontalMirror 是否水平镜像，如果左右聊天气泡都用一张图的话，需要水平镜像一下
     * 注意，资源都是一倍图，在drawable文件夹的，从资源加载会自动缩放到当前的density。如果是从文件加载，则需要自己处理缩放。
     */
    fun get9PatchDrawableFromResource(
        resources: Resources,
        resId: Int,
        patchHorizontal: PatchStretchBean,
        patchVertical: PatchStretchBean,
        paddingRect: Rect,
        originWidth: Int,
        originHeight: Int,
        horizontalMirror: Boolean = false,
    ): Drawable? {

        this.horizontalMirror = horizontalMirror
        val currentTimeMillis = System.currentTimeMillis()

        setPatchHorizontal(patchHorizontal)
        setPatchVertical(patchVertical)
        setPadding(paddingRect)
        setOriginSize(originWidth, originHeight)

        val ninePatchDrawable = get9PatchFromResource(resources, resId)

        Log.i(
            TAG,
            "get9PatchDrawableFromResource: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return ninePatchDrawable

    }

    /**
     * @param resources
     * @param resId 图片资源id
     */
    private fun get9PatchFromResource(resources: Resources, resId: Int): Drawable? {
        val resIdString = resId.toString()
        Log.i(TAG, "setResourceData: resId = $resId")

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
            Log.i(TAG, "setResourceData: 从缓存中获取bitmap != null")
        }

        setBitmapData(bitmap)

        return get9PatchDrawable(bitmap, resources)
    }


    /**
     * @param pngFile 本地png文件
     * @param scale 是否需要缩放，如果文件夹中是1倍图，需要缩放，如果就是正常的3倍图，不需要缩放，这块缩放逻辑可以自行调整
     * @param patchHorizontal 横向拉伸的线段
     * @param patchVertical 竖向拉伸的线段
     * @param paddingRect padding的区域
     * @param originWidth 原始图片的宽度
     * @param originHeight 原始图片的高度
     */
    fun get9PatchDrawableFromFile(
        resources: Resources,
        scale: Boolean,
        pngFile: File,
        patchHorizontal: PatchStretchBean,
        patchVertical: PatchStretchBean,
        paddingRect: Rect,
        originWidth: Int,
        originHeight: Int,
        horizontalMirror: Boolean = false
    ): Drawable? {

        val currentTimeMillis = System.currentTimeMillis()

        this.scale = scale
        this.horizontalMirror = horizontalMirror

        setPatchHorizontal(patchHorizontal)
        setPatchVertical(patchVertical)
        setPadding(paddingRect)
        setOriginSize(originWidth, originHeight)
        val ninePatchDrawable = get9PatchFromFile(resources, pngFile)

        Log.i(
            TAG,
            "get9PatchDrawableFromFile: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return ninePatchDrawable

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
                BitmapFactory.decodeFile(absolutePath)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }

            if (bitmap != null) {
                if (scale) {
                    // warning：2023/11/5: 注意，这里从文件里加载的是1倍图，所以要放大一下
                    val displayMetrics: DisplayMetrics = resources.displayMetrics
                    val density = displayMetrics.density

                    val matrix = Matrix()
                    if (horizontalMirror) {
                        matrix.preScale(-1f, 1f)
                    }
                    matrix.postScale(density, density)
                    val scaledBitmap =
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    //val width = (bitmap.width * density).toInt()
                    //val height = (bitmap.height * density).toInt()

                    //Log.i(TAG, "setFileData: width = ${bitmap.width}, height = ${bitmap.height}")
                    //val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                    Log.i(
                        TAG,
                        "setFileData: scaledBitmap width = ${scaledBitmap.width}, height = ${scaledBitmap.height}"
                    )
                    if (horizontalMirror) {
                        bitmapLruCache.putBitmap(
                            HORIZONTAL_MIRROR_PREFIX + absolutePath,
                            scaledBitmap
                        )
                    } else {
                        bitmapLruCache.putBitmap(absolutePath, scaledBitmap)
                    }
                    bitmap = scaledBitmap
                } else {
                    Log.i(
                        TAG,
                        "setFileData: not scale  width = ${bitmap.width}, height = ${bitmap.height}"
                    )

                    if (horizontalMirror) {
                        val matrix = Matrix()
                        matrix.postScale(-1f, 1f)
                        val mirrorBitmap =
                            Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.width,
                                bitmap.height,
                                matrix,
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
        }

        setBitmapData(bitmap)

        return get9PatchDrawable(bitmap, resources)

    }

    private fun get9PatchDrawable(bitmap: Bitmap?, resources: Resources): Drawable? {
        return try {
            if (mRectPadding.left == 0 && mRectPadding.right == 0 && mRectPadding.top == 0 && mRectPadding.bottom == 0) {
                buildPadding()
            }
            if (chunk == null) {
                chunk = buildChunk()
            }
            val ninePatchDrawable =
                NinePatchDrawable(resources, bitmap, chunk, mRectPadding, null)
            ninePatchDrawable
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 直接处理bitmap数据
     */
    private fun setBitmapData(bitmap: Bitmap?) {
        Log.i(TAG, "setBitmapData: width = ${bitmap?.width}, height = ${bitmap?.height}")
        this.width = bitmap?.width ?: 0
        this.height = bitmap?.height ?: 0
    }

    private fun buildChunk(): ByteArray {
        // 横向和竖向端点的数量 = 线段数量 * 2
        val horizontalEndpointsSize = patchRegionHorizontal.size * 2
        val verticalEndpointsSize = patchRegionVertical.size * 2

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

        //Note: 等待进一步研究，这四个int值，好像都写0也可以。
        //左右padding
        byteBuffer.putInt(mRectPadding.left)
        byteBuffer.putInt(mRectPadding.right)
        //上下padding
        byteBuffer.putInt(mRectPadding.top)
        byteBuffer.putInt(mRectPadding.bottom)

//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)
//        //上下padding
//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)


//        // skip 4 bytes
        byteBuffer.putInt(0)

        // regions 控制横向拉伸的线段数据
        //mDivX数组
        patchRegionHorizontal.forEach {
            if (horizontalMirror) {
                byteBuffer.putInt((originWidth - it.end) * width / originWidth)
                byteBuffer.putInt((originWidth - it.start) * width / originWidth)
            } else {
                byteBuffer.putInt(it.start * width / originWidth)
                byteBuffer.putInt(it.end * width / originWidth)
            }
        }

        //mDivY数组
        // regions 控制竖向拉伸的线段数据
        patchRegionVertical.forEach {
            byteBuffer.putInt(it.start * height / originHeight)
            byteBuffer.putInt(it.end * height / originHeight)
        }

        //mColor数组
        for (i in 0 until COLOR_SIZE) {
            byteBuffer.putInt(NO_COLOR)
        }

        return byteBuffer.array()
    }

    private fun setPatchHorizontal(vararg patchRegion: PatchStretchBean) {
        patchRegion.forEach {
            if (patchRegionHorizontal.contains(it)) {
                return@forEach
            }
            patchRegionHorizontal.add(it)
        }
    }

    private fun setPatchVertical(vararg patchRegion: PatchStretchBean) {
        patchRegion.forEach {
            if (patchRegionVertical.contains(it)) {
                return@forEach
            }
            patchRegionVertical.add(it)
        }
    }


    /**
     * 设置原始图片的宽高
     */
    private fun setOriginSize(originWidth: Int, originHeight: Int): NinePatchDrawableFactory {
        this.originWidth = originWidth
        this.originHeight = originHeight
        return this
    }

    private fun setPadding(rect: Rect) {
        paddingLeft = rect.left
        paddingTop = rect.top
        paddingRight = rect.right
        paddingBottom = rect.bottom
    }

    private var mRectPadding = Rect()

    /**
     * 控制内容填充的区域
     * （注意：这里的left，top，right，bottom同xml文件中的padding意思一致，只不过这里是百分比形式）
     */
    private fun buildPadding() {
        if (horizontalMirror) {
            mRectPadding.left = ((originWidth - paddingRight) * width / originWidth)
            mRectPadding.right = ((paddingLeft * width) / originWidth)
        } else {
            mRectPadding.left = ((paddingLeft * width) / originWidth)
            mRectPadding.right = ((originWidth - paddingRight) * width / originWidth)
        }
        mRectPadding.top = (paddingTop * height / originHeight)
        mRectPadding.bottom = ((originHeight - paddingBottom) * height / originHeight)

        Log.i(TAG, "buildPadding: rect = $mRectPadding")
    }


}