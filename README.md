# Android 使用NinePatchDrawable实现动态聊天气泡

最近一段时间，在做一个需求，需要实现一个聊天气泡的动画效果，如下图所示：

### 动态聊天气泡动画

![动态聊天气泡动画](images%2Fdynamic_bubble.webp)

### 静态聊天气泡

![静态聊天气泡](images%2Fstatic_bubble.webp)

经过一段时间调研，实现方案如下

### 实现方案

* 从服务端下载zip文件，文件中包含配置文件和多张png图片，配置文件定义了图片的横向拉伸拉伸区域、纵向拉伸区域、padding信息等。
* 从本地加载配置文件，加载多张png图片为bitmap。
* 将bitmap存储在内存里。LruCache，避免多次解析。
* 根据配置文件，将png图片转换为.9图，NinePatchDrawable。
* 使用多张NinePatchDrawable创建一个帧动画对象AnimationDrawable
* 将AnimationDrawable设置为控件的背景，并让AnimationDrawable播放动画，执行一定的次数后停止动画。

其中的难点在于第3步，将png图片转换为.9图，NinePatchDrawable。

NinePatchDrawable 的构造函数。

```java
/**
 * Create drawable from raw nine-patch data, setting initial target density
 * based on the display metrics of the resources.
 */
public NinePatchDrawable(Resources res,Bitmap bitmap,byte[]chunk,Rect padding,String srcName){
        this(new NinePatchState(new NinePatch(bitmap,chunk,srcName),padding),res);
}
```

其中最关键的点在于构建`byte[] chunk`参数。
通过查看这个类[NinePatchChunk.java](https://android.googlesource.com/platform/packages/apps/Gallery2/+/jb-dev/src/com/android/gallery3d/ui/NinePatchChunk.java)
，并参阅了许多博客，通过反向分析NinePatchChunk类的deserialize方法，得到了如何构建`byte[] chunk`的方法。

```java
// See "frameworks/base/include/utils/ResourceTypes.h" for the format of
// NinePatch chunk.
class NinePatchChunk {

    public static final int NO_COLOR = 0x00000001;
    public static final int TRANSPARENT_COLOR = 0x00000000;
    public Rect mPaddings = new Rect();
    public int mDivX[];
    public int mDivY[];
    public int mColor[];

    private static void readIntArray(int[] data, ByteBuffer buffer) {
        for (int i = 0, n = data.length; i < n; ++i) {
            data[i] = buffer.getInt();
        }
    }

    private static void checkDivCount(int length) {
        if (length == 0 || (length & 0x01) != 0) {
            throw new RuntimeException("invalid nine-patch: " + length);
        }
    }

    //注释1处，解析byte[]数据，构建NinePatchChunk对象
    public static NinePatchChunk deserialize(byte[] data) {
        ByteBuffer byteBuffer =
                ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
        byte wasSerialized = byteBuffer.get();
        if (wasSerialized == 0)//第一个字节不能为0
            return null;
        NinePatchChunk chunk = new NinePatchChunk();
        chunk.mDivX = new int[byteBuffer.get()];//第二个字节为x方向上的切割线的个数
        chunk.mDivY = new int[byteBuffer.get()];//第三个字节为y方向上的切割线的个数
        chunk.mColor = new int[byteBuffer.get()];//第四个字节为颜色的个数
        checkDivCount(chunk.mDivX.length);//判断x方向上的切割线的个数是否为偶数
        checkDivCount(chunk.mDivY.length);//判断y方向上的切割线的个数是否为偶数
        // skip 8 bytes，跳过8个字节
        byteBuffer.getInt();
        byteBuffer.getInt();

        //注释2处，处理padding，发现都设置为0也可以。
        chunk.mPaddings.left = byteBuffer.getInt();//左边的padding
        chunk.mPaddings.right = byteBuffer.getInt();//右边的padding
        chunk.mPaddings.top = byteBuffer.getInt();//上边的padding
        chunk.mPaddings.bottom = byteBuffer.getInt();//下边的padding
        // skip 4 bytes
        byteBuffer.getInt();//跳过4个字节
        readIntArray(chunk.mDivX, byteBuffer);//读取x方向上的切割线的位置
        readIntArray(chunk.mDivY, byteBuffer);//读取y方向上的切割线的位置
        readIntArray(chunk.mColor, byteBuffer);//读取颜色
        return chunk;
    }
}
```

注释1处，解析byte[]数据，构建NinePatchChunk对象。我们添加了一些注释，意思已经很清晰了。

然后我们根据这里类来构建`byte[] chunk`参数。

```kotlin
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

    //Note: 目前还没搞清楚，发现都 byteBuffer.putInt(0)，也没问题。
    //左右padding
    byteBuffer.putInt(mRectPadding.left)
    byteBuffer.putInt(mRectPadding.right)
    //上下padding
    byteBuffer.putInt(mRectPadding.top)
    byteBuffer.putInt(mRectPadding.bottom)

    //byteBuffer.putInt(0)
    //byteBuffer.putInt(0)
    //上下padding
    //byteBuffer.putInt(0)
    //byteBuffer.putInt(0)

    //skip 4 bytes
    byteBuffer.putInt(0)

    //mDivX数组，控制横向拉伸的线段数据，目前只支持一个线段
    patchRegionHorizontal.forEach {
        byteBuffer.putInt(it.start * width / originWidth)
        byteBuffer.putInt(it.end * width / originWidth)
    }

    //mDivY数组，控制竖向拉伸的线段数据，目前只支持一个线段
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
```

完整的类请参考 `AnimationDrawableFactory.kt` 。

### 使用

完整的使用请查看 ChatAdapter 类。

AnimationDrawableFactory 支持从文件构建动画，也支持从Android的资源文件夹构建动画。

**!!!注意，从文件构建动画，需要将请把工程下的`bubbleframe`文件夹拷贝到手机的`Android/data/包名/files`
目录下，否则会报错。**

**从文件构建动画**

```kotlin
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
```

这里注意一下：因为文件中的图片是一倍图，所以这里需要放大，所以设置了`setScaleFromFile(true)`。
如果文件中的图片是3倍图，就不需要设置这个参数了。如果需要更加精细的缩放控制，后面再增加支持。

**从Android的资源文件夹构建动画**

```kotlin

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

/**
 * 从正常的资源文件加载动态气泡
 */
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
```

有时候可能我们只需要构建静态气泡，也就是只需要一张
NinepatchDrawable，我们提供了一个类来构建静态气泡，`NinePatchDrawableFactory.kt`。

**从文件加载**

```kotlin
return NinePatchDrawableFactory(context)
            .setDrawableFile(pngFile)//图片文件
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setScaleFromFile(true)//是否从文件中读取图片的缩放比例，不是必须的
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .buildFromFile()
```

**从资源加载**

```kotlin
return NinePatchDrawableFactory(context)
            .setDrawableResId(R.drawable.bubble_frame1)//图片资源id
            .setHorizontalStretchBean(PatchStretchBean(60, 61))//水平拉伸区域
            .setVerticalStretchBean(PatchStretchBean(52, 53))//垂直拉伸区域
            .setOriginSize(128, 112)//原始图片大小
            .setPadding(Rect(31, 37, 90, 75))//padding区域
            .setHorizontalMirror(isSelf)//是否水平镜像，不是必须的
            .buildFromResource()
```

### padding 取值

如图所示：宽高是128*112。横向padding取值为31,90，纵向padding取值为37,75。

![padding值.png](images%2Fpadding%E5%80%BC.png)

### 其他

在实现过程中发现Android 的 帧动画 AnimationDrawable
无法控制动画执行的次数。最后自定义了一个类，`CanStopAnimationDrawable.kt` 解决。

参考链接：

* [Carson带你学Android：关于逐帧动画的使用都在这里了！-腾讯云开发者社区-腾讯云](https://cloud.tencent.com/developer/article/1963233?areaId=106001)
* [聊天气泡图片的动态拉伸、镜像与适配 - 掘金](https://juejin.cn/post/7188708254346641465)
* [Android 点九图机制讲解及在聊天气泡中的应用 - 掘金](https://juejin.cn/post/6844903945031139336)
* [Android动态布局入门及NinePatchChunk解密](https://mp.weixin.qq.com/s?__biz=MzI1NjEwMTM4OA==&mid=2651232105&idx=1&sn=fcc4fa956f329f839f2a04793e7dd3b9&mpshare=1&scene=21&srcid=0719Nyt7J8hsr4iYwOjVPXQE#wechat_redirect)
* [Android点九图总结以及在聊天气泡中的使用-腾讯云开发者社区-腾讯云](https://cloud.tencent.com/developer/article/1168755?)
* https://developer.android.com/studio/write/draw9patch?utm_source=android-studio&hl=zh-cn

