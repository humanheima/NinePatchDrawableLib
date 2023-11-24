package com.dmw.lib.ninepatch

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.DrawableRes
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Created by p_dmweidu on 2023/11/6
 * Desc: 从文件或者资源中加载一张png，然后转化成NinePatchDrawable返回。
 */
class NinePatchDrawableFactory(private val context: Context) {

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
     * 图片资源id
     */
    @DrawableRes
    private var drawableResId: Int = 0

    /**
     * 图片文件，目前只测试了png格式
     */
    private var drawableFile: File? = null

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

    private var chunk: ByteArray? = null

    private var finalPaddingRect = Rect()

    /**
     * 从资源文件中加载图片
     */
    fun buildFromResource(): Drawable? {
        val currentTimeMillis = System.currentTimeMillis()
        val ninePatchDrawable = get9PatchFromResource(context.resources, drawableResId)
        Log.i(
            TAG, "buildFromResource: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return ninePatchDrawable
    }

    /**
     * 从文件中加载图片
     */
    fun buildFromFile(): Drawable? {
        val currentTimeMillis = System.currentTimeMillis()
        val ninePatchDrawable = get9PatchFromFile(context.resources, drawableFile)

        Log.i(
            TAG,
            "buildFromFile: end 耗时：${System.currentTimeMillis() - currentTimeMillis} ms"
        )
        return ninePatchDrawable
    }

    /**
     * 设置横向的拉伸线段，必须的
     * @param horizontalStretchBean 横向的拉伸线段
     */
    fun setHorizontalStretchBean(horizontalStretchBean: PatchStretchBean): NinePatchDrawableFactory {
        this.horizontalStretchBean = horizontalStretchBean
        return this
    }

    /**
     * 设置竖向的拉伸线段，必须的
     * @param verticalStretchBean 竖向的拉伸线段
     */
    fun setVerticalStretchBean(verticalStretchBean: PatchStretchBean): NinePatchDrawableFactory {
        this.verticalStretchBean = verticalStretchBean
        return this
    }

    /**
     * 设置原始图片的宽高，必须的，是不是可以从bitmap里获取？不能，因为第二次复用的时候，无法获取原始图片的宽度和高度
     * @param originWidth 原始图片的宽度
     * @param originHeight 原始图片的高度
     */
    fun setOriginSize(originWidth: Int, originHeight: Int): NinePatchDrawableFactory {
        this.originWidth = originWidth
        this.originHeight = originHeight
        return this
    }

    fun setDrawableResId(drawableResId: Int): NinePatchDrawableFactory {
        this.drawableResId = drawableResId
        return this
    }

    fun setDrawableFile(drawableFile: File): NinePatchDrawableFactory {
        this.drawableFile = drawableFile
        return this
    }

    fun setBitmapMapInDensity(bitmapMapInDensity: Int): NinePatchDrawableFactory {
        this.bitmapMapInDensity = bitmapMapInDensity
        return this
    }

    /**
     * 设置padding，不是必须的。
     * @param rect padding的区域
     */
    fun setPadding(rect: Rect): NinePatchDrawableFactory {
        paddingRect = rect
        return this
    }

    /**
     * 设置是否水平镜像，如果左右聊天气泡都用一张图的话，需要水平镜像一下，不是必须的
     * @param horizontalMirror 是否水平镜像
     */
    fun setHorizontalMirror(horizontalMirror: Boolean): NinePatchDrawableFactory {
        this.horizontalMirror = horizontalMirror
        return this
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

        setFinalUsedBitmapSize(bitmap)

        return get9PatchDrawable(bitmap, resources)
    }

    /**
     * 设置本地文件夹中的图片
     * 从文件解析的话，需要处理缩放，density
     * @param file 本地png文件路径
     */
    private fun get9PatchFromFile(
        resources: Resources,
        file: File?
    ): Drawable? {

        if (file == null || !file.exists()) {
            return null
        }

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
        setFinalUsedBitmapSize(bitmap)

        return get9PatchDrawable(bitmap, resources)

    }

    /**
     * 控制内容填充的区域
     * （注意：这里的left，top，right，bottom同xml文件中的padding意思一致，只不过这里是百分比形式）
     */
    private fun buildPadding() {
        if (horizontalMirror) {
            finalPaddingRect.left = ((originWidth - paddingRight) * finalWidth / originWidth)
            finalPaddingRect.right = ((paddingLeft * finalWidth) / originWidth)
        } else {
            finalPaddingRect.left = ((paddingLeft * finalWidth) / originWidth)
            finalPaddingRect.right = ((originWidth - paddingRight) * finalWidth / originWidth)
        }
        finalPaddingRect.top = (paddingTop * finalHeight / originHeight)
        finalPaddingRect.bottom = ((originHeight - paddingBottom) * finalHeight / originHeight)

        Log.i(TAG, "buildPadding: rect = $finalPaddingRect")
    }

    /**
     * 直接处理bitmap数据
     */
    private fun setFinalUsedBitmapSize(bitmap: Bitmap?) {
        Log.i(TAG, "setBitmapData: width = ${bitmap?.width}, height = ${bitmap?.height}")
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

        //Note: 等待进一步研究，这四个int值，好像都写0也可以。
        //左右padding
        byteBuffer.putInt(finalPaddingRect.left)
        byteBuffer.putInt(finalPaddingRect.right)
        //上下padding
        byteBuffer.putInt(finalPaddingRect.top)
        byteBuffer.putInt(finalPaddingRect.bottom)

//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)
//        //上下padding
//        byteBuffer.putInt(0)
//        byteBuffer.putInt(0)


//        // skip 4 bytes
        byteBuffer.putInt(0)

        // regions 控制横向拉伸的线段数据
        //mDivX数组
        horizontalStretchBean?.let {
            if (horizontalMirror) {
                byteBuffer.putInt((originWidth - it.end) * finalWidth / originWidth)
                byteBuffer.putInt((originWidth - it.start) * finalWidth / originWidth)
            } else {
                byteBuffer.putInt(it.start * finalWidth / originWidth)
                byteBuffer.putInt(it.end * finalWidth / originWidth)
            }
        }

        //mDivY数组
        // regions 控制竖向拉伸的线段数据
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

}